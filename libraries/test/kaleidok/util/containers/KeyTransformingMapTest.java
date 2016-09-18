package kaleidok.util.containers;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.*;


public class KeyTransformingMapTest
{
  private Map<String, String> underlying;

  private KeyTransformingMap<String, String> map;


  private <U> void assertOverlapStatus( final Map<String, U> m,
    boolean assertionValue )
  {
    Collection<Pair<Entry<String, U>, Entry<String, U>>> overlappingEntries =
      map.findKeyConflicts(m);

    if (assertionValue)
    {
      assertNotEquals(0, overlappingEntries.size());
    }
    else
    {
      assertEquals(Collections.emptyList(), overlappingEntries);
    }
  }


  @Before
  public void setUp()
  {
    Map<String, String> m = new HashMap<>();
    m.put("a", "foo");
    m.put("b", "bar");

    underlying = m;
    map = new LowercaseStringMap<>(m, Locale.ROOT);
    assertTrue(map.checkUnderlyingMapIntegrity(m));
  }


  @Test
  public void testHasKeyConflict1()
  {
    assertFalse(map.hasKeyConflict(underlying));
  }


  @Test
  public void containsKey()
  {
    assertTrue(map.containsKey("a"));
    assertTrue(map.containsKey("b"));
    assertTrue(map.containsKey("A"));
    assertTrue(map.containsKey("B"));
  }


  @Test
  public void get()
  {
    assertSame(map.get("a"), map.get("A"));
    assertSame(map.get("b"), map.get("B"));
  }


  @Test
  public void put1()
  {
    String v = "baz";
    map.put("c", v);

    assertTrue(map.containsKey("c"));
    assertTrue(map.containsKey("C"));
    assertSame(v, underlying.get("c"));
    assertFalse(underlying.containsKey("C"));
  }


  @Test
  public void put2()
  {
    String v = "baz";
    map.put("C", v);

    assertTrue(map.containsKey("c"));
    assertTrue(map.containsKey("C"));
    assertSame(v, underlying.get("c"));
    assertFalse(underlying.containsKey("C"));
  }


  @Test
  public void put3()
  {
    String v = "baz";
    map.put("b", v);

    assertTrue(map.containsKey("b"));
    assertTrue(map.containsKey("B"));
    assertSame(v, underlying.get("b"));
    assertFalse(underlying.containsKey("B"));
  }


  @Test
  public void put4()
  {
    String v = "baz";
    map.put("B", v);

    assertTrue(map.containsKey("b"));
    assertTrue(map.containsKey("B"));
    assertSame(v, underlying.get("b"));
    assertFalse(underlying.containsKey("B"));
  }


  @Test
  public void remove1()
  {
    map.remove("a");
    assertFalse(map.containsKey("a"));
    assertFalse(map.containsKey("A"));
  }


  @Test
  public void remove2()
  {
    map.remove("A");
    assertFalse(map.containsKey("a"));
    assertFalse(map.containsKey("A"));
  }


  @Test
  public void putAll1()
  {
    String c = "baz", d = "foobar";
    Map<String, String> o = new HashMap<>();
    o.put("c", c);
    o.put("D", d);
    assertOverlapStatus(o, false);

    map.putAll(o);

    assertSame(c, map.get("c"));
    assertSame(c, map.get("C"));
    assertSame(d, map.get("d"));
    assertSame(d, map.get("D"));
    assertEquals(4, map.size());

    assertSame(c, underlying.get("c"));
    assertFalse(underlying.containsKey("C"));
    assertSame(d, underlying.get("d"));
    assertFalse(underlying.containsKey("D"));
  }


  @Test
  public void putAll2()
  {
    String v = "baz";
    Map<String, String> o = new HashMap<>();
    o.put("a", v);
    assertOverlapStatus(o, false);

    map.putAll(o);

    assertSame(v, map.get("a"));
    assertSame(v, map.get("A"));
    assertEquals(2, map.size());

    assertSame(v, underlying.get("a"));
    assertFalse(underlying.containsKey("A"));
  }


  @Test
  public void putAll3()
  {
    String v = "baz";
    Map<String, String> o = new HashMap<>();
    o.put("A", v);
    assertOverlapStatus(o, false);

    map.putAll(o);

    assertSame(v, map.get("a"));
    assertSame(v, map.get("A"));
    assertEquals(2, map.size());

    assertSame(v, underlying.get("a"));
    assertFalse(underlying.containsKey("A"));
  }


  @Test
  public void putAll4()
  {
    String v = "baz";
    Map<String, String> o = new HashMap<>();
    o.put("c", v);
    o.put("C", v);
    assertOverlapStatus(o, false);

    map.putAll(o);

    assertSame(v, map.get("c"));
    assertSame(v, map.get("C"));
    assertEquals(3, map.size());

    assertSame(v, underlying.get("c"));
    assertFalse(underlying.containsKey("C"));
  }


  @Test(expected = IllegalArgumentException.class)
  public void putAll5()
  {
    Map<String, String> o = new HashMap<>();
    o.put("c", "x");
    o.put("C", "y");
    assertOverlapStatus(o, true);

    map.putAll(o);
  }
}
