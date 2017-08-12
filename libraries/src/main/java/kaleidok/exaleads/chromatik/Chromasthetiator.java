package kaleidok.exaleads.chromatik;

import kaleidok.exaleads.chromatik.data.ChromatikColor;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.Photo;
import kaleidok.flickr.SizeMap;
import kaleidok.util.Math;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kaleidok.util.Arrays.shuffle;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public abstract class Chromasthetiator<F extends Flickr>
{
  static final Logger logger =
    Logger.getLogger(ChromasthetiationService.class.getPackage().getName());

  public static int EXPECTED_NEUTRAL_RESULT_COUNT = 10000;


  protected SynesthetiatorEmotion synesthetiator;

  protected SynesketchPalette palettes;

  protected F flickr;


  protected Chromasthetiator()
  {
    try {
      synesthetiator = new SynesthetiatorEmotion();
    } catch (IOException ex) {
      throw new Error(ex);
    }
    palettes = new SynesketchPalette("standard");
  }


  protected Chromasthetiator( Chromasthetiator<? extends F> other )
  {
    synesthetiator = other.synesthetiator;
    palettes = other.palettes;
    flickr = other.flickr;
  }


  public abstract int getMaxColors();

  public abstract void setMaxColors( int maxColors );


  public abstract int getMaxKeywords();

  public abstract void setMaxKeywords( int n );


  public abstract ChromatikQuery getChromatikQuery();

  public abstract void setChromatikQuery( ChromatikQuery chromatikQuery );


  public void setFlickrApi( F flickr )
  {
    this.flickr = flickr;
  }


  public abstract Chromasthetiator<F> toSimple();


  public ChromatikResponse query( String text ) throws IOException
  {
    EmotionalState emoState = synesthetiator.synesthetiseDirect(text);
    ChromatikQuery chromatikQuery = getChromatikQuery();
    chromatikQuery.setKeywords(getQueryKeywords(emoState));

    Random textRandom = new Random(text.hashCode());
    getQueryOptions(emoState, chromatikQuery.optionMap, textRandom);

    int queryStart = chromatikQuery.getStart();
    if (chromatikQuery.getKeywords().isEmpty() &&
      emoState.getStrongestEmotion().getType() == Emotion.NEUTRAL)
    {
      textRandom.setSeed(text.hashCode());
      chromatikQuery.randomizeRequestedSubset(
        EXPECTED_NEUTRAL_RESULT_COUNT, textRandom);
    }

    ChromatikResponse queryResult = chromatikQuery.getResult();
    chromatikQuery.setStart(queryStart);
    addFlickrPhotos(queryResult);

    return queryResult;
  }


  protected String getQueryKeywords( EmotionalState synState )
  {
    String keywords = getChromatikQuery().getKeywords();
    if (keywords.isEmpty())
    {
      Emotion emo = synState.getStrongestEmotion();
      if (emo.getType() != Emotion.NEUTRAL)
      {
        keywords =
          findStrongestAffectWords(synState.getAffectWords(), getMaxKeywords())
            .collect(Collectors.joining(" "));
        logger.log(Level.FINE, "Selected keywords: {0}", keywords);
      }
    }
    return keywords;
  }


  protected Map<Serializable, Serializable> getQueryOptions(
    EmotionalState synState, Map<Serializable, Serializable> opts,
    Random random )
  {
    int maxColors = getMaxColors();
    int colorCount;
    if (opts != null)
    {
      colorCount = !opts.isEmpty() ?
        (int) opts.keySet().stream()
          .filter((k) -> k instanceof ChromatikColor)
          .count() :
        0;
    }
    else
    {
      opts = new HashMap<>(maxColors * 2);
      colorCount = 0;
    }

    if (maxColors > colorCount)
    {
      Emotion emo = synState.getStrongestEmotion();

      // Derive color weight in search query from emotional weighting
      Double weight = max(sqrt(emo.getWeight()) * 0.5, 0.1) / maxColors;

      // Use (up to) maxColors random colors from palette for search query
      Formatter fmt = null;
      int[] palette = palettes.getColors(emo);
      if (random != null)
        palette = shuffle(palette.clone(), random);

      for (int c : palette)
      {
        if (colorCount >= maxColors)
          break;
        ChromatikColor cc = new ChromatikColor(c);
        if (opts.put(cc, weight) == null)
        {
          colorCount++;

          if (logger.isLoggable(Level.FINE))
          {
            if (fmt == null)
            {
              //noinspection resource,IOResourceOpenedButNotSafelyClosed
              fmt = new Formatter(new StringBuilder("Colors:"));
            }
            fmt.format(" #%06x (%s) at %.0f%%,",
              cc.value, cc.groupName, weight * 100);
          }
        }
      }

      if (fmt != null)
      {
        StringBuilder sb = (StringBuilder) fmt.out();
        logger.fine(sb.substring(0, sb.length() - 1));
      }
    }

    return opts;
  }


  private void addFlickrPhotos( ChromatikResponse response )
  {
    logger.log(Level.FINE, "Found {0} search results", response.hits);

    Flickr flickr = this.flickr;
    for (ChromatikResponse.Result imgInfo: response.results)
    {
      imgInfo.flickrPhoto =
        FlickrPhoto.fromChromatikResponseResult(flickr, imgInfo);
    }
  }


  protected static class FlickrPhoto extends Photo
  {
    private static final long serialVersionUID = -4527884875677374872L;

    protected transient Flickr flickr;


    public FlickrPhoto( Flickr flickr )
    {
      Objects.requireNonNull(flickr);
      this.flickr = flickr;
    }


    public static FlickrPhoto fromChromatikResponseResult( Flickr flickr,
      ChromatikResponse.Result imgInfo )
    {
      FlickrPhoto photo = new FlickrPhoto(flickr);
      photo.parseUrl(imgInfo.squareThumbnailUrl);
      return photo;
    }


    @Override
    public SizeMap getSizes()
    {
      SizeMap sizes = super.getSizes();
      if (sizes == null)
      {
        try
        {
          sizes = getSizesThrow();
        }
        catch (FlickrException ex)
        {
          switch (ex.getErrorCode())
          {
          case 1: // Photo not found
          case 2: // Permission denied
            logger.log(Level.FINER,
              "Couldn’t retrieve Flickr image sizes for {0}: {1}",
              new Object[]{ this, ex.getLocalizedMessage() });
            sizes = new SizeMap();
            break;

          default:
            logThrown(logger, Level.WARNING,
              "Couldn’t retrieve Flickr image sizes for {0}",
              ex, this);
            break;
          }
        }
        catch (IOException ex)
        {
          logThrown(logger, Level.WARNING,
            "Couldn’t retrieve Flickr image sizes for {0}",
            ex, this);
        }
      }
      return sizes;
    }


    @Override
    public SizeMap getSizesThrow() throws FlickrException, IOException
    {
      SizeMap sizes = super.getSizes();
      if (sizes == null) {
        try {
          sizes = flickr.getPhotoSizes(id);
        } catch (FlickrException ex) {
          ex.setPertainingObject(getMediumUrl());
          throw ex;
        }
        setSizes(sizes);
      }
      return sizes;
    }
  }


  private static Stream<String> findStrongestAffectWords(
    Collection<AffectWord> affectWords, int maxCount )
  {
    return (maxCount > 0 && !affectWords.isEmpty()) ?
      affectWords.stream()
        .map(
          (aw) -> new AbstractMap.SimpleEntry<>(
            aw.getWord(), aw.getWeights().map(Math::square).sum()))
        .sorted(Map.Entry.comparingByValue())
        .limit(maxCount)
        .map(Map.Entry::getKey) :
      Stream.empty();
  }
}
