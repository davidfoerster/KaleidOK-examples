package kaleidok.exaleads.chromatik;

import kaleidok.exaleads.chromatik.data.ChromatikColor;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.Photo;
import kaleidok.flickr.SizeMap;
import kaleidok.util.Strings;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.*;

import java.applet.Applet;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kaleidok.util.Arrays.shuffle;
import static kaleidok.util.LoggingUtils.logThrown;


public abstract class ChromasthetiatorBase<F extends Flickr>
{
  static final Logger logger =
    Logger.getLogger(ChromasthetiationService.class.getPackage().getName());

  // Configuration:
  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  /**
   * Maximum amount of keywords to select from affect words, if no search terms
   * are returned by {@link #getQueryKeywords()}
   */
  public int maxKeywords = 0;

  public static int EXPECTED_NEUTRAL_RESULT_COUNT = 10000;

  // other instance attributes:

  public ChromatikQuery chromatikQuery;

  protected final Applet parent;

  protected SynesthetiatorEmotion synesthetiator;

  protected SynesketchPalette palettes;

  protected F flickr;


  protected ChromasthetiatorBase( Applet parent )
  {
    this.parent = parent;

    try {
      synesthetiator = new SynesthetiatorEmotion();
    } catch (IOException ex) {
      throw new Error(ex);
    }
    palettes = new SynesketchPalette("standard");

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nHits = 10;
  }


  /**
   * Construct a new chromasthetiator with the same parent applet and
   * parameters as the original, but don't copy the reference to
   * {@link #flickr} (as it may have an incompatible type).
   *
   * @param other  The original chromasthetiator
   */
  protected ChromasthetiatorBase( ChromasthetiatorBase<? extends Flickr> other )
  {
    parent = other.parent;
    maxColors = other.maxColors;
    maxKeywords = other.maxKeywords;
    chromatikQuery = other.chromatikQuery.clone();
    synesthetiator = other.synesthetiator;
    palettes = other.palettes;
  }


  public void setFlickrApi( F flickr )
  {
    this.flickr = flickr;
  }


  public ChromatikResponse query( String text ) throws IOException
  {
    EmotionalState emoState = synesthetiator.synesthetiseDirect(text);
    ChromatikQuery chromatikQuery = this.chromatikQuery;
    chromatikQuery.keywords = getQueryKeywords(emoState);

    Random textRandom = new Random(text.hashCode());
    getQueryOptions(emoState, chromatikQuery.opts, textRandom);

    int queryStart = chromatikQuery.start;
    if (chromatikQuery.keywords.isEmpty() &&
      emoState.getStrongestEmotion().getType() == Emotion.NEUTRAL)
    {
      textRandom.setSeed(text.hashCode());
      chromatikQuery.randomizeRequestedSubset(
        EXPECTED_NEUTRAL_RESULT_COUNT, textRandom);
    }

    ChromatikResponse queryResult = chromatikQuery.getResult();
    chromatikQuery.start = queryStart;
    addFlickrPhotos(queryResult);

    return queryResult;
  }


  protected abstract String getQueryKeywords();


  protected String getQueryKeywords( EmotionalState synState )
  {
    String keywords = getQueryKeywords();
    if (keywords != null)
      return keywords;

    Emotion emo = synState.getStrongestEmotion();
    if (emo.getType() != Emotion.NEUTRAL) {
      keywords = Strings.join(findStrongestAffectWords(
        synState.getAffectWords(), maxKeywords), ' ');
      logger.log(Level.FINE, "Selected keywords: {0}", keywords);
    } else {
      keywords = "";
    }
    return keywords;
  }


  protected Map<Serializable, Serializable> getQueryOptions(
    EmotionalState synState, Map<Serializable, Serializable> opts,
    Random random )
  {
    Emotion emo = synState.getStrongestEmotion();

    // Derive color weight in search query from emotional weighting
    Double weight = max(sqrt(emo.getWeight()) * 0.5, 0.1) / maxColors;

    if (opts == null)
      opts = new HashMap<>();

    // Use (up to) maxColors random colors from palette for search query
    @SuppressWarnings({ "resource", "IOResourceOpenedButNotSafelyClosed" })
    final Formatter fmt =
      logger.isLoggable(Level.FINE) ?
        new Formatter(new StringBuilder("Colors:")) :
        null;

    int[] palette = palettes.getColors(emo);
    if (random != null)
      palette = shuffle(palette, random);

    for (int c: palette) {
      if (opts.size() >= maxColors)
        break;
      ChromatikColor cc = new ChromatikColor(c);
      if (opts.put(cc, weight) == null && fmt != null)
      {
        fmt.format(" #%06x (%s) at %.0f%%,",
          cc.value, cc.groupName, weight * 100);
      }
    }

    if (fmt != null) {
      StringBuilder sb = (StringBuilder) fmt.out();
      sb.setLength(sb.length() - 1);
      logger.log(Level.FINE, sb.toString());
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
      photo.parseUrl(imgInfo.squarethumbnailurl);
      return photo;
    }


    private static final String flickrErrorMessage =
      "Couldn't retrieve Flickr image sizes";

    @Override
    public SizeMap getSizes()
    {
      SizeMap sizes = super.getSizes();
      if (sizes == null) {
        try {
          sizes = getSizesThrow();
        } catch (FlickrException ex) {
          switch (ex.getErrorCode()) {
          case 1: // Photo not found
          case 2: // Permission denied
            logger.log(Level.FINER, "{0} for {1}: {2}",
              new Object[]{flickrErrorMessage, this, ex.getLocalizedMessage()});
            sizes = new SizeMap();
            break;

          default:
            logThrown(logger, Level.WARNING, "{0} for {1}", ex,
              new Object[]{flickrErrorMessage, this});
            break;
          }
        } catch (IOException ex) {
          logThrown(logger, Level.WARNING, "{0} for {1}", ex,
            new Object[]{flickrErrorMessage, this});
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


  private static List<String> findStrongestAffectWords(
    Collection<AffectWord> affectWords, int maxCount)
  {
    if (maxCount < 0 || maxCount > affectWords.size())
      maxCount = affectWords.size();
    if (maxCount == 0)
      return Collections.emptyList();

    ArrayList<String> resultWords = new ArrayList<>(maxCount);
    Comparator<AffectWord> comp = AffectWord.SquareWeightSumComparator.INSTANCE;

    if (maxCount == 1) {
      resultWords.add(Collections.max(affectWords, comp).getWord());
    } else {
      AffectWord[] a = affectWords.toArray(new AffectWord[affectWords.size()]);
      Arrays.sort(a, comp);
      final int limit = affectWords.size() - maxCount;
      for (int i = affectWords.size() - 1; i >= limit; i--)
        resultWords.add(a[i].getWord());
    }
    return resultWords;
  }
}
