package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.PImageFuture;
import kaleidok.util.BeanUtils;
import processing.core.PImage;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.LoggingUtils.logThrown;


public class LayerManager extends ArrayList<ImageLayer>
  implements Runnable
{
  private static final int MIN_IMAGES = 5;

  private final Kaleidoscope parent;

  public List<PImageFuture> images; // list to hold input images

  private SpectrogramLayer spectrogramLayer;
  private OuterMovingShape outerMovingShape;
  private FoobarLayer foobarLayer;
  private CentreMovingShape centreLayer;
  private BackgroundLayer backgroundLayer;


  LayerManager( Kaleidoscope parent )
  {
    super(8);
    this.parent = parent;

    BackgroundLayer bg = getBackgroundLayer();
    add(getSpectrogramLayer());
    add(getOuterMovingShape());
    add(getFoobarLayer());
    add(getCentreLayer());

    Properties prop = getLayerProperties();
    Package pack = ImageLayer.class.getPackage();
    int count = BeanUtils.applyBeanProperties(prop, pack, bg);
    for (ImageLayer l: this)
      count += BeanUtils.applyBeanProperties(prop, pack, l);

    if (count != prop.size()) {
      logger.log(Level.FINEST,
        "Only {0} of your {1} layer property settings were used",
        new Object[]{count, prop.size()});
    }
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
        PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(),
        apm.getAudioBufferSize(),
        outerMovingShape.getPitchDetectionHandler()));
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
    if (this.images == null) {
      String imagesParam = parent.getParameter(
        parent.getClass().getPackage().getName() + ".images.initial");
      if (imagesParam == null) {
        this.images = new ArrayList<>(MIN_IMAGES);
      } else {
        String[] images = imagesParam.split("\\s+");
        this.images = new ArrayList<>(Math.max(images.length, MIN_IMAGES));
        for (String strImage : images) {
          if (!strImage.isEmpty()) {
            char c = strImage.charAt(0);
            if (c != '/' && c != File.separatorChar &&
              strImage.indexOf(':') < 0) {
              strImage = "/images/" + strImage;
            }
            PImageFuture image = parent.getImageFuture(strImage);
            if (image != null) {
              this.images.add(image);
            } else {
              String msg = "Couldn't load image";
              Throwable ex = new FileNotFoundException(strImage);
              if (LayerManager.class.desiredAssertionStatus()) {
                throw new AssertionError(msg, ex);
              } else {
                logger.log(Level.WARNING, msg, ex);
              }
            }
          }
        }
      }

      if (images.isEmpty())
        images.add(PImageFuture.EMPTY);
      int imageCount = images.size();
      for (int i = images.size(); i < MIN_IMAGES; i++)
        images.add(images.get(i % imageCount));
    }
    return this.images;
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
            // go on...
          }
        }
        assert img != null && img.width > 0 && img.height > 0 :
          String.valueOf(img) + " has width or height ≤0";
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
      parent.strokeWeight(strokeWeight);
      l.run();
    }
    parent.popMatrix();
  }


  private static final URL layerPropertiesResource =
    ImageLayer.class.getResource("layer.properties");

  private Properties layerProperties = null;


  private Properties getLayerProperties()
  {
    if (layerProperties == null) {
      layerProperties = new Properties();
      if (layerPropertiesResource != null) {
        try (Reader r = new InputStreamReader(layerPropertiesResource.openStream())) {
          layerProperties.load(r);
        } catch (IOException ex) {
          logThrown(logger, Level.SEVERE,
            "Couldn't load properties from {0}",
            ex, layerPropertiesResource);
        }
      } else {
        logger.config("No layer properties file found");
      }
    }
    return layerProperties;
  }
}
