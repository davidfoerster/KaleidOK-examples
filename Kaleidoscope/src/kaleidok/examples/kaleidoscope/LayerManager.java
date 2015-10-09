package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;


public class LayerManager extends ArrayList<ImageLayer>
  implements Runnable
{
  private static final int MIN_IMAGES = 5;

  private final Kaleidoscope parent;

  public List<PImageFuture> images; // list to hold input images

  public PImageFuture bgImage;

  public SpectrogramLayer spectrogramLayer;
  private OuterMovingShape outerMovingShape;
  public FoobarLayer foobarLayer;
  public CentreMovingShape centreLayer;


  LayerManager( Kaleidoscope parent )
  {
    this.parent = parent;
    add(getSpectrogramLayer());
    add(getOuterMovingShape());
    add(getFoobarLayer());
    add(getCentreLayer());
    updateLayerSizes();
  }


  private SpectrogramLayer getSpectrogramLayer()
  {
    if (spectrogramLayer == null) {
      AudioProcessingManager apm = parent.getAudioProcessingManager();
      spectrogramLayer = new SpectrogramLayer(parent,
        1 << 8, -1, -1, apm.getFftProcessor(),
        apm.getAudioDispatcher().getFormat().getSampleRate());
      spectrogramLayer.setNextImage(getImages().get(0));
    }
    return spectrogramLayer;
  }


  private OuterMovingShape getOuterMovingShape()
  {
    if (outerMovingShape == null) {
      outerMovingShape = new OuterMovingShape(parent, 1 << 5, -1);
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
      foobarLayer = new FoobarLayer(parent, 1 << 4, -1, -1);
      foobarLayer.setNextImage(getImages().get(3));
    }
    return foobarLayer;
  }


  private CentreMovingShape getCentreLayer()
  {
    if (centreLayer == null) {
      centreLayer = new CentreMovingShape(parent,
        getImages(), 1 << 5, -1,
        parent.getAudioProcessingManager().getVolumeLevelProcessor());
    }
    return centreLayer;
  }


  private void updateLayerSizes()
  {
    float r = Math.min(parent.width, parent.height) / 1000f;

    centreLayer.setOuterRadius(r * 150);
    centreLayer.setScaleFactor(r);

    foobarLayer.setInnerRadius(r * 0.500f * 125);
    foobarLayer.setOuterRadius(r * 1.333f * 275);

    outerMovingShape.setOuterRadius(r * 150);

    spectrogramLayer.setInnerRadius(r * 250);
    spectrogramLayer.setOuterRadius(r * 290);
    spectrogramLayer.setScaleFactor(r * 5e-3f);
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
            } else if (debug >= 1) {
              throw new AssertionError(new FileNotFoundException(strImage));
            } else {
              System.err.println("Couldn't load image: " + strImage);
            }
          }
        }
      }

      int bgImageIndex;
      switch (images.size()) {
      case 0:
        images.add(PImageFuture.EMPTY);
        // fall through

      case 1:
        bgImageIndex = 0;
        break;

      default:
        bgImageIndex = (int) parent.random(images.size()); // randomly choose the bgImageIndex
        break;
      }
      bgImage = images.get(bgImageIndex);
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
    if (parent.wasResized())
      updateLayerSizes();

    drawBackgroundTexture();

    for (Runnable l : this) {
      l.run();
    }
  }


  private void drawBackgroundTexture()
  {
    Kaleidoscope parent = this.parent;
    PImage bgImage;
    if (wireframe < 1 && (bgImage = this.bgImage.getNoThrow()) != null) {
      // background image
      parent.image(bgImage, ExtPApplet.ImageResizeMode.PAN, 0, 0,
        parent.width, parent.height); // resize-display image correctly to cover the whole screen
      parent.fill(255, 125 + (float) Math.sin(parent.frameCount * 0.01) * 5); // white fill with dynamic transparency
      parent.rect(0, 0, parent.width, parent.height); // rect covering the whole canvas
    } else {
      parent.background(0);
    }
  }
}
