import synesketch.*;
import synesketch.emotion.*;
import synesketch.art.util.SynesketchPalette;
import java.net.URLEncoder;

Synesthetiator synesthiator;
SynesketchPalette palettes; //colors taken from xml files in synesketch library
EmotionalState synState = null; //no emotional state

String emoText = "I'm so happy this finally works."; //text that is analysed
int[] bwPalette = { -10461088, -7303024, -6579301, -10987432, -7368817, //neutral colors
      -9868951,
      -5921371, -10526881, -8421505, -8224126, -6381922, -8224126, -8816263,
      -10724260, -11645362, -9934744, -5658199, -8947849, -5395027, -6579301,
      -9868951, -6842473, -11053225, -9276814, -6645094, -8816263, -6710887,
      -5921371, -10987432, -8092540, -7039852, -7697782, -5789785, -8750470,
      -10197916, -6381922, -8750470, -5855578 };

void setup() {
  size(900, 200);

  try {
    synesthiator = new SynesthetiatorEmotion(this); //initialises synesthetiator for current sketch
    palettes = new SynesketchPalette("standard"); //colors initialised
  } catch (Exception ex) {
    ex.printStackTrace(); //shows content of current stack (function currently in, and called previosuly)
    exit();
  }

  noLoop(); //stops setup looping
}

//  if this code throws an exception when mouse cliked, then the code in "catch" is run
void mouseClicked() {
  try {
    synesthiator.synesthetise(emoText);
  } catch (Exception ex) {
    ex.printStackTrace();
  }
}

/**
 * This is required by the synesthetiator, and called everytime "synesthetised()" is called
 *
 * @param state - result of emotion analysis
 */
void synesketchUpdate(SynesketchState state) {
  synState = (EmotionalState) state;
  redraw();
}

void draw() { //redraw background
  background(255);
  fill(0);
  text(frameCount, 5, 100); //counter of times clicked

  if (synState != null) { //does something
    Emotion emo = synState.getStrongestEmotion();
    int[] currentPalette;
    float sat;

    if (emo.getType() != Emotion.NEUTRAL) {
      currentPalette = palettes.getColors(emo);
      sat = sqrt((float) emo.getWeight());
    } else {
      currentPalette = bwPalette;
      sat = 0.5f;
    }

    // TODO: Shorter
    int[] colors = new int[]{ 0xe51919 }; //java.util.Arrays.copyOf(currentPalette, 1);
    drawChromatikImages(buildChromatikQuery(0, 10, null, colors));

    text(synState.getText(), 5, 120);
    text(emo.getType() + ", " + emo.getWeight(), 5, 140);

    for (int i = 0; i < colors.length; i++) {
      fill(colors[i] | 0xFF000000);
      rect(10 + i * 15, 160, 10, 10);
    }
  }
}

String buildChromatikQuery(int start, int nhits, String keywords, int[] colors)
{
  String q = (keywords != null && !keywords.isEmpty()) ? keywords + ' ' : "";

  if (colors != null && colors.length != 0) {
    q += '(';
    int weight = (int)(100.0 / colors.length);
    for (int c: colors) {
      c &= 0x00FFFFFF;
      q += "OPT color:Red/" + Integer.toHexString(c) + '/' + weight + "{s=200000}";
      //q += " colorgroup:Blue/" + weight;
    }
    q += ')';
  }

  try {
    return
      "http://chromatik.labs.exalead.com/searchphotos" +
      "?start=" + start +
      "&nhits=" + nhits +
      "&q=" + URLEncoder.encode(q, "UTF-8");
  } catch (java.io.UnsupportedEncodingException ex) {
    throw new Error(ex);
  }
}

void drawChromatikImages(String url) {
  println(url);

  // Load and parse response object from URL
  JSONArray a = loadJSONArray(url);

  // get number of elements in array
  int length = a.size();

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
    println(title + " (" + thumbnailUrl + ')');

    // download image
    // TODO: React to images that can't be loaded
    PImage img = loadImage(thumbnailUrl);
    // download image
    int imgXpos = (i - 1) * (75 + 5);
    // draw image
    image(img, imgXpos + 10, 10, 75, 75);
  }
}
