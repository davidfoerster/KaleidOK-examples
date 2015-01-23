import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Runs a game of falling bubbles that the player is supposed to catch with a
 * controllable to increase his score points.
 */
public class BubblesGame extends PApplet
{
  public static final float bubblesPerSecond = 4;

  private Catcher catcher = null;
  private Collection<Bubble> bubbles = new ArrayList<Bubble>();
  private int score = 0;

  public void setup() {
    size(600, 600);

    // initialise the catcher bar
    catcher = new Catcher(this, height - 20, 100, 20);

    // spawn some initial bubbles
    spawnBubbles((int) bubblesPerSecond);
  }

  public void draw() {
    // Calculate next state of the game
    step();

    background(255, 255, 255);

    // draw all bubbles
    for (Bubble b: bubbles)
      b.draw(this);

    // draw catcher bar
    catcher.draw();

    // draw point score
    text(score, 10, 10);
  }

  /**
   * Advances the state of the game. This is supposed to be called for every
   * call to draw().
   */
  private void step() {
    Iterator<Bubble> it = bubbles.iterator();
    while (it.hasNext()) { // Iterate over all bubbles until none is left
      // Get current bubble
      Bubble b = it.next();

      // Move current bubble
      b.move();

      // Check collision with catcher
      if (catcher.collides(b)) {
        // Add score of caught bubble to total score
        score += (int)(b.getScore() * frameRate);
        // Remove current bubble
        it.remove();
      }
      // Check if bubble left the canvas ...
      else if (b.isBelowCanvas(height)) {
        // ... if so, remove it
        it.remove();
      }
    }

    /*
     * Spawn a new bubble every few frames so that there will be roughly
     * bubblesPerSecond new bubbles per second.
     */
    if (frameCount % (int)(frameRate / bubblesPerSecond) == 0)
      spawnBubbles(1);
  }

  /**
   * Create n new bubbles and add them to the bubble collection.
   *
   * @param n Amount of bubbles to spawn
   */
  private void spawnBubbles(int n) {
    for (; n > 0; n--)
      bubbles.add(new Bubble(this, 0xFFFFFF00));
  }
}
