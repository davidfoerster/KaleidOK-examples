package kaleidok.util;

import java.util.Collection;
import java.util.Iterator;


public final class Strings
{
  private Strings() { }

  public static String join( Collection<? extends CharSequence> ar, char separator )
  {
    if (ar.isEmpty())
      return "";

    Iterator<? extends CharSequence> it = ar.iterator();
    if (ar.size() == 1)
      return it.next().toString();

    int len = ar.size() - 1;
    while (it.hasNext())
      len += it.next().length();
    StringBuilder sb = new StringBuilder(len);

    it = ar.iterator();
    sb.append(it.next());
    while (it.hasNext())
      sb.append(separator).append(it.next());

    return sb.toString();
  }
}
