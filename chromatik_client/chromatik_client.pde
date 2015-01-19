import java.net.URLEncoder;
import http.requests.*;

void setup() {
  size(800, 200);
  
  String exampleRequestString =
    buildChromatikQuery(0, 10, null, new int[]{#0000ff, #00ff00});
  //deprecatedWay(exampleRequestString);
  preferredWay(exampleRequestString);
  
  noLoop();
}

String buildChromatikQuery(int start, int nhits, String keywords, int[] colors)
{
  String q = (keywords != null && !keywords.isEmpty()) ? keywords + ' ' : "";
  
  if (colors != null && colors.length != 0) {
    q += '(';
    int weight = (int)(100.0 / colors.length);
    for (int c: colors) {
      c &= 0x00FFFFFF;
      q += "OPT color:O/" + Integer.toHexString(c) + '/' + weight + ' ';
    }
    q += ')';
  }
  
  try {
    return
      "http://chromatik.labs.exalead.com/searchphotos" +
      "?start=" + start +
      "&nhits=" + nhits +
      "&q=" + URLEncoder.encode(q, "UTF-8");
  } catch (java.io.UnsupportedEncodingException ex) {
    throw new Error(ex);
  }
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
    String title = imgInfo.getString("title");
    
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
