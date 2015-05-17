import chromatik.ChromatikColor;
import chromatik.ChromatikQuery;
import com.getflourish.stt.STT;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import synesketch.SynesketchState;
import synesketch.Synesthetiator;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.AffectWord;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;
import synesketch.emotion.SynesthetiatorEmotion;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.*;


public class SpeechChromasthetiator
{
  // Configuration:

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  public int maxKeywords = 1;

  public final boolean autoRecord = false;

  // other instance attributes:

  public Document keywordsDoc;

  private final Kaleidoscope parent;

  ChromatikQuery chromatikQuery;

  ArrayList<PImage> resultSet;

  private STT stt;

  private Synesthetiator synesthetiator;

  private SynesketchPalette palettes;

  EmotionalState synState = null;

  private final Random rnd = new Random();

  public SpeechChromasthetiator(Kaleidoscope parent)
  {
    this.parent = parent;
  }

  public void setup()
  {
    String googleSpeechApiKey;
    try {
      googleSpeechApiKey = new String(parent.loadBytes("api-key.txt")).trim();
      synesthetiator = new SynesthetiatorEmotion(parent);
      palettes = new SynesketchPalette("standard");
    } catch (Exception ex) {
      ex.printStackTrace();
      parent.exit();
      return;
    }

    stt = new STT(parent, googleSpeechApiKey);
    stt.enableDebug();
    stt.setLanguage("en");

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;

    resultSet = new ArrayList<PImage>(chromatikQuery.nhits);

    if (autoRecord) {
      stt.enableAutoRecord();
    } else {
      //parent.noLoop();
    }
  }

  public void keyPressed ()
  {
    if (!autoRecord) {
      //parent.loop();
      stt.begin();
    }
  }

  public void keyReleased ()
  {
    if (!autoRecord) {
      stt.end();
    }
  }

  /**
   * Callback method for {@link #stt}
   *
   * @param utterance  The most likely string representation of the uttered
   *   words
   * @param confidence  The confidence of the transcription algorithm about the
   *   correctness of the utterance
   */
  @SuppressWarnings("UnusedDeclaration")
  public void transcribe( String utterance, float confidence )
  {
    if (!autoRecord) {
      //parent.noLoop();
    }

    //println("Transcription finished: " + utterance + ' ' + '(' + confidence + ')');

    try {
      synesthetiator.synesthetise(utterance);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
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
    updateResultSet(chromatikQuery.getResult());

    if (!resultSet.isEmpty()) {
      parent.layers[parent.layers.length - 1].currentImage = resultSet.get(0);
    }

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
    } else {
      chromatikQuery.keywords = "";
    }


    // Derive color weight in search query from emotional weighting
    Float weight =
      Math.max((float) Math.sqrt(emo.getWeight()) * 0.5f, 0.1f) / maxColors;

    // Use (up to) maxColors random colors from palette for search query
    chromatikQuery.opts.clear();
    for (int c: shuffleArray(palettes.getColors(emo))) {
      if (chromatikQuery.opts.size() == maxColors)
        break;
      chromatikQuery.opts.put(new ChromatikColor(c), weight);
    }
  }

  private void updateResultSet( JSONArray a )
  {
    // get number of elements in array
    int length = a.size();
    resultSet.clear();
    resultSet.ensureCapacity(length - 1);

    // array index 0 contains number total search hits
    PApplet.println("Hits: " + a.getInt(0));

    // loop over result set, starting from index 1(!)
    for (int i = 1; i < length; i++) {
      // get image object at index i
      JSONObject imgInfo = a.getJSONObject(i);

      // the image title is stored with the key "title"
      String title = imgInfo.getString("title", "<untitled>");

      // the thumbnail URL is stored under "squarethumbnailurl"
      String thumbnailUrl = imgInfo.getString("squarethumbnailurl");
      PApplet.println(title + ' ' + '(' + thumbnailUrl + ')');

      // download image
      PImage img = parent.loadImage(thumbnailUrl);
      if (img != null && img.width > 0 && img.height > 0)
        resultSet.add(img);
    }
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
    affectWords = new ArrayList<AffectWord>(affectWords);
    Collections.sort(affectWords, AffectWord.WeightSumComparator.getInstance());

    if (maxCount < 0 || maxCount > affectWords.size())
      maxCount = affectWords.size();
    String[] resultWords = new String[maxCount];
    for (int i = 0; i < maxCount; i++)
      resultWords[i] = affectWords.get(i).getWord();

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

}
