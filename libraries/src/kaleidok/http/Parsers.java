package kaleidok.http;

import kaleidok.util.StringPartsIterator;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


public final class Parsers
{
  private Parsers() { }


  public static Map<String, String> getQueryMap( URI uri, Charset chs )
  {
    String q = uri.getRawQuery();
    if (q == null)
      return null;

    Map<String, String> qm = new HashMap<>(8);
    try {
      for (String strParam : new StringPartsIterator(q, '&')) {
        if (!strParam.isEmpty()) {
          StringPartsIterator itParam = new StringPartsIterator(strParam, '=');
          String name = URLDecoder.decode(itParam.next(), chs.name()),
            value = itParam.hasNext() ?
              URLDecoder.decode(strParam.substring(itParam.getBegin()), chs.name()) :
              null;
          if (qm.containsKey(name))
            throw new IllegalArgumentException("Query parameter appears multiple times: " + name);
          qm.put(name, value);
        }
      }
    } catch (UnsupportedEncodingException ex) {
      throw new Error("Provided Charset has the name of an unknown encoding", ex);
    }
    return qm;
  }

  public static Map<String, String> getHeaderValueMap( String h )
  {
    Map<String, String> hm = new HashMap<>(4);
    for (String strParam : h.split(";\\s*", 0)) {
      if (!strParam.isEmpty()) {
        StringPartsIterator itParam = new StringPartsIterator(strParam, '=');
        String name = itParam.next(),
          value = itParam.hasNext() ? strParam.substring(itParam.getBegin()) : null;
        if (value == null) {
          value = name;
          name = null;
        }
        if (hm.containsKey(name))
          throw new IllegalArgumentException("Header parameter appears multiple times: " + name);
        hm.put(name, value);
      }
    }
    return hm;
  }
}
