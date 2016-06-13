package kaleidok.containers;

public interface FloatList
{
  int size();

  float get( int i );

  float[] get( float[] a, int offset, int first, int length );

  default float[] get( float[] a )
  {
    int size = this.size();
    if (a == null)
      a = new float[size];
    return get(a, 0, 0, size);
  }
}
