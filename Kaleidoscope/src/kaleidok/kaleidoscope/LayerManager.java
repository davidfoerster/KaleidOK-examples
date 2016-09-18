package kaleidok.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import kaleidok.kaleidoscope.layer.*;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.Strings;
import kaleidok.util.prefs.BeanUtils;
import kaleidok.util.prefs.PropertyLoader;
import org.apache.commons.io.FilenameUtils;
import processing.core.PImage;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public class LayerManager implements List<ImageLayer>, Runnable
{
  private static final int MIN_IMAGES = 5;

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  private final Kaleidoscope parent;

  private final List<ImageLayer> layers = new ArrayList<>(8);

  public List<PImageFuture> images; // list to hold input images

  private SpectrogramLayer spectrogramLayer;
  private OuterMovingShape outerMovingShape;
  private FoobarLayer foobarLayer;
  private CentreMovingShape centreLayer;
  private BackgroundLayer backgroundLayer;


  LayerManager( Kaleidoscope parent )
  {
    this.parent = parent;

    layers.add(getBackgroundLayer());
    layers.add(getSpectrogramLayer());
    layers.add(getOuterMovingShape());
    layers.add(getFoobarLayer());
    layers.add(getCentreLayer());

    applyLayerProperties();
  }


  private void applyLayerProperties()
  {
    Properties prop = loadLayerProperties();
    Package pack = ImageLayer.class.getPackage();
    MessageFormat screenshotPathPattern = getScreenshotPathPattern();
    Set<String> appliedProperties =
      logger.isLoggable(Level.FINEST) ? new HashSet<>(prop.size() * 2) : null;

    for (ImageLayer l: this)
    {
      BeanUtils.applyBeanProperties(prop, pack, l, appliedProperties);
      l.setScreenshotPathPattern(screenshotPathPattern);
    }

    if (appliedProperties != null && appliedProperties.size() < prop.size())
    {
      Map<String, String> unappliedProperties =
        new HashMap<>(Math.max(prop.size() - appliedProperties.size(), 0) * 2);
      for (Map.Entry<Object, Object> entry: prop.entrySet())
      {
        String key = (String) entry.getKey();
        if (!appliedProperties.contains(key))
          unappliedProperties.put(key, (String) entry.getValue());
      }
      logger.log(Level.FINEST,
        "The following {0} of your {1} layer property settings were not " +
          "used: {2}",
        new Object[]{
          unappliedProperties.size(), prop.size(), unappliedProperties});
    }
  }


  private MessageFormat getScreenshotPathPattern()
  {
    String pattern = parent.getParameterMap().get(
      parent.getClass().getPackage().getName() + ".screenshots.pattern");
    return (pattern != null) ? new MessageFormat(pattern) : null;
  }


  private SpectrogramLayer getSpectrogramLayer()
  {
    if (spectrogramLayer == null) {
      AudioProcessingManager apm = parent.getAudioProcessingManager();
      spectrogramLayer = new SpectrogramLayer(parent,
        1 << 8, 0.275f, 0.480f,
        apm.getFftProcessor(),
        apm.getAudioDispatcher().getFormat().getSampleRate());
      spectrogramLayer.setNextImage(getImages().get(0));
    }
    return spectrogramLayer;
  }


  private OuterMovingShape getOuterMovingShape()
  {
    if (outerMovingShape == null) {
      outerMovingShape = new OuterMovingShape(parent, 16, 0.280f);
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
      foobarLayer = new FoobarLayer(parent, 16, 0.100f, 0.300f);
      foobarLayer.setNextImage(getImages().get(3));
    }
    return foobarLayer;
  }


  private CentreMovingShape getCentreLayer()
  {
    if (centreLayer == null) {
      centreLayer = new CentreMovingShape(parent,
        getImages(), 1 << 5, 0.050f, 0.150f,
        parent.getAudioProcessingManager().getVolumeLevelProcessor());
    }
    return centreLayer;
  }


  BackgroundLayer getBackgroundLayer()
  {
    if (backgroundLayer == null) {
      final List<PImageFuture> images = getImages();
      backgroundLayer = new BackgroundLayer(parent);
      if (!images.isEmpty()) {
        backgroundLayer.setNextImage(
          images.get((int) parent.random(images.size())));
      }
    }
    return backgroundLayer;
  }


  public List<PImageFuture> getImages()
  {
    if (images == null)
    {
      String imagesParam = parent.getParameterMap().get(
        parent.getClass().getPackage().getName() + ".images.initial");
      images = (imagesParam == null || imagesParam.isEmpty()) ?
        new ArrayList<>(MIN_IMAGES) :
        WHITESPACE_PATTERN.splitAsStream(imagesParam)
          .filter(( s ) -> !s.isEmpty())
          .map(( s ) -> parent.getImageFuture(
            (FilenameUtils.getPrefixLength(s) == 0 && !Strings.looksLikeUrl(s)) ? "/images/" + s : s))
          .collect(Collectors.toList());
      if (images.isEmpty())
        images.add(PImageFuture.EMPTY);
      int imageCount = images.size();
      for (int i = images.size(); i < MIN_IMAGES; i++)
        images.add(images.get(i % imageCount));
    }
    return images;
  }


  public void waitForImages()
  {
    for (PImageFuture futureImg: getImages()) {
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


  private static Properties loadLayerProperties()
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
    return layerProperties;
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
