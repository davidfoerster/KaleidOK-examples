package kaleidok.exaleads.chromatik;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.exaleads.chromatik.data.ChromatikColor;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.Photo;
import kaleidok.flickr.SizeMap;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.adapter.preference.IntegerPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kaleidok.util.Arrays.shuffle;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public class Chromasthetiator<F extends Flickr>
  implements Cloneable, PreferenceBean
{
  static final Logger logger =
    Logger.getLogger(ChromasthetiationService.class.getPackage().getName());

  private IntegerProperty maxColors;

  private IntegerProperty maxKeywords;

  public static int EXPECTED_NEUTRAL_RESULT_COUNT = 10000;

  public ChromatikQuery chromatikQuery;

  protected SynesthetiatorEmotion synesthetiator;

  protected SynesketchPalette palettes;

  protected F flickr;


  private Chromasthetiator( int maxColors, int maxKeywords )
  {
    AspectedIntegerProperty maxColorsProp =
      new AspectedIntegerProperty(this, "max. colors", maxColors);
    maxColorsProp.addAspect(BoundedIntegerTag.INSTANCE,
      new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
    maxColorsProp.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(maxColorsProp, Chromasthetiator.class));
    this.maxColors = maxColorsProp;

    AspectedIntegerProperty maxKeywordsProp =
      new AspectedIntegerProperty(this, "max. keywords", maxKeywords);
    maxKeywordsProp.addAspect(BoundedIntegerTag.INSTANCE,
      new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
    maxKeywordsProp.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(maxKeywordsProp, Chromasthetiator.class));
    this.maxKeywords = maxKeywordsProp;
  }


  public Chromasthetiator()
  {
    this(2, 0);
    try {
      synesthetiator = new SynesthetiatorEmotion();
    } catch (IOException ex) {
      throw new Error(ex);
    }
    palettes = new SynesketchPalette("standard");
    chromatikQuery = new ChromatikQuery(10, null, null);
  }


  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public IntegerProperty maxColorsProperty()
  {
    return maxColors;
  }


  /**
   * Maximum amount of keywords to select from affect words, if no search terms
   * are specified in {@link ChromatikQuery#keywords}.
   */
  public IntegerProperty maxKeywordsProperty()
  {
    return maxKeywords;
  }

  public int getMaxKeywords()
  {
    return maxKeywords.get();
  }

  public void setMaxKeywords( int n )
  {
    maxKeywords.set(n);
  }


  public void setFlickrApi( F flickr )
  {
    this.flickr = flickr;
  }


  public ChromatikResponse query( String text ) throws IOException
  {
    EmotionalState emoState = synesthetiator.synesthetiseDirect(text);
    ChromatikQuery chromatikQuery = this.chromatikQuery;
    chromatikQuery.setKeywords(getQueryKeywords(emoState));

    Random textRandom = new Random(text.hashCode());
    getQueryOptions(emoState, chromatikQuery.opts, textRandom);

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
    String keywords = chromatikQuery.getKeywords();
    if (keywords.isEmpty())
    {
      Emotion emo = synState.getStrongestEmotion();
      if (emo.getType() != Emotion.NEUTRAL)
      {
        keywords = String.join(" ", findStrongestAffectWords(
          synState.getAffectWords(), maxKeywords.get()));
        logger.log(Level.FINE, "Selected keywords: {0}", keywords);
      }
    }
    return keywords;
  }


  protected Map<Serializable, Serializable> getQueryOptions(
    EmotionalState synState, Map<Serializable, Serializable> opts,
    Random random )
  {
    if (opts == null)
      opts = new HashMap<>();

    int maxColors = this.maxColors.get();
    if (maxColors > 0)
    {
      Emotion emo = synState.getStrongestEmotion();

      // Derive color weight in search query from emotional weighting
      Double weight = max(sqrt(emo.getWeight()) * 0.5, 0.1) / maxColors;

      // Use (up to) maxColors random colors from palette for search query
      Formatter fmt = null;
      int[] palette = palettes.getColors(emo);
      if (random != null)
        palette = shuffle(palette, random);

      for (int c : palette)
      {
        if (opts.size() >= maxColors)
          break;
        ChromatikColor cc = new ChromatikColor(c);
        if (opts.put(cc, weight) == null)
        {
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


  @SuppressWarnings("CloneCallsConstructors")
  @Override
  public Chromasthetiator<F> clone()
  {
    Chromasthetiator<F> clone;
    try
    {
      //noinspection unchecked
      clone = (Chromasthetiator<F>) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }

    clone.maxColors = new SimpleIntegerProperty(
      clone, clone.maxColors.getName(), clone.maxColors.get());
    clone.maxKeywords = new SimpleIntegerProperty(
      clone, clone.maxKeywords.getName(), clone.maxKeywords.get());
    if (clone.chromatikQuery != null)
      clone.chromatikQuery = clone.chromatikQuery.clone();

    return clone;
  }


  @Override
  public String getName()
  {
    return "Chromasthetiator";
  }


  @Override
  public Object getParent()
  {
    return null;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
  return
    (maxColors instanceof AspectedReadOnlyProperty &&
      maxKeywords instanceof AspectedReadOnlyProperty)
    ?
      Stream.of(
        ((AspectedReadOnlyProperty<?>) maxColors).getAspect(
          PropertyPreferencesAdapterTag.getWritableInstance()),
        ((AspectedReadOnlyProperty<?>) maxKeywords).getAspect(
          PropertyPreferencesAdapterTag.getWritableInstance())) :
      Stream.empty();
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
