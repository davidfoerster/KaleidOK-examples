import chromatik.Color;
import chromatik.Query;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.Map;


public class ChromatikClientSketch extends PApplet
{

  public void setup()
  {
    size(800, 200);

    Query q = new Query();
    q.nhits = 10;
    q.keywords = "";
    q.opts.put(new Color(color(192, 0, 0)), 0.20f);
    q.opts.put(new Color(color(128, 0, 128)), 0.15f);
    println("query: " + q.getQueryString());

    JSONArray resultSet = q.getResult();
    if (q != null) {
      exit();
      return;
    }
    drawResultSet(resultSet);

    noLoop();
  }

  private void drawQuery( Query q )
  {
    fill(0);
    text("keywords: " + q.keywords, 10, 20);

    int x = 10;
    stroke(0);
    noFill();
    rect(x, 25, 200, 25);

    for (Map.Entry<Object, Object> o: q.opts.entrySet()) {
      if (o.getKey() instanceof Color) {
        Color c = (Color) o.getKey();

        fill(c.value);
        int width = (int)(((Number) o.getValue()).floatValue() * 200);
        rect(x, 25, width, 25);

        fill((brightness(c.value) >= 128) ? 255 : 0);
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

    int imgXpos = 10;
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
      // draw image
      image(img, imgXpos, 60, 75, 75);

      imgXpos += 75 + 5;
    }
  }

}
