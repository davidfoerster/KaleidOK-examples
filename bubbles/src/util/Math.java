package util;

import processing.core.PApplet;

public final class Math
{
  private Math() {}

  public static int square(int x) {
    return x * x;
  }

  public static float square(float x) {
    return x * x;
  }

  public static float clamp(float x, float min, float max) {
    return PApplet.min(PApplet.max(x, min), max);
  }

  public static boolean between(int x, int min, int max) {
    assert min <= max;
    return min <= x && x <= max;
  }
}
