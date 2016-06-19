package kaleidok.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class StringTokenIterator implements Iterable<String>, Iterator<String>
{
  public final String source;

  public final char delimiter;

  private int begin = -1, end = -1;

  public StringTokenIterator( String source, char delimiter )
  {
    this.source = source;
    this.delimiter = delimiter;
  }

  @Override
  public boolean hasNext()
  {
    if (begin >= 0)
      return true;
    if (this.end >= source.length())
      return false;

    begin = this.end + 1;
    int end = source.indexOf(delimiter, begin);
    this.end = (end >= 0) ? end : source.length();
    return true;
  }

  public int getBegin()
  {
    if (!hasNext())
      throw new NoSuchElementException();
    return begin;
  }

  public int getEnd()
  {
    if (!hasNext())
      throw new NoSuchElementException();
    return end;
  }

  @Override
  public String next()
  {
    if (!hasNext())
      throw new NoSuchElementException();

    String s = (begin != end) ? source.substring(begin, end) : "";
    begin = -1;
    return s;
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<String> iterator()
  {
    return this;
  }
}
