import processing.core.PApplet;


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
  public boolean collides(int x, int y, int width, int height) {
    /*
     * Currently: Simple collision detection with the bounding box of the rectangle.
     * TODO: Calculate collision with the actual circle instead.
     */
    int r = diameter / 2;
    return (x < this.x + r) && (x + width > this.x - r) &&
      (y < this.y + r) && (y + height > this.y - r);
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
