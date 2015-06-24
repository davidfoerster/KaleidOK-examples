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
    String qs = q.getQueryString();
    int qp = qs.indexOf('?');
    assertTrue(qp >= 0);

    HashMap<String, String> m = new HashMap<>();
    for (String p: new StringTokenIterator(qs.substring(qp + 1), '&')) {
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

    String splitter = "(OPT ";
    int split = q.indexOf(splitter);
    List<String> opts;
    if (split >= 0) {
      assertEquals(')', q.charAt(q.length() - 1));
      String[] a = q
        .substring(split + splitter.length(), q.length() - 1)
        .split(" +");
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
    ChromatikQuery q = new ChromatikQuery(10, null, null);
    q.opts.put(new ChromatikColor(0xe51919), 0.07f);
    q.opts.put(new ChromatikColor(0xebeb52), 0.23f);
    q.opts.put(new ChromatikColor(0x1313ac), 0.42f);
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
}
