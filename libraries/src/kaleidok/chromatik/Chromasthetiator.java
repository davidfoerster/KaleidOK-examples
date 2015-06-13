package kaleidok.chromatik;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import kaleidok.awt.ReadyImageFuture;
import kaleidok.concurrent.Callback;
import kaleidok.util.Strings;
import synesketch.*;
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


public class Chromasthetiator implements UpdateHandler
{
  // Configuration:
  public static int verbose = 0;

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


  private final Applet parent;

  public ChromatikQuery chromatikQuery;

  public Callback<ChromatikResponse> searchResultHandler = null;

  private Synesthetiator synesthetiator;

  private SynesketchPalette palettes;

  private PhotosInterface flickrPhotos = null;


  public Chromasthetiator( Applet parent, Callback<ChromatikResponse> srh )
    throws IOException
  {

    this.parent = parent;
    this.searchResultHandler = srh;

    synesthetiator = new SynesthetiatorEmotion(this);
    palettes = new SynesketchPalette("standard");

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;
  }


  public void setFlickrApi( Flickr flickr )
  {
    flickrPhotos = flickr.getPhotosInterface();
  }


  public void issueQuery( String text ) throws Exception
  {
    synesthetiator.synesthetise(text);
  }


  /**
   * Callback method for {@link #synesthetiator}
   *
   * @param synState  The emotion analysis result
   */
  public void synesketchUpdate( SynesketchState synState )
  {
    EmotionalState emoState = (EmotionalState) synState;
    ChromatikQuery chromatikQuery = this.chromatikQuery;
    chromatikQuery.keywords = getQueryKeywords(emoState);
    getQueryOptions(emoState, chromatikQuery.opts);

    ChromatikResponse queryResult;
    try {
      // TODO: Don't do this in the event handler thread
      queryResult = chromatikQuery.getResult();
    } catch (IOException ex) {
      ex.printStackTrace();
      return;
    }
    addFlickrPhotos(queryResult);

    Callback<ChromatikResponse> searchResultHandler = this.searchResultHandler;
    if (searchResultHandler != null)
      searchResultHandler.call(queryResult);
  }


  private String getQueryKeywords( EmotionalState synState )
  {
    Document keywordsDoc = this.keywordsDoc;
    if (keywordsDoc != null && keywordsDoc.getLength() != 0) {
      try {
        return keywordsDoc.getText(0, keywordsDoc.getLength());
      } catch (BadLocationException e) {
        // this really shouldn't happen with the chosen location
        throw new Error(e);
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


  private class FlickrPhotoImpl extends kaleidok.chromatik.FlickrPhoto
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
