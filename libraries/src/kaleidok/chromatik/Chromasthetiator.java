package kaleidok.chromatik;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import kaleidok.awt.ReadyImageFuture;
import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.chromatik.data.FlickrPhoto;
import kaleidok.util.Strings;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.applet.Applet;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kaleidok.util.Arrays.shuffle;


public class Chromasthetiator
{
  // Configuration:
  public static int verbose = 0;

  protected final Applet parent;

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  /**
   * Maximum amount of keywords to select from affect words, if no search terms
   * are defined by {@link #keywordsDoc}
   */
  public int maxKeywords = 0;

  // other instance attributes:
  /**
   * A document object tha holds the currently defined search terms
   */
  public Document keywordsDoc;

  public ChromatikQuery chromatikQuery;

  protected SynesthetiatorEmotion synesthetiator;

  protected SynesketchPalette palettes;

  private PhotosInterface flickrPhotos = null;


  public Chromasthetiator( Applet parent ) throws IOException
  {
    this.parent = parent;

    palettes = new SynesketchPalette("standard");
    synesthetiator = new SynesthetiatorEmotion();

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;
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


  public void setFlickrApi( Flickr flickr )
  {
    flickrPhotos = flickr.getPhotosInterface();
  }


  public ChromatikResponse query( String text ) throws IOException
  {
    EmotionalState emoState = synesthetiator.synesthetiseDirect(text);
    ChromatikQuery chromatikQuery = this.chromatikQuery;
    chromatikQuery.keywords = getQueryKeywords(emoState);
    getQueryOptions(emoState, chromatikQuery.opts);

    ChromatikResponse queryResult;
    // TODO: Don't do this in the event handler thread
    queryResult = chromatikQuery.getResult();
    addFlickrPhotos(queryResult);

    return queryResult;
  }


  private String getQueryKeywords( EmotionalState synState )
  {
    Document keywordsDoc = this.keywordsDoc;
    if (keywordsDoc != null) {
      try {
        String keywords =  keywordsDoc.getText(0, keywordsDoc.getLength());
        if (!keywords.isEmpty())
          return keywords;
      } catch (BadLocationException ex) {
        // this really shouldn't happen with the chosen location
        throw new AssertionError(ex);
      }
    }

    Emotion emo = synState.getStrongestEmotion();
    if (emo.getType() != Emotion.NEUTRAL) {
      String keywords =
        Strings.join(findStrongestAffectWords(
          synState.getAffectWords(), maxKeywords), ' ');
      if (verbose >= 1)
        System.out.println("Selected keywords: " + keywords);
    }

    return "";
  }


  private Map<Object, Object> getQueryOptions( EmotionalState synState, Map<Object, Object> opts )
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
      imgInfo.flickrPhoto = new FlickrPhotoImpl(imgInfo);
  }


  private class FlickrPhotoImpl extends FlickrPhoto
  {
    public FlickrPhotoImpl() { }

    public FlickrPhotoImpl( ChromatikResponse.Result imgInfo )
    {
      super(imgInfo.squarethumbnailurl);
    }

    @Override
    public Collection<Size> getSizes()
    {
      Collection<Size> sizes = super.getSizes();
      if (sizes == null) {
        try {
          sizes = flickrPhotos.getSizes(getId());
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
        setSizes(sizes);
      }
      return sizes;
    }

    protected ReadyImageFuture loadImage( URL url )
    {
      return ReadyImageFuture.createInstance(parent, parent.getImage(url));
    }
  }
}
