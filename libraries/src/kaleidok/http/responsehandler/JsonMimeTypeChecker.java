package kaleidok.http.responsehandler;

import kaleidok.http.MimeTypeMap;


public class JsonMimeTypeChecker extends ResponseMimeTypeChecker
{
  public static final JsonMimeTypeChecker INSTANCE = new JsonMimeTypeChecker();

  public static final MimeTypeMap MIME_TYPE_MAP = new MimeTypeMap() {{
    put("application/json", ONE);
    put("text/json", 0.9f);
    put("text/javascript", 0.5f);
    put(WILDCARD, 0.1f);
    freeze();
  }};

  protected JsonMimeTypeChecker()
  {
    super(MIME_TYPE_MAP);
  }
}
