package kaleidok.http;

import kaleidok.util.StringTokenIterator;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static kaleidok.http.URLEncoding.decode;


public final class Parsers
{
  private Parsers() { }


  public static Map<String, String> getQueryMap( URI uri, Charset chs )
  {
    String q = uri.getRawQuery();
    if (q == null)
      return null;

    Map<String, String> qm = new HashMap<>(8);
    for (String strParam : new StringTokenIterator(q, '&')) {
      if (!strParam.isEmpty()) {
        StringTokenIterator itParam = new StringTokenIterator(strParam, '=');
        String name = decode(itParam.next(), chs),
          value = itParam.hasNext() ?
            decode(strParam.substring(itParam.getBegin()), chs) :
            null;
        if (qm.containsKey(name))
          throw new IllegalArgumentException("Query parameter appears multiple times: " + name);
        qm.put(name, value);
      }
    }
    return qm;
  }

  public static Map<String, String> getHeaderValueMap( String h )
  {
    Map<String, String> hm = new HashMap<>(4);
    for (String strParam : h.split("\\s*;\\s*", 0)) {
      if (!strParam.isEmpty()) {
        StringTokenIterator itParam = new StringTokenIterator(strParam, '=');
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
