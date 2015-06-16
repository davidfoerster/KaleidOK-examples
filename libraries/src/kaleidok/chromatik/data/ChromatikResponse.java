package kaleidok.chromatik.data;

import com.google.gson.annotations.Expose;


public class ChromatikResponse
{
  public int hits;

  public Result[] results;

  public static class Result
  {
    @Expose
    public int ind;

    @Expose
    public String id, title;

    @Expose
    public String[] tags;

    @Expose
    public int width, height;

    @Expose
    public String thumbnailurl, squarethumbnailurl;

    public FlickrPhoto flickrPhoto;
  }
}
