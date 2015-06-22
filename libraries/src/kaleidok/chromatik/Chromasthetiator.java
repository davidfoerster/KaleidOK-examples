package kaleidok.chromatik;

import kaleidok.chromatik.data.ChromatikResponse;

import java.io.IOException;


public interface Chromasthetiator
{
  ChromatikResponse query( String text ) throws IOException;
}
