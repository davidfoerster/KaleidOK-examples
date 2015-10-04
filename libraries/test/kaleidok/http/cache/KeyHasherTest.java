package kaleidok.http.cache;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public class KeyHasherTest
{
  private static final String[] EXTERNAL_KEYS = {
    "https://farm1.staticflickr.com:443/53/158856832_bf22c667e6_z.jpg?zz=1",
    "https://farm1.staticflickr.com:443/63/195742611_b9200758a4_z.jpg?zz=1",
    "https://farm1.staticflickr.com:443/55/169230094_65f714eb01_o.jpg",
    "https://farm1.staticflickr.com:443/37/124643511_d9e450ed65_b.jpg",
    "https://farm3.staticflickr.com:443/2070/2076352572_c72df65a1c_o.jpg",
    "https://farm3.staticflickr.com:443/2494/4119612077_8a0991d643_o.jpg",
    "https://farm3.staticflickr.com:443/2204/2761237669_6f1f7ec949_b.jpg"
  };

  private final KeyHasher kh;
  {
    try {
      kh = new KeyHasher(MessageDigest.getInstance("SHA-384"));
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(ex);
    }
  }

  private final Map<String, String> km =
    new HashMap<String, String>(EXTERNAL_KEYS.length * 2) {{
      for (String k: EXTERNAL_KEYS) {
        assert !containsKey(k) : '\"' + k + "\" was already inserted earlier";
        put(k, kh.toInternalKey(k));
      }
    }};


  @Test
  public void testToInternalKey1()
  {
    for (String a: EXTERNAL_KEYS) {
      for (String b: EXTERNAL_KEYS) {
        //noinspection StringEquality
        if (a != b) {
          assertNotEquals("key collision", km.get(a), km.get(b));
        }
      }
    }
  }
}
