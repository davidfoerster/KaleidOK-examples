package kaleidok.net.http.responsehandler;

import kaleidok.net.http.util.MimeTypeMap;


public class JsonMimeTypeChecker extends ResponseMimeTypeChecker
{
  public static final JsonMimeTypeChecker INSTANCE = new JsonMimeTypeChecker();

  public static final MimeTypeMap MIME_TYPE_MAP;

  static
  {
    MimeTypeMap m = MIME_TYPE_MAP = new MimeTypeMap(8);
    m.put("application/json", MimeTypeMap.ONE);
    m.put("text/json", 0.9f);
    m.put("text/javascript", 0.5f);
    m.put(MimeTypeMap.WILDCARD, 0.1f);
    m.freeze();
  }

  protected JsonMimeTypeChecker()
  {
    super(MIME_TYPE_MAP);
  }
}
