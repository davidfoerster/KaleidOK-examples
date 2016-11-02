package kaleidok.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.StringProperty;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.kaleidoscope.layer.*;
import kaleidok.text.FormatUtils;
import kaleidok.util.Arrays;
import kaleidok.util.Strings;
import kaleidok.util.SynchronizedFormat;
import kaleidok.util.concurrent.ImmediateFuture;
import kaleidok.util.function.ChangeListener;
import kaleidok.util.prefs.PropertyLoader;
import org.apache.commons.io.FilenameUtils;
import processing.core.PImage;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public final class LayerManager
  implements List<ImageLayer>, Runnable, PreferenceBean
{
  private static final int MIN_IMAGES = 5;

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  private final Kaleidoscope parent;

  private final List<ImageLayer> layers;

  public List<Future<PImage>> images; // list to hold input images

  private SpectrogramLayer spectrogramLayer;
  private OuterMovingShape outerMovingShape;
  private FoobarLayer foobarLayer;
  private CentreMovingShape centreLayer;
  private BackgroundLayer backgroundLayer;

  private final AspectedStringProperty screenshotPathFormatString;

  private final ScreenshotPathFormatBinding screenshotPathFormat;


  LayerManager( Kaleidoscope parent )
  {
    this.parent = parent;

    screenshotPathFormatString =
      new AspectedStringProperty(this, "screenshot path pattern");
    screenshotPathFormatString.addAspect(
      PropertyPreferencesAdapterTag.getInstance());
    screenshotPathFormat = new ScreenshotPathFormatBinding(this);

    layers = new ArrayList<>(Arrays.asImmutableList(
      getBackgroundLayer(),
      getSpectrogramLayer(),
      getOuterMovingShape(),
      getFoobarLayer(),
      getCentreLayer()));

    initScreenshotFormat();
    initLayerProperties();

    layers.forEach(ImageLayer::init);
  }


  private void initScreenshotFormat()
  {
    String formatString = parent.getParameterMap().get(
      parent.getClass().getPackage().getName() + ".screenshots.pattern");
    if (formatString != null && !formatString.isEmpty())
      screenshotPathFormatString.set(formatString);

    final ChangeListener<Object, Object> screenshotCallback =
      ( owner, oldValue, newValue ) -> {
        if (oldValue != null)  // skip the initial image
          saveScreenshot();
      };
    this.forEach(
      (l) -> l.imageChangeCallback = screenshotCallback);
  }


  private int initLayerProperties()
  {
    final Map<String, String> propertyEntries = loadLayerProperties();
    Stream<String> appliedEntries = getLayerPreferenceAdapters()
      .map((pa) -> new AbstractMap.SimpleEntry<>(
        PropertyUtils.applyProperty(
          propertyEntries, "kaleidok.kaleidoscope.layer", pa.property),
        pa))
      .peek((item) -> item.getValue().load())
      .map(Map.Entry::getKey)
      .filter(Objects::nonNull);

    return logger.isLoggable(Level.FINEST) ?
      logUnappliedPropertyEntries(propertyEntries, appliedEntries) :
      (int) appliedEntries.count();
  }


  private static int logUnappliedPropertyEntries(
    Map<String, String> allEntries, Stream<String> appliedEntries )
  {
    final Set<String> appliedEntriesSet =
      appliedEntries.collect(Collectors.toSet());

    if (appliedEntriesSet.size() < allEntries.size())
    {
      Collection<Map.Entry<String, String>> unappliedEntries =
        appliedEntriesSet.isEmpty() ?
          allEntries.entrySet() :
          allEntries.entrySet().stream()
            .filter((e) -> !appliedEntriesSet.contains(e.getKey()))
            .collect(Collectors.toList());

      logger.log(Level.FINEST,
        "The following {0} of your {1} layer property entries were not " +
          "used: {2}",
        new Object[]{ unappliedEntries.size(), allEntries.size(),
          unappliedEntries });
    }

    return appliedEntriesSet.size();
  }


  public void dispose()
  {
    clear();
    backgroundLayer = null;
    centreLayer = null;
    foobarLayer = null;
    outerMovingShape = null;
    spectrogramLayer = null;
  }


  @Override
  public String getName()
  {
    return "Layers";
  }


  @Override
  public Object getParent()
  {
    return parent;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?,?>>
  getPreferenceAdapters()
  {
    return Stream.concat(
      Stream.of(screenshotPathFormatString.getAspect(
        PropertyPreferencesAdapterTag.getWritableInstance())),
      getLayerPreferenceAdapters());
  }


  private Stream<? extends PropertyPreferencesAdapter<?,?>>
  getLayerPreferenceAdapters()
  {
    return this.stream().flatMap(ImageLayer::getPreferenceAdapters);
  }


  public StringProperty screenshotPathFormatStringProperty()
  {
    return screenshotPathFormatString;
  }

  public String getScreenshotPathFormatString()
  {
    return screenshotPathFormatString.get();
  }

  public void setScreenshotPathFormatString( String fmt )
  {
    screenshotPathFormatString.set(fmt);
  }


  private void saveScreenshot()
  {
    String pathName =
      screenshotPathFormat.format(new Date(), parent.frameCount);
    if (pathName != null)
      parent.save(pathName, true);
  }


  private SpectrogramLayer getSpectrogramLayer()
  {
    if (spectrogramLayer == null) {
      AudioProcessingManager apm = parent.getAudioProcessingManager();
      spectrogramLayer = new SpectrogramLayer(parent,
        1 << 8, 0.275, 0.480,
        apm.getFftProcessor(),
        apm.getAudioDispatcher().getFormat().getSampleRate());
      spectrogramLayer.setNextImage(getImages().get(0));
    }
    return spectrogramLayer;
  }


  private OuterMovingShape getOuterMovingShape()
  {
    if (outerMovingShape == null) {
      outerMovingShape = new OuterMovingShape(parent, 16, 0.280);
      outerMovingShape.setNextImage(getImages().get(4));

      AudioProcessingManager apm = parent.getAudioProcessingManager();
      AudioDispatcher audioDispatcher = apm.getAudioDispatcher();
      audioDispatcher.addAudioProcessor(new PitchProcessor(
        PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(),
        apm.getAudioBufferSize(),
        outerMovingShape.pitchDetectionHandler));
    }
    return outerMovingShape;
  }


  private FoobarLayer getFoobarLayer()
  {
    if (foobarLayer == null) {
      foobarLayer = new FoobarLayer(parent, 16, 0.100, 0.300);
      foobarLayer.setNextImage(getImages().get(3));
    }
    return foobarLayer;
  }


  private CentreMovingShape getCentreLayer()
  {
    if (centreLayer == null) {
      centreLayer = new CentreMovingShape(parent,
        getImages(), 1 << 5, 0.050, 0.150,
        parent.getAudioProcessingManager().getVolumeLevelProcessor());
    }
    return centreLayer;
  }


  BackgroundLayer getBackgroundLayer()
  {
    if (backgroundLayer == null) {
      final List<Future<PImage>> images = getImages();
      backgroundLayer = new BackgroundLayer(parent);
      if (!images.isEmpty()) {
        backgroundLayer.setNextImage(
          images.get((int) parent.random(images.size())));
      }
    }
    return backgroundLayer;
  }


  public List<Future<PImage>> getImages()
  {
    if (images == null)
    {
      String imagesParam = parent.getParameterMap().get(
        parent.getClass().getPackage().getName() + ".images.initial");
      //noinspection CallToStringConcatCanBeReplacedByOperator
      images = (imagesParam == null || imagesParam.isEmpty()) ?
        new ArrayList<>(MIN_IMAGES) :
        WHITESPACE_PATTERN.splitAsStream(imagesParam)
          .filter(( s ) -> !s.isEmpty())
          .map(( s ) -> parent.getImageFuture(
            (FilenameUtils.getPrefixLength(s) == 0 && !Strings.looksLikeUrl(s)) ? "/images/".concat(s) : s))
          .collect(Collectors.toList());
      if (images.isEmpty())
        images.add(ImmediateFuture.empty());
      int imageCount = images.size();
      for (int i = images.size(); i < MIN_IMAGES; i++)
        images.add(images.get(i % imageCount));
    }
    return images;
  }


  public void waitForImages()
  {
    for (Future<PImage> futureImg: getImages()) {
      try {
        PImage img;
        while (true) {
          try {
            img = futureImg.get();
            break;
          } catch (InterruptedException ex) {
            logger.log(Level.FINEST, "Waiting for images was interrupted", ex);
          }
        }
        assert img != null && img.width > 0 && img.height > 0 :
          img + " has width or height ≤0";
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex.getCause());
      }
    }
  }


  /**
   * Draws the managed layers.
   */
  @Override
  public void run()
  {
    final Kaleidoscope parent = this.parent;

    backgroundLayer.run();

    float
      scale = Math.min(parent.width, parent.height),
      strokeWeight = 1 / scale;
    parent.pushMatrix();
    parent.translate(parent.width * 0.5f, parent.height * 0.5f);
    parent.scale(scale);
    for (Runnable l : this) {
      if (l != backgroundLayer) {
        parent.strokeWeight(strokeWeight);
        l.run();
      }
    }
    parent.popMatrix();
  }


  private static Map<String, String> loadLayerProperties()
  {
    String propFn = "layer.properties";
    Properties layerProperties = new Properties();
    try
    {
      if (PropertyLoader.load(
        layerProperties, null, ImageLayer.class, propFn) == 0)
      {
        logger.log(Level.CONFIG,
          "No layer properties file \"{0}\" found; using defaults",
          propFn);
      }
    }
    catch (IOException ex)
    {
      logThrown(logger, Level.SEVERE,
        "Couldn't load layer properties file \"{0}\"; using defaults", ex,
        propFn);
    }
    return !layerProperties.isEmpty() ?
      PropertyLoader.toMap(layerProperties, null) :
      Collections.emptyMap();
  }


  private static final class ScreenshotPathFormatBinding
    extends ObjectBinding<MessageFormat>
  {
    private final LayerManager layerManager;

    private final SynchronizedFormat format = new SynchronizedFormat();


    private ScreenshotPathFormatBinding( LayerManager layerManager )
    {
      this.layerManager = layerManager;
      onInvalidating();
      bind(layerManager.screenshotPathFormatStringProperty());
    }


    @Override
    protected MessageFormat computeValue()
    {
      String s = layerManager.getScreenshotPathFormatString();
      if (s != null)
      {
        s = s.trim();
        if (!s.isEmpty())
        {
          return
            FormatUtils.verifyFormatThrowing(MessageFormat::new, s, TEST_ARGS,
              ScreenshotPathFormatBinding::verifyPath, null);
        }
      }
      return null;
    }


    @Override
    public void dispose()
    {
      unbind(layerManager.screenshotPathFormatStringProperty());
    }


    private static final Object[] TEST_ARGS = { new Date(0), 0 };


    private static boolean verifyPath( CharSequence filename )
    {
      if (filename.length() == 0)
        return false;

      File f = new File(filename.toString());
      if (!f.isAbsolute())
        throw new IllegalArgumentException("Path must be absolute");

      return true;
    }


    @Override
    protected void onInvalidating()
    {
      format.setUnderlying(get());
    }


    public String format( Object... args )
    {
      return format.format(args, null);
    }
  }


  @Override
  public int size()
  {
    return layers.size();
  }

  @Override
  public boolean isEmpty()
  {
    return layers.isEmpty();
  }

  @Override
  public boolean contains( Object o )
  {
    return layers.contains(o);
  }

  @Override
  public Iterator<ImageLayer> iterator()
  {
    return layers.iterator();
  }

  @Override
  public Object[] toArray()
  {
    return layers.toArray();
  }

  @Override
  public <T> T[] toArray( T[] a )
  {
    //noinspection SuspiciousToArrayCall
    return layers.toArray(a);
  }

  @Override
  public boolean add( ImageLayer imageLayer )
  {
    return layers.add(imageLayer);
  }

  @Override
  public boolean remove( Object o )
  {
    return layers.remove(o);
  }

  @Override
  public boolean containsAll( Collection<?> c )
  {
    return layers.containsAll(c);
  }

  @Override
  public boolean addAll( Collection<? extends ImageLayer> c )
  {
    return layers.addAll(c);
  }

  @Override
  public boolean addAll( int index, Collection<? extends ImageLayer> c )
  {
    return layers.addAll(index, c);
  }

  @Override
  public boolean removeAll( Collection<?> c )
  {
    return layers.removeAll(c);
  }

  @Override
  public boolean retainAll( Collection<?> c )
  {
    return layers.retainAll(c);
  }

  @Override
  public void replaceAll( UnaryOperator<ImageLayer> operator )
  {
    layers.replaceAll(operator);
  }

  @Override
  public void sort( Comparator<? super ImageLayer> c )
  {
    layers.sort(c);
  }

  @Override
  public void clear()
  {
    layers.clear();
  }

  @Override
  public ImageLayer get( int index )
  {
    return layers.get(index);
  }

  @Override
  public ImageLayer set( int index, ImageLayer element )
  {
    return layers.set(index, element);
  }

  @Override
  public void add( int index, ImageLayer element )
  {
    layers.add(index, element);
  }

  @Override
  public ImageLayer remove( int index )
  {
    return layers.remove(index);
  }

  @Override
  public int indexOf( Object o )
  {
    return layers.indexOf(o);
  }

  @Override
  public int lastIndexOf( Object o )
  {
    return layers.lastIndexOf(o);
  }

  @Override
  public ListIterator<ImageLayer> listIterator()
  {
    return layers.listIterator();
  }

  @Override
  public ListIterator<ImageLayer> listIterator( int index )
  {
    return layers.listIterator(index);
  }

  @Override
  public List<ImageLayer> subList( int fromIndex, int toIndex )
  {
    return layers.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<ImageLayer> spliterator()
  {
    return layers.spliterator();
  }

  @Override
  public boolean removeIf( Predicate<? super ImageLayer> filter )
  {
    return layers.removeIf(filter);
  }

  @Override
  public Stream<ImageLayer> stream()
  {
    return layers.stream();
  }

  @Override
  public Stream<ImageLayer> parallelStream()
  {
    return layers.parallelStream();
  }

  @Override
  public void forEach( Consumer<? super ImageLayer> action )
  {
    layers.forEach(action);
  }
}
