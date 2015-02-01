import chromatik.ChromatikColor;
import chromatik.ChromatikQuery;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.Map;


public class ChromatikClientSketch extends PApplet
{

  @SuppressWarnings("ConstantConditions")
  public void setup()
  {
    size(800, 200);

    ChromatikQuery q = new ChromatikQuery();
    q.nhits = 10;
    q.keywords = "";
    q.opts.put(new ChromatikColor(0xeb5252), 0.25f);
    q.opts.put(new ChromatikColor(0x9feb52), 0.18f);
    println("query: " + q.getQueryString());
    drawQuery(q);

    JSONArray resultSet = q.getResult();
    if (q == null) {
      exit();
      return;
    }
    drawResultSet(resultSet);

    noLoop();
  }

  private void drawQuery( ChromatikQuery q )
  {
    background(255.f);

    fill(0.f);
    text("keywords: " + q.keywords, 5, 20);

    int x = 5;
    stroke(0.f);
    noFill();
    rect(x - 1, 24, 200, 26);
    noStroke();

    for (Map.Entry<Object, Object> o: q.opts.entrySet()) {
      if (o.getKey() instanceof ChromatikColor) {
        ChromatikColor c = (ChromatikColor) o.getKey();

        fill(c.value | 0xff000000);
        int width = (int)(((Number) o.getValue()).floatValue() * 100) * 2;
        rect(x, 25, width, 25);

        fill((brightness(c.value) < 128) ? 255.f : 0.f);
        text(c.groupName, x + 1, 45);

        x += width;
      }
    }
  }

  private void drawResultSet( JSONArray a )
  {
    // get number of elements in array
    int length = a.size();

    // array index 0 contains number total search hits
    println("Hits: " + a.getInt(0));

    int imgXpos = 5;
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
      PImage img = loadImage(thumbnailUrl);

      if (img != null && img.width > 0 && img.height > 0) {
        // draw image
        image(img, imgXpos, 60, 75, 75);

        imgXpos += 75 + 5;
      }
    }
  }

}
