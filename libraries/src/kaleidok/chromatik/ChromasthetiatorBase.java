package kaleidok.chromatik;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.flickr.Photo;
import kaleidok.util.Strings;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.*;

import java.applet.Applet;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kaleidok.util.Arrays.shuffle;


public abstract class ChromasthetiatorBase implements Chromasthetiator
{
  // Configuration:

  public static int verbose = 0;

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  /**
   * Maximum amount of keywords to select from affect words, if no search terms
   * are returned by {@link #getQueryKeywords()}
   */
  public int maxKeywords = 0;

  // other instance attributes:

  public ChromatikQuery chromatikQuery;

  protected final Applet parent;

  protected SynesthetiatorEmotion synesthetiator;

  protected SynesketchPalette palettes;

  private PhotosInterface flickrPhotos = null;


  public ChromasthetiatorBase( Applet parent )
  {
    this.parent = parent;

    try {
      synesthetiator = new SynesthetiatorEmotion();
    } catch (IOException ex) {
      throw new Error(ex);
    }
    palettes = new SynesketchPalette("standard");

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;
  }


  protected ChromasthetiatorBase( ChromasthetiatorBase other )
  {
    parent = other.parent;
    maxColors = other.maxColors;
    maxKeywords = other.maxKeywords;
    chromatikQuery = new ChromatikQuery(other.chromatikQuery);
    synesthetiator = other.synesthetiator;
    palettes = other.palettes;
    flickrPhotos = other.flickrPhotos;
  }


  public void setFlickrApi( Flickr flickr )
  {
    flickrPhotos = flickr.getPhotosInterface();
  }


  @Override
  public ChromatikResponse query( String text ) throws IOException
  {
    EmotionalState emoState = synesthetiator.synesthetiseDirect(text);
    ChromatikQuery chromatikQuery = this.chromatikQuery;
    chromatikQuery.keywords = getQueryKeywords(emoState);
    getQueryOptions(emoState, chromatikQuery.opts);

    ChromatikResponse queryResult = chromatikQuery.getResult();
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
      if (verbose >= 1)
        System.out.println("Selected keywords: " + keywords);
    } else {
      keywords = "";
    }
    return keywords;
  }


  protected Map<Object, Object> getQueryOptions( EmotionalState synState,
    Map<Object, Object> opts )
  {
    Emotion emo = synState.getStrongestEmotion();

    // Derive color weight in search query from emotional weighting
    Float weight = max((float) sqrt(emo.getWeight()) * 0.5f, 0.1f) / maxColors;

    if (opts == null)
      opts = new HashMap<>();

    // Use (up to) maxColors random colors from palette for search query
    if (verbose >= 1)
      System.out.print("Colors:");
    for (int c: shuffle(palettes.getColors(emo), new Random(synState.getText().hashCode()))) {
      if (opts.size() >= maxColors)
        break;
      ChromatikColor cc = new ChromatikColor(c);
      if (opts.put(cc, weight) == null && verbose >= 1)
        System.out.format(" #%06x (%s),", cc.value, cc.groupName);
    }
    if (verbose >= 1)
      System.out.println();

    return opts;
  }


  private void addFlickrPhotos( ChromatikResponse response )
  {
    if (verbose >= 1)
      System.out.println("Hits: " + response.hits);

    for (ChromatikResponse.Result imgInfo: response.results)
      imgInfo.flickrPhoto = new FlickrPhoto(imgInfo);
  }


  protected class FlickrPhoto extends Photo
  {
    public FlickrPhoto() { }

    public FlickrPhoto( ChromatikResponse.Result imgInfo )
    {
      super(imgInfo.squarethumbnailurl);
    }


    @Override
    public Collection<Size> getSizes()
    {
      Collection<Size> sizes = super.getSizes();
      if (sizes == null) {
        try {
          sizes = getSizesThrow();
        } catch (FlickrException ex) {
          switch (Integer.parseInt(ex.getErrorCode())) {
          case 1: // Photo not found
          case 2: // Permission denied
            //noinspection unchecked
            sizes = Collections.EMPTY_LIST;
            System.err.println(ex.getLocalizedMessage() + ' ' + '(' + getMediumUrl() + ')');
            break;

          default:
            ex.printStackTrace();
            return null;
          }
        }
      }
      return sizes;
    }


    @Override
    public Collection<Size> getSizesThrow() throws FlickrException
    {
      Collection<Size> sizes = super.getSizes();
      if (sizes == null) {
        try {
          sizes = flickrPhotos.getSizes(getId());
        } catch (FlickrException ex) {
          throw new FlickrException(ex.getErrorCode(),
            ex.getErrorMessage() + ':' + ' ' + getMediumUrl());
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
    if (maxCount == 0) {
      //noinspection unchecked
      return Collections.EMPTY_LIST;
    }

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
