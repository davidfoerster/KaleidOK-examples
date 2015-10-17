package kaleidok.exaleads.chromatik.data;

import static org.junit.Assert.*;


public class ChromatikColorTest
{
  @org.junit.Test
  public void testColorPredefined() {
    for (int i = 0; i < ChromatikColor.COLORS.length; i++) {
      int c1 = ChromatikColor.COLORS[i];
      ChromatikColor c2 = new ChromatikColor(c1);
      assertEquals(c1, c2.value);
      assertEquals(ChromatikColor.HUE_NAMES[i % ChromatikColor.HUE_COLS], c2.groupName);
    }
  }

  @org.junit.Test
  public void testColorGrayscale() {
    for (int i = 1; i <= ChromatikColor.HUE_COLS; i++) {
      int brightness = i * 255 / 12;
      ChromatikColor gray = new ChromatikColor(brightness, brightness, brightness);
      assertEquals(gray.value & 0xff, (gray.value >>> 8) & 0xff);
      assertEquals(gray.value & 0xff, (gray.value >>> 16));
      assertEquals(brightness, gray.value & 0xff);

      if (brightness <= 0x1f) {
        assertEquals("Black", gray.groupName);
      } else if (brightness > 0xde) {
        assertEquals("White", gray.groupName);
      } else {
        assertEquals("Gray", gray.groupName);
      }
    }
  }

  public static void main(String[] args)
  {
    for (int i = 0; i < ChromatikColor.COLORS.length; i++) {
      int c1 = ChromatikColor.COLORS[i];
      ChromatikColor c2 = new ChromatikColor(c1);
      System.out.format("%#08x => %#08x %c (%s)%n", c1, c2.value, (c1 == c2.value) ? ' ' : '!', c2.groupName);

      if ((i + 1) % ChromatikColor.HUE_COLS == 0)
        System.out.println();
    }
  }
}
