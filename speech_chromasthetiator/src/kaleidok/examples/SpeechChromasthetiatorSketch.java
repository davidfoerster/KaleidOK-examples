package kaleidok.examples;

import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.ChromatikQuery;
import com.getflourish.stt.STT;
import kaleidok.chromatik.data.ChromatikResponse;
import processing.core.PApplet;
import processing.core.PImage;
import synesketch.SynesketchState;
import synesketch.Synesthetiator;
import synesketch.art.util.SynesketchPalette;
import synesketch.emotion.AffectWord;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;
import synesketch.emotion.SynesthetiatorEmotion;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.IOException;
import java.util.*;


public class SpeechChromasthetiatorSketch extends PApplet
{
  // Configuration:

  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

  public int maxKeywords = 1;

  public final boolean autoRecord = true;

  // other instance attributes:

  public Document keywordsDoc;

  private ChromatikQuery chromatikQuery;

  private ArrayList<PImage> resultSet;

  private STT stt;

  private Synesthetiator synesthetiator;

  private SynesketchPalette palettes;

  private EmotionalState synState = null;

  private final Random rnd = new Random();

  @Override
  public void setup()
  {
    String googleSpeechApiKey;
    try {
      googleSpeechApiKey = new String(loadBytes("api-key.txt")).trim();
      synesthetiator = new SynesthetiatorEmotion(this);
      palettes = new SynesketchPalette("standard");
    } catch (Exception ex) {
      ex.printStackTrace();
      exit();
      return;
    }

    stt = new STT(this, googleSpeechApiKey);
    stt.enableDebug();
    stt.setLanguage("en");

    chromatikQuery = new ChromatikQuery();
    chromatikQuery.nhits = 10;

    resultSet = new ArrayList<PImage>(chromatikQuery.nhits);

    size(800, 200);

    if (autoRecord) {
      stt.enableAutoRecord();
    } else {
      noLoop();
    }
  }

  @Override
  public void draw()
  {
    background(255);
    drawQuery();
    drawResultSet();
  }

  private void drawQuery()
  {
    int y = 0;

    if (synState != null) {
      // draw uttered text
      fill(0);
      text("keywords: " + chromatikQuery.keywords + ',' + ' ' +
          synState.getStrongestEmotion().toString(),
        5, y += 20);
      text(synState.getText(), 5, y += 20);
    }

    // rectangle frame for color composition
    int x = 5;
    stroke(0);
    noFill();
    rect(x - 1, y += 4, 200, 26);
    noStroke();

    y += 1;
    for (Map.Entry<Object, Object> o: chromatikQuery.opts.entrySet()) {
      if (o.getKey() instanceof ChromatikColor) {
        ChromatikColor c = (ChromatikColor) o.getKey();

        /*
         * draw a filled rectangle for each color with a width relative to its
         * weight
         */
        fill(c.value | 0xff000000);
        int width = (int)((Float) o.getValue() * 100) * 2;
        rect(x, y, width, 25);

        // draw color group name over color rectangle
        fill((brightness(c.value) < 128) ? 255 : 0);
        text(c.groupName, x + 1, y + 20);

        x += width;
      }
    }
  }

  private void drawResultSet()
  {
    int imgXpos = 5;
    for (PImage img: resultSet) {
      image(img, imgXpos, 80, 75, 75);
      imgXpos += 75 + 5;
    }
  }

  @Override
  public void keyPressed ()
  {
    if (!autoRecord) {
      loop();
      stt.begin();
    }
  }

  @Override
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
    if (!autoRecord)
      noLoop();

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
    try {
      updateResultSet(chromatikQuery.getResult());
    } catch (IOException ex) {
      ex.printStackTrace();
      return;
    }

    /*
     * The Processing API says, we shouldn't call draw() directly, but this is
     * called from inside draw() already, so the redraw flag is deleted
     * afterwards and in this case it doesn't hurt to just call draw() again.
     */
    //redraw();
    draw();
  }

  private void updateQuery()
  {
    Emotion emo = synState.getStrongestEmotion();

    if (keywordsDoc != null && keywordsDoc.getLength() != 0) {
      try {
        chromatikQuery.keywords = keywordsDoc.getText(0, keywordsDoc.getLength());
      } catch (BadLocationException e) {
        // this really shouldn't happen with the chosen location
        throw new AssertionError(e);
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

  private void updateResultSet( ChromatikResponse response )
  {
    resultSet.clear();
    resultSet.ensureCapacity(response.results.length);

    // array index 0 contains number total search hits
    println("Hits: " + response.hits);

    // loop over result set
    for (ChromatikResponse.Result imgInfo: response.results) {
      // the image title is stored with the key "title"
      String title = (imgInfo.title != null) ? imgInfo.title : "<untitled>";

      // the thumbnail URL is stored under "squarethumbnailurl"
      String thumbnailUrl = imgInfo.squarethumbnailurl;
      println(title + '(' + ' ' + thumbnailUrl + ')');

      // download image
      PImage img = loadImage(thumbnailUrl);
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

  public static void main( String... args )
  {
    new SpeechChromasthetiatorSketch().runSketch(args);
  }
}
