package kaleidok.audio.spectrum;

import kaleidok.util.containers.FloatList;


public interface Spectrum extends FloatList
{
  float getSampleRate();

  float getBin( float freq );

  float getFreq( float n );

  float getFreq( int n );
}
