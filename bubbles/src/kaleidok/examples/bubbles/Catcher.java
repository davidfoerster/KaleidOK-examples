package kaleidok.examples.bubbles;

import processing.core.PApplet;


/**
 * Represents the bar at the canvas bottom enabling the player to catch
 * colliding bubbles.
 *
 * The player steers the catcher bar by moving the mouse pointer. Therefore
 * the catcher position depends on the current mouse pointer positions.
 */
public class Catcher
{
  private PApplet a;
  private int y, w, h;

  public Catcher(PApplet a, int y, int w, int h ) {
    this.a = a;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /**
   * Draws the catcher centered around the current horizontal position of the
   * mouse pointer.
   */
  public void draw() {
    a.g.fill(0);
    a.g.rect(getHorizontalPos(), y, w, h);
  }

  /**
   * Checks for a collision of this catcher with a given bubble.
   *
   * @param b A bubble
   * @return true on collision; false otherwise
   */
  public boolean collides(Bubble b) {
    return b.collidesRect(getHorizontalPos(), y, w, h);
  }

  private int getHorizontalPos() {
    return a.mouseX - w / 2;
  }
}
