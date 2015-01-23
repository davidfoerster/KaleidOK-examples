import processing.core.PApplet;

import static util.Math.between;
import static util.Math.square;
import static util.Math.clamp;


/**
 * Represents a falling bubble.
 */
public class Bubble
{
  public static final int speedMin = 1, speedMax = 10;
  public static final int diameterMin = 20, diameterMax = 100;

  private int x, y, speed, diameter, color;

  public Bubble(int x, int speed, int diameter, int color) {
    this.x = x;
    this.speed = speed;
    this.diameter = diameter;
    this.color = color;
    init();
  }

  /**
   * Construct bubble with random horizontal position, speed, and diameter
   * constrained by the canvas size, speedMin, speedMax, diameterMin, and
   * diameterMax.
   *
   * @param a A Processing applet
   * @param color Bubble color
   */
  public Bubble(PApplet a, int color) {
    diameter = (int) a.random(diameterMin, diameterMax);
    speed = (int) a.random(speedMin, speedMax);
    x = (int) a.random(diameter, a.width - diameter);
    this.color = color;
    init();
  }

  private void init() {
    y = diameter / -2; // Initially position bubble just above the canvas
  }

  public void draw(PApplet a) {
    a.g.fill(color);
    a.g.ellipse(x, y, diameter, diameter);
  }

  /**
   * Move bubble down by "speed" pixels
   */
  public void move() {
    y += speed;
  }

  /**
   * Check for a collision of this bubble with a rectangle.
   *
   * @param x horizontal rectangle position
   * @param y vertical rectangle position
   * @param width rectangle width
   * @param height rectangle height
   * @return true on collision; false otherwise
   */
  public boolean collidesRect( int x, int y, int width, int height ) {
    boolean collides =
      // Is the circle center inside rectangle?
      (between(this.x, x, x + height) && between(this.y, y, y + width)) ||
      // Does any of the rectangle sides intersect the circle?
      collidesLineSegment(x, y, width, 0) ||
      collidesLineSegment(x + width, y, 0, height) ||
      collidesLineSegment(x, y + height, width, 0) ||
      collidesLineSegment(x, y, 0, height);
    //color = collides ? 0xFFFF0000 : 0xFFFFFF00;
    return collides;
  }

  /**
   * Checks whether the circle of the bubble intersects with a given axis-
   * parallel line segment.
   *
   * @param ox horizontal position of the support vector of the line
   * @param oy vertical position of the support vector of the line
   * @param vx horizontal direction and length of the line segment
   * @param vy vertical direction and length of the line segment
   * @return true on intersection; false otherwise
   */
  public boolean collidesLineSegment( int ox, int oy, int vx, int vy ) {
    if (vx == 0 || vy == 0) {
      float r;
      if (vx != 0) {
        r = (float)(this.x - ox) / vx;
      } else if (vy != 0) {
        r = (float)(this.y - oy) / vy;
      } else {
        r = 0;
      }

      r = clamp(r, 0, 1);
      float x = ox + r * vx, y = oy + r * vy;
      return square(this.x - x) + square(this.y - y) <= square(diameter / 2);
    }

    throw new UnsupportedOperationException("The line segment isn't parallel to a coordinate axis");
  }

  /**
   * Check if this bubble has completely left the canvas by the canvas bottom.
   *
   * @param canvasHeight The height of the canvas
   * @return true, if the bubble is outside of the canvas; false otherwise
   */
  public boolean isBelowCanvas( int canvasHeight ) {
    return y + diameter / 2 > canvasHeight;
  }

  /**
   * @return The score points awardable for this bubble
   */
  public float getScore() {
    return speed / (diameter * 0.1f);
  }
}
