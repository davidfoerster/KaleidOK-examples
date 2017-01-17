package kaleidok.exaleads.chromatik;

import kaleidok.flickr.Flickr;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.SynesthetiatorEmotion;

import java.io.IOException;


public class SimpleChromasthetiator<F extends Flickr>
  extends Chromasthetiator<F> implements Cloneable
{
  private int maxColors;

  private int maxKeywords;

  private ChromatikQuery chromatikQuery;


  public SimpleChromasthetiator()
  {
    maxColors = 2;
    maxKeywords = 0;

    try {
      synesthetiator = new SynesthetiatorEmotion();
    } catch (IOException ex) {
      throw new Error(ex);
    }

    palettes = new SynesketchPalette("standard");
    chromatikQuery = new SimpleChromatikQuery();
  }


  SimpleChromasthetiator( Chromasthetiator<? extends F> other )
  {
    super(other);
    maxColors = other.getMaxColors();
    maxKeywords = other.getMaxKeywords();
    chromatikQuery = other.getChromatikQuery();
  }


  @Override
  public int getMaxColors()
  {
    return maxColors;
  }

  @Override
  public void setMaxColors( int maxColors )
  {
    this.maxColors = maxColors;
  }


  @Override
  public int getMaxKeywords()
  {
    return maxKeywords;
  }

  @Override
  public void setMaxKeywords( int n )
  {
    maxKeywords = n;
  }


  @Override
  public ChromatikQuery getChromatikQuery()
  {
    return chromatikQuery;
  }

  @Override
  public void setChromatikQuery( ChromatikQuery chromatikQuery )
  {
    this.chromatikQuery = chromatikQuery;
  }


  @Override
  public SimpleChromasthetiator<F> toSimple()
  {
    return clone();
  }


  @Override
  public SimpleChromasthetiator<F> clone()
  {
    SimpleChromasthetiator<F> clone;
    try
    {
      //noinspection unchecked
      clone = (SimpleChromasthetiator<F>) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }

    if (clone.chromatikQuery != null)
      clone.chromatikQuery = clone.chromatikQuery.toSimple();

    return clone;
  }
}
