import synesketch.*;
import synesketch.emotion.*;
import synesketch.art.util.SynesketchPalette;

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
  size(500, 500);

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
  text(frameCount, 5, 20); //counter of times clicked

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

    color c = currentPalette[(int) random(currentPalette.length)];
    fill(red(c), green(c), blue(c));
    text(synState.getText(), 5, 40);
    text(emo.getType() + ", " + emo.getWeight(), 5, 60);
  }
}
