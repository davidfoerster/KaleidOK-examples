package kaleidok.chromatik;

import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.http.URLEncoding;
import kaleidok.util.StringTokenIterator;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;


public class ChromatikQueryTest
{

  private Map<String, String> getParams( ChromatikQuery q )
  {
    String qs = q.getUri().getRawQuery();
    HashMap<String, String> m = new HashMap<>();
    for (String p: new StringTokenIterator(qs, '&')) {
      int split = p.indexOf('=');
      String key, value;
      if (split >= 0) {
        key = URLEncoding.decode(p.substring(0, split));
        value = URLEncoding.decode(p.substring(split + 1));
      } else {
        key = URLEncoding.decode(p);
        value = null;
      }

      assertFalse(m.containsKey(key));
      m.put(key, value);
    }

    return m;
  }


  private List<String> getOpts( Map<String, String> m )
  {
    String q = m.get("q");
    if (q == null)
      return null;

    int split = q.indexOf('(');
    List<String> opts;
    if (split >= 0) {
      assertEquals(')', q.charAt(q.length() - 1));
      String[] a = q
        .substring(split + 1, q.length() - 1)
        .split(" +");
      int i = 0, j = 0;
      while (j < a.length) {
        if (a[j].equals("OPT")) {
          j++;
          assertTrue(j < a.length);
          assertTrue(a[j].startsWith("color:"));
        }
        a[i++] = a[j++];
      }
      a = Arrays.copyOf(a, i);

      opts = new ArrayList<>(a.length + 1);
      if (split > 0) {
        assertEquals(' ', q.charAt(split - 1));
        opts.add(q.substring(0, split - 1));
      } else {
        opts.add(null);
      }
      Arrays.sort(a);
      opts.addAll(Arrays.asList(a));
    } else {
      opts = new ArrayList<>(1);
      opts.add(q);
    }
    return opts;
  }


  @Test
  public void testGetQueryString1() throws Exception
  {
    ChromatikQuery q = new ChromatikQuery();
    q.start = 42;
    Map<String, String> p = getParams(q);

    assertEquals("42", p.get("start"));
    assertEquals(Integer.toString(ChromatikQuery.QUERY_NHITS_DEFAULT), p.get("nhits"));

    assertEquals(2, p.size());
  }


  @Test
  public void testGetQueryString2() throws Exception
  {
    String keywords = "foo bar";
    Map<String, String> p = getParams(new ChromatikQuery(10, keywords, null));

    assertEquals("0", p.get("start"));
    assertEquals("10", p.get("nhits"));
    assertEquals(keywords, p.get("q"));

    assertEquals(3, p.size());
  }


  @Test
  public void testGetQueryString3() throws Exception
  {
    Map<String, String> p = getParams(new ChromatikQuery(10, null, 0xe51919));

    assertEquals("0", p.get("start"));
    assertEquals("10", p.get("nhits"));
    assertEquals("(OPT color:Red/e51919/25{s=200000} colorgroup:Red/25)", p.get("q"));

    assertEquals(3, p.size());
  }


  @Test
  public void testGetQueryString4() throws Exception
  {
    String keywords = "foo bar";
    Map<String, String> p = getParams(new ChromatikQuery(10, keywords, 0xe51919));

    assertEquals("0", p.get("start"));
    assertEquals("10", p.get("nhits"));
    assertEquals(keywords + " (OPT color:Red/e51919/25{s=200000} colorgroup:Red/25)", p.get("q"));

    assertEquals(3, p.size());
  }


  @Test
  public void testGetQueryString5() throws Exception
  {
    ChromatikQuery q = new ChromatikQuery(10, null, null) {{
        opts.put(new ChromatikColor(0xe51919), 0.07);
        opts.put(new ChromatikColor(0xebeb52), 0.23);
        opts.put(new ChromatikColor(0x1313ac), 0.42);
      }};
    Map<String, String> p = getParams(q);

    assertEquals("0", p.get("start"));
    assertEquals("10", p.get("nhits"));

    List<String> opts = getOpts(p);
    assertNotNull(opts);
    assertNull(opts.get(0));
    assertEquals("color:Blue/1313ac/42{s=200000}", opts.get(1));
    assertEquals("color:Red/e51919/7{s=200000}", opts.get(2));
    assertEquals("color:Yellow/ebeb52/23{s=200000}", opts.get(3));
    assertEquals("colorgroup:Blue/42", opts.get(4));
    assertEquals("colorgroup:Red/7", opts.get(5));
    assertEquals("colorgroup:Yellow/23", opts.get(6));

    assertEquals(3, p.size());
  }


  @Test
  public void testGetQueryString6() throws Exception
  {
    ChromatikQuery q = new ChromatikQuery(10, null, null) {{
        opts.put(new ChromatikColor(0xe51919), 0.07);
        opts.put(new ChromatikColor(0xac1313), 0.23);
      }};
    Map<String, String> p = getParams(q);

    assertEquals("0", p.get("start"));
    assertEquals("10", p.get("nhits"));

    List<String> opts = getOpts(p);
    assertNotNull(opts);
    assertNull(opts.get(0));
    assertEquals("color:Red/ac1313/23{s=200000}", opts.get(1));
    assertEquals("color:Red/e51919/7{s=200000}", opts.get(2));
    assertEquals("colorgroup:Red/30", opts.get(3));

    assertEquals(3, p.size());
  }

}
