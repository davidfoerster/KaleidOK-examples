package kaleidok.examples.kaleidoscope.chromatik;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;


public class Chromasthetiator implements UpdateHandler
{
  // Configuration:

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  public int maxKeywords = 1;

  // other instance attributes:

  public Document keywordsDoc;

  private final PApplet parent;

  public ChromatikQuery chromatikQuery;

  public SearchResultHandler searchResultHandler = null;

  private ArrayList<PImage> resultSet;

  private Synesthetiator synesthetiator;

  private SynesketchPalette palettes;

  private EmotionalState synState = null;

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
    for (int c: shuffleArray(palettes.getColors(emo))) {
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

  private static final Pattern FLICKR_URL_PATTERN = Pattern.compile(
    "(?:_[khbcznmqts])?\\.(jpg|gif|png)$", Pattern.CASE_INSENSITIVE);

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
  private PImage getFlickrImage( JSONObject imgInfo )
  {
    String thumbnailUrl = imgInfo.getString("squarethumbnailurl");
    Matcher m = FLICKR_URL_PATTERN.matcher(thumbnailUrl);
    if (m.find())
    {
      String extension = thumbnailUrl.substring(m.start(1) - 1);
      StringBuilder url = new StringBuilder(m.start() + 6);
      url.append(thumbnailUrl, 0, m.start());
      for (byte size: FLICKR_URL_SIZESUFFIXES)
      {
        if (size != 0)
          url.append('_').append((char) size);
        url.append(extension);

        String urlStr = url.toString();
        PImage img = parent.loadImage(urlStr);
        if (img != null && img.width > 0 && img.height > 0) {
          System.out.println(imgInfo.getString("title") + ' ' + '(' + urlStr + ')');
          return img;
        }

        url.setLength(m.start());
      }
    }
    return null;
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

  static String[] findStrongestAffectWords(List<AffectWord> affectWords, int maxCount)
  {
    AffectWord[] a = affectWords.toArray(new AffectWord[affectWords.size()]);
    Arrays.sort(a, AffectWord.WeightSumComparator.getReverseInstance());

    if (maxCount < 0 || maxCount > a.length)
      maxCount = a.length;
    String[] resultWords = new String[maxCount];
    for (int i = 0; i < maxCount; i++)
      resultWords[i] = a[i].getWord();

    return resultWords;
  }

  static String joinStrings(String[] ar, char separator)
  {
    if (ar.length == 0)
      return "";
    if (ar.length == 1)
      return ar[0];

    int len = ar.length - 1;
    for (String s: ar)
      len += s.length();
    StringBuilder sb = new StringBuilder(len);

    sb.append(ar[0]);
    for (int i = 1; i < ar.length; i++)
      sb.append(separator).append(ar[i]);

    return sb.toString();
  }


  public interface SearchResultHandler {
    void handleChromatikResult( JSONArray queryResult, List<PImage> resultSet );
  }

}
