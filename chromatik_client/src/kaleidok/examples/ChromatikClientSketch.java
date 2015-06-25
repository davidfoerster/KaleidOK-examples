package kaleidok.examples;

import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.ChromatikQuery;
import kaleidok.chromatik.data.ChromatikResponse;
import processing.core.PApplet;
import processing.core.PImage;

import java.io.IOException;
import java.util.Map;


/**
 * Demo sketch for the Chromatik query connector
 */
public class ChromatikClientSketch extends PApplet
{

  @SuppressWarnings("ConstantConditions")
  public void setup()
  {
    size(800, 200);

    // Set up query
    ChromatikQuery q = new ChromatikQuery();
    q.nhits = 10;
    q.keywords = "";
    q.opts.put(new ChromatikColor(0xeb5252), 0.25f);
    q.opts.put(new ChromatikColor(0x9feb52), 0.18f);
    println("query: " + q.getUri().getRawQuery());
    drawQuery(q);

    ChromatikResponse resultSet;
    try {
      resultSet = q.getResult();
    } catch (IOException ex) {
      ex.printStackTrace();
      exit();
      return;
    }
    drawResultSet(resultSet);

    noLoop();
  }

  /**
   * Draws a visual representation of a query.
   * @param q A query object
   */
  private void drawQuery( ChromatikQuery q )
  {
    background(255.f);

    // draw keywords
    fill(0.f);
    text("keywords: " + q.keywords, 5, 20);

    // rectangle frame for color composition
    int x = 5;
    stroke(0.f);
    noFill();
    rect(x - 1, 24, 200, 26);
    noStroke();

    for (Map.Entry<Object, Object> o: q.opts.entrySet()) {
      if (o.getKey() instanceof ChromatikColor) {
        ChromatikColor c = (ChromatikColor) o.getKey();

        /*
         * draw a filled rectangle for each color with a width relative to its
         * weight
         */
        fill(c.value | 0xff000000);
        int width = (int)(((Number) o.getValue()).floatValue() * 100) * 2;
        rect(x, 25, width, 25);

        // draw color group name over color rectangle
        fill((brightness(c.value) < 128) ? 255.f : 0.f);
        text(c.groupName, x + 1, 45);

        x += width;
      }
    }
  }

  /**
   * Fetches and draws the images of a query result set.
   * @param response A result object of a {@link ChromatikQuery}
   * @see kaleidok.chromatik.ChromatikQuery#getResult()
   */
  private void drawResultSet( ChromatikResponse response )
  {
    // array index 0 contains number total search hits
    println("Hits: " + response.hits);

    int imgXpos = 5;
    // loop over result set
    for (ChromatikResponse.Result imgInfo: response.results) {
      // the image title is stored with the key "title"
      String title = (imgInfo.title != null) ? imgInfo.title : "<untitled>";

      // the thumbnail URL is stored under "squarethumbnailurl"
      String thumbnailUrl = imgInfo.squarethumbnailurl;
      println(title + '(' + ' ' + thumbnailUrl + ')');

      // download image
      PImage img = loadImage(thumbnailUrl);

      if (img != null && img.width > 0 && img.height > 0) {
        // draw image
        image(img, imgXpos, 60, 75, 75);

        imgXpos += 75 + 5;
      }
    }
  }

  public static void main( String... args )
  {
    new ChromatikClientSketch().runSketch(args);
  }
}
