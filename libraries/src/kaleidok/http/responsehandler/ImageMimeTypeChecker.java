package kaleidok.http.responsehandler;

import kaleidok.http.MimeTypeMap;

import javax.imageio.ImageIO;


public class ImageMimeTypeChecker extends ResponseMimeTypeChecker
{
  public static final MimeTypeMap IMAGE_MIMETYPE_MAP;
  static {
    final String[] imageMimeTypes = ImageIO.getReaderMIMETypes();
    IMAGE_MIMETYPE_MAP =
      new MimeTypeMap(imageMimeTypes.length + (imageMimeTypes.length + 1) / 2);
    for (String mimeType: imageMimeTypes)
      IMAGE_MIMETYPE_MAP.put(mimeType, null);
    IMAGE_MIMETYPE_MAP.freeze();
  }

  public static final ImageMimeTypeChecker INSTANCE =
    new ImageMimeTypeChecker();

  protected ImageMimeTypeChecker()
  {
    super(IMAGE_MIMETYPE_MAP);
  }
}
