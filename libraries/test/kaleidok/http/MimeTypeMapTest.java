package kaleidok.http;

import kaleidok.http.util.MimeTypeMap;
import kaleidok.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static kaleidok.http.util.MimeTypeMap.WILDCARD;
import static org.junit.Assert.*;


@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public class MimeTypeMapTest
{
  MimeTypeMap m;


  @Before
  public void setUp()
  {
    m = new MimeTypeMap() {{
      put(WILDCARD, .5f);
      put("text/*", null);
      put("text/plain", ZERO);
    }};
  }


  @Test
  public void testAllows()
  {
    assertEquals(WILDCARD, m.allows("application/octetstream"));
    assertEquals("text/*", m.allows("text/html"));
    assertNull(m.allows("text/plain"));
  }


  @Test
  public void testToString()
  {
    Collection<String> strAccept =
      Arrays.asImmutableList(m.toString().split("\\s*,\\s*"));
    assertEquals(m.size(), strAccept.size());
    assertContains(strAccept, WILDCARD + ";q=0.5");
    assertContains(strAccept, "text/*");
    assertContains(strAccept, "text/plain;q=0");
  }


  private static void assertContains( Collection<String> haystack, String needle )
  {
    if (!haystack.contains(needle))
      fail(haystack + " doesn't contain \"" + needle + '\"');
  }
}
