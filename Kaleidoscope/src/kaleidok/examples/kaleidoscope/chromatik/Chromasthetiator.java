package kaleidok.examples.kaleidoscope.chromatik;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import kaleidok.util.chromatik.ChromatikColor;
import kaleidok.util.chromatik.ChromatikQuery;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import synesketch.SynesketchState;
import synesketch.Synesthetiator;
import synesketch.UpdateHandler;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.AffectWord;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;
import synesketch.emotion.SynesthetiatorEmotion;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;


public class Chromasthetiator implements UpdateHandler
{
  // Configuration:

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  public int maxKeywords = 0;

  // other instance attributes:

  public Document keywordsDoc;

  private final PApplet parent;

  public ChromatikQuery chromatikQuery;

  public SearchResultHandler searchResultHandler = null;

  private ArrayList<PImage> resultSet;

  private Synesthetiator synesthetiator;

  private SynesketchPalette palettes;

  private EmotionalState synState = null;

  private PhotosInterface flickrPhotos = null;

  private final Random rnd = new Random();

  public Chromasthetiator( PApplet parent, SearchResultHandler srh )
  {

    this.parent = parent;
    this.searchResultHandler = srh;

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;
  }

  public void setup()
  {
    try {
      synesthetiator = new SynesthetiatorEmotion(this);
      palettes = new SynesketchPalette("standard");
    } catch (Exception ex) {
      ex.printStackTrace();
      parent.exit();
      return;
    }

    resultSet = new ArrayList<>(chromatikQuery.nhits);
  }

  public void issueQuery( String text ) throws Exception
  {
    synesthetiator.synesthetise(text);
  }

  /**
   * Callback method for {@link #synesthetiator}
   *
   * @param state  The emotion analysis result
   */
  @SuppressWarnings("UnusedDeclaration")
  public void synesketchUpdate( SynesketchState state )
  {
    synState = (EmotionalState) state;
    updateQuery();

    JSONArray queryResult = chromatikQuery.getResult();
    updateResultSet(queryResult);

    SearchResultHandler searchResultHandler = this.searchResultHandler;
    if (searchResultHandler != null)
      searchResultHandler.handleChromatikResult(queryResult, resultSet);

    /*
     * The Processing API says, we shouldn't call draw() directly, but this is
     * called from inside draw() already, so the redraw flag is deleted
     * afterwards and in this case it doesn't hurt to just call draw() again.
     */
    //redraw();
    //draw();
  }

  private void updateQuery()
  {
    Emotion emo = synState.getStrongestEmotion();

    if (keywordsDoc != null && keywordsDoc.getLength() != 0) {
      try {
        chromatikQuery.keywords = keywordsDoc.getText(0, keywordsDoc.getLength());
      } catch (BadLocationException e) {
        // this really shouldn't happen with the chosen location
        throw new Error(e);
      }
    } else if (emo.getType() != Emotion.NEUTRAL) {
      chromatikQuery.keywords =
        joinStrings(findStrongestAffectWords(
          synState.getAffectWords(), maxKeywords), ' ');
      System.out.println("Selected keywords: " + chromatikQuery.keywords);
    } else {
      chromatikQuery.keywords = "";
    }


    // Derive color weight in search query from emotional weighting
    Float weight = max((float) sqrt(emo.getWeight()) * 0.5f, 0.1f) / maxColors;

    // Use (up to) maxColors random colors from palette for search query
    chromatikQuery.opts.clear();
    System.out.print("Colors:");
    for (int c: palettes.getColors(emo)) {
      if (chromatikQuery.opts.size() == maxColors)
        break;
      ChromatikColor cc = new ChromatikColor(c);
      System.out.format(" #%06x (%s),", cc.value, cc.groupName);
      chromatikQuery.opts.put(cc, weight);
    }
    System.out.println();
  }

  private void updateResultSet( JSONArray a )
  {
    // get number of elements in array
    int length = a.size();
    resultSet.clear();
    resultSet.ensureCapacity(length - 1);

    // array index 0 contains number total search hits
    System.out.println("Hits: " + a.getInt(0));

    // loop over result set, starting from index 1(!)
    for (int i = 1; i < length; i++) {
      // get image object at index i
      JSONObject imgInfo = a.getJSONObject(i);

      // download image
      PImage img = getFlickrImage(imgInfo);
      if (img != null)
        resultSet.add(img);
    }
  }


  private PImage getFlickrImage( JSONObject imgInfo )
  {
    if (flickrPhotos == null)
      return getFlickrImageNoApi(imgInfo);

    String thumbnailUrl = imgInfo.getString("squarethumbnailurl");
    int start = thumbnailUrl.lastIndexOf('/');
    if (start >= 0) {
      start += 1;
      int end = thumbnailUrl.indexOf('_', start);
      if (end >= 0) {
        String photoId = thumbnailUrl.substring(start, end);
        try {
          Collection<Size> sizes = flickrPhotos.getSizes(photoId);
          if (!sizes.isEmpty()) {
            Size maxSize = Collections.max(sizes, PhotoSizeComparator.getInstance());
            PImage img = parent.loadImage(maxSize.getSource());
            if (img != null && img.width > 0 && img.height > 0) {
              System.out.println(getImageTitle(imgInfo) + ' ' + '(' + maxSize.getSource() + ')');
              return img;
            }
          }
        } catch (FlickrException ex) {
          System.err.println(ex.getLocalizedMessage());
        }
      }
    }
    return null;
  }

  public static class PhotoSizeComparator implements Comparator<Size>
  {
    private static PhotoSizeComparator instance = null;

    public static PhotoSizeComparator getInstance()
    {
      if (instance == null)
        instance = new PhotoSizeComparator();
      return instance;
    }

    private PhotoSizeComparator() { }

    @Override
    public int compare( Size o1, Size o2 )
    {
      return Integer.compare(getSize(o1), getSize(o2));
    }

    private static int getSize( Size o )
    {
      return o.getWidth() * o.getHeight();
    }
  }

  private static final byte[] FLICKR_URL_SIZESUFFIXES = {
      'k', 'h', 'b', 'c', 'z', 0, 'n', 'm', 'q', 't', 's'
    };

  /**
   * Returns the the full-sized image.
   *
   * @param imgInfo  A Chromatik response object with the image info
   * @return  the full-sized image; or <code>null</code>, if none could be
   *   retrieved
   */
  private PImage getFlickrImageNoApi( JSONObject imgInfo )
  {
    String thumbnailUrl = imgInfo.getString("squarethumbnailurl");
    int len = thumbnailUrl.length();
    if (thumbnailUrl.charAt(len - 4) == '.') {
      len -= 4;
      String extension = thumbnailUrl.substring(len);
      if (thumbnailUrl.charAt(len - 2) == '_')
        len -= 2;
      StringBuilder url = new StringBuilder(len);
      url.append(thumbnailUrl, 0, len);
      for (byte size: FLICKR_URL_SIZESUFFIXES)
      {
        if (size != 0)
          url.append('_').append((char) size);
        url.append(extension);

        String urlStr = url.toString();
        PImage img = parent.loadImage(urlStr);
        if (img != null && img.width > 0 && img.height > 0) {
          System.out.println(getImageTitle(imgInfo) + ' ' + '(' + urlStr + ')');
          return img;
        }

        url.setLength(len);
      }
    }
    return null;
  }

  private static String getImageTitle( JSONObject imgInfo )
  {
    return imgInfo.getString("title", "<untitled>");
  }


  public void setFlickrApi( Flickr flickr )
  {
    flickrPhotos = flickr.getPhotosInterface();
  }


  private int[] shuffleArray(int[] ar)
  {
    ar = ar.clone();

    for (int i = ar.length - 1; i > 0; i--)
    {
      int index = rnd.nextInt(i + 1);

      // Simple swap
      int a = ar[index];
      ar[index] = ar[i];
      ar[i] = a;
    }

    return ar;
  }

  static List<String> findStrongestAffectWords(List<AffectWord> affectWords, int maxCount)
  {
    if (maxCount < 0 || maxCount > affectWords.size())
      maxCount = affectWords.size();
    if (maxCount == 0)
      return Collections.EMPTY_LIST;

    ArrayList<String> resultWords = new ArrayList<>(maxCount);
    Comparator<AffectWord> comp = AffectWord.WeightSumComparator.getInstance();

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

  static String joinStrings(Collection<String> ar, char separator)
  {
    if (ar.isEmpty())
      return "";

    Iterator<String> it = ar.iterator();
    if (ar.size() == 1)
      return it.next();

    int len = ar.size() - 1;
    while (it.hasNext())
      len += it.next().length();
    StringBuilder sb = new StringBuilder(len);

    it = ar.iterator();
    sb.append(it.next());
    while (it.hasNext())
      sb.append(separator).append(it.next());

    return sb.toString();
  }


  public interface SearchResultHandler {
    void handleChromatikResult( JSONArray queryResult, List<PImage> resultSet );
  }

}
