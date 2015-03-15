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
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;
import synesketch.emotion.SynesthetiatorEmotion;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;


public class SpeechChromasthetiatorSketch extends PApplet
{
  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public int maxColors = 2;

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
    noLoop();
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
    loop();
    stt.begin();
  }

  @Override
  public void keyReleased ()
  {
    stt.end();
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
    updateResultSet(chromatikQuery.getResult());

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
    try {
      if (keywordsDoc != null && keywordsDoc.getLength() != 0) {
        chromatikQuery.keywords = keywordsDoc.getText(0, keywordsDoc.getLength());
      } else {
        chromatikQuery.keywords = synState.getStrongestEmotion().getTypeName();
      }
    } catch (BadLocationException ex) {
      throw new Error(ex);
    }

    Emotion emo = synState.getStrongestEmotion();

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
    println("Hits: " + a.getInt(0));

    // loop over result set, starting from index 1(!)
    for (int i = 1; i < length; i++) {
      // get image object at index i
      JSONObject imgInfo = a.getJSONObject(i);

      // the image title is stored with the key "title"
      String title = imgInfo.getString("title", "<untitled>");

      // the thumbnail URL is stored under "squarethumbnailurl"
      String thumbnailUrl = imgInfo.getString("squarethumbnailurl");
      println(title + ' ' + '(' + thumbnailUrl + ')');

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

  public static void main( String... args )
  {
    new SpeechChromasthetiatorSketch().runSketch(args);
  }
}
