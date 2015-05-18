package kaleidok.util.http;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class MimeTypeMapTest
{
  MimeTypeMap m;

  @Before
  public void setUp() throws Exception
  {
    m = new MimeTypeMap() {{
      put(MimeTypeMap.WILDCARD, .5f);
      put("text/*", null);
      put("text/plain", 0.f);
    }};
  }

  @Test
  public void testAllows() throws Exception
  {
    assertEquals(MimeTypeMap.WILDCARD, m.allows("application/octetstream"));
    assertEquals("text/*", m.allows("text/html"));
    assertNull(m.allows("text/plain"));
  }

  @Test
  public void testToString() throws Exception
  {
    List<String> strAccept = Arrays.asList(m.toString().split("\\s*,\\s*"));
    assertEquals(m.size(), strAccept.size());
    assertTrue(strAccept.contains(MimeTypeMap.WILDCARD + ";q=0.5"));
    assertTrue(strAccept.contains("text/*"));
    assertTrue(strAccept.contains("text/plain;q=0"));
  }
}
