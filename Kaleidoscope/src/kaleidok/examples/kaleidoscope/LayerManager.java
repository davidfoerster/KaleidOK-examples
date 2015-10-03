package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;


public class LayerManager extends ArrayList<CircularLayer>
{
  private static final int MIN_IMAGES = 5;

  private final Kaleidoscope parent;

  public List<PImageFuture> images; // list to hold input images

  public PImageFuture bgImage;

  public final SpectrogramLayer spectrogramLayer;
  private OuterMovingShape outerMovingShape;
  public final FoobarLayer foobarLayer;
  public final CentreMovingShape centreLayer;


  LayerManager( Kaleidoscope parent )
  {
    this.parent = parent;
    List<PImageFuture> images = getImages();
    AudioProcessingManager apm = parent.getAudioProcessingManager();

    spectrogramLayer =
      new SpectrogramLayer(parent, images.get(0), 1 << 8, -1, -1,
        apm.getFftProcessor(),
        apm.getAudioDispatcher().getFormat().getSampleRate());
    OuterMovingShape outerMovingShape =
      getOuterMovingShape();
    foobarLayer =
      new FoobarLayer(parent, images.get(3), 1 << 4, -1, -1);
    centreLayer =
      new CentreMovingShape(parent, images, 1 << 5, -1,
        apm.getVolumeLevelProcessor());

    add(spectrogramLayer);
    add(outerMovingShape);
    add(foobarLayer);
    add(centreLayer);
    updateLayerSizes();
  }


  private OuterMovingShape getOuterMovingShape()
  {
    if (outerMovingShape == null) {
      OuterMovingShape outerMovingShape =
        new OuterMovingShape(parent, getImages().get(4), 1 << 5, -1);
      AudioProcessingManager apm = parent.getAudioProcessingManager();
      AudioDispatcher audioDispatcher = apm.getAudioDispatcher();
      audioDispatcher.addAudioProcessor(new PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(),
        apm.getAudioBufferSize(),
        outerMovingShape.getPitchDetectionHandler()));
      this.outerMovingShape = outerMovingShape;
    }
    return outerMovingShape;
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
            PImageFuture image = getImageFuture(strImage);
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


  public PImageFuture getImageFuture( String path )
  {
    URL url = this.getClass().getResource(path);
    if (url == null) {
      try {
        url = new URL(parent.getDocumentBase(), path);
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(ex);
      }
      if (url.getProtocol().equals("file")) {
        File file;
        try {
          file = new File(url.toURI());
        } catch (URISyntaxException ex) {
          throw new AssertionError(ex);
        }
        if (!file.isFile() || !file.canRead())
          url = null;
      }
    }
    return (url != null) ? parent.getImageFuture(url) : null;
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
        assert img != null && img.width > 0 && img.height > 0;
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex.getCause());
      }
    }
  }


  public void draw()
  {
    if (parent.wasResized())
      updateLayerSizes();

    drawBackgroundTexture();

    for (CircularLayer l : this) {
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
