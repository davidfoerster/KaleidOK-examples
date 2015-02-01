import http.requests.*;

void setup() {
  size(800, 200);

  String exampleRequestString =
    buildChromatikQuery(0, 10, null, 0xeb5252, 0x19e519);
  //deprecatedWay(exampleRequestString);
  //preferredWay(exampleRequestString);

  noLoop();
}

String buildChromatikQuery(int start, int nhits, String keywords, int... colors)
{
  String q = (keywords != null) ? keywords : "";

  if (colors != null && colors.length != 0) {
    if (!q.isEmpty()) q += ' ';
    q += '(';
    int weight = min((int)(100.0 / colors.length), 25);
    for (int c: colors) {
      Object[] cci = getChromatikColorInfo(c);
      System.out.printf("%#08x => %#08x\n", c, cci[0]);
      q += String.format(
        "OPT color:%2$s/%1$06x/%3$d{s=200000} colorgroup:%2$s/%3$d ",
        cci[0], cci[1], weight);
    }
    q += ')';
  }

  return
    "http://chromatik.labs.exalead.com/searchphotos" +
    "?start=" + start +
    "&nhits=" + nhits +
    "&q=" + urlEncode(q);
}

Object[] getChromatikColorInfo(int c)
{
  String colorGroupName;
  if (saturation(c) < saturationBins[0] / 2)
  {
    // gray scale:
    // find brightness bin index (white = 0, black = hueNames.length-1)
    int brBin = round(map(brightness(c), 255, 0, 0, hueNames.length - 1));
    // and go back to gray value
    c = c * 255 / (hueNames.length - 1);
    c |= c << 8 | c << 16;
    // find color group name based on rules
    colorGroupName =
      (brBin <= 1) ? "White" :
      (brBin == hueNames.length - 1) ? "Black" :
        "Gray";
  }
  else
  {
    int hueBin =
      round(map(hue(c), 0, 256, 0, hueNames.length)) % hueNames.length;
    colorGroupName = hueNames[hueBin];

    float satBin = findBin(saturation(c), saturationBins, 1);
    float brBin = findBin(brightness(c), brightnessBins, -1);
    int horizontalBin = round((satBin + brBin) / 2);
    c = chromatikColors[horizontalBin * saturationBins.length + hueBin];
  }

  return new Object[]{ Integer.valueOf(c), colorGroupName };
}

String[] hueNames = {
  "Red", "Orange", "Yellow", "Green", "Green", "Green",
  "Cyan", "Blue", "Blue", "Purple", "Pink", "Pink"
};

float[] saturationBins = { 52, 107, 166, 227, 227, 228, 228 };
float[] brightnessBins = { 248, 242, 235, 229, 172, 114, 57 };

int[] chromatikColors = {
  0xf8c5c5, 0xf8dfc5, 0xf8f8c5, 0xdff8c5, 0xc5f8c5, 0xc5f8df, 0xc5f8f8, 0xc5dff8, 0xc5c5f8, 0xdfc5f8, 0xf8c5f8, 0xf8c5df,
  0xf28c8c, 0xf2bf8c, 0xf2f28c, 0xbff28c, 0x8cf28c, 0x8cf2bf, 0x8cf2f2, 0x8cbff2, 0x8c8cf2, 0xbf8cf2, 0xf28cf2, 0xf28cbf,
  0xeb5252, 0xeb9f52, 0xebeb52, 0x9feb52, 0x52eb52, 0x52eb9f, 0x52ebeb, 0x529feb, 0x5252eb, 0x9f52eb, 0xeb52eb, 0xeb529f,
  0xe51919, 0xe57f19, 0xe5e519, 0x7fe519, 0x19e519, 0x19e57f, 0x19e5e5, 0x197fe5, 0x1919e5, 0x7f19e5, 0xe519e5, 0xe5197f,
  0xac1313, 0xac5f13, 0xacac13, 0x5fac13, 0x13ac13, 0x13ac5f, 0x13acac, 0x135fac, 0x1313ac, 0x5f13ac, 0xac13ac, 0xac135f,
  0x720c0c, 0x723f0c, 0x72720c, 0x3f720c, 0x0c720c, 0x0c723f, 0x0c7272, 0x0c3f72, 0x0c0c72, 0x3f0c72, 0x720c72, 0x720c3f,
  0x390606, 0x391f06, 0x393906, 0x1f3906, 0x063906, 0x06391f, 0x063939, 0x061f39, 0x060639, 0x1f0639, 0x390639, 0x39061f
};

int mapBin(float x, float start, float end, int nbins)
{
  x = map(x, start, end, 0, nbins - 1);
  x = map(round(x), 0, nbins - 1, start, end);
  return round(x);
}

float findBin(float x, float[] bins, int direction)
{
  assert direction != 0;
  int i = (direction > 0) ? 0 : bins.length + direction;

  float d, dprev = abs(x - bins[i]);
  for (i += direction; i >= 0 && i < bins.length; i += direction)
  {
    assert bins[i - direction] <= bins[i];
    d = abs(x - bins[i]);
    if (d >= dprev)
    {
      assert bins[i - direction] <= x && x <= bins[i];
      return (bins[i - direction] < bins[i]) ?
        map(x, bins[i - direction], bins[i], i - direction, i) :
        i - direction;
    }
    dprev = d;
  }
  return i - direction;
}

void preferredWay(String url) {
  println(url);

  // Load and parse response object from URL
  JSONArray a = loadJSONArray(url);

  // get number of elements in array
  int length = a.size();

  // array index 0 contains number total search hits
  println("Hits: " + a.getInt(0));

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
    // download image
    int imgXpos = (i - 1) * (75 + 5);
    // draw image
    image(img, imgXpos + 10, 10, 75, 75);
  }
}

void deprecatedWay(String url) {
  GetRequest get = new GetRequest(url);
  get.send();
  println("Reponse Content: " + get.getContent());
  println("Reponse Content-Length Header: " + get.getHeader("Content-Length"));
}
