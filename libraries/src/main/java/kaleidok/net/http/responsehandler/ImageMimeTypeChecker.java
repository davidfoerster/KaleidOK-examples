package kaleidok.net.http.responsehandler;

import kaleidok.net.http.util.MimeTypeMap;

import javax.imageio.ImageIO;


public class ImageMimeTypeChecker extends ResponseMimeTypeChecker
{
  public static final MimeTypeMap IMAGE_MIMETYPE_MAP;
  static {
    final String[] imageMimeTypes = ImageIO.getReaderMIMETypes();
    MimeTypeMap mtm = IMAGE_MIMETYPE_MAP =
      new MimeTypeMap(imageMimeTypes.length * 3 / 2);
    for (String mimeType: imageMimeTypes)
      mtm.put(mimeType, null);
    mtm.freeze();
  }

  public static final ImageMimeTypeChecker INSTANCE =
    new ImageMimeTypeChecker();

  protected ImageMimeTypeChecker()
  {
    super(IMAGE_MIMETYPE_MAP);
  }
}
