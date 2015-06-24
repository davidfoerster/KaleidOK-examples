package kaleidok.audio.spectrum;

import kaleidok.containers.FloatList;


public interface Spectrum extends FloatList
{
  float get( int n );

  int size();

  float getSampleRate();

  float getBin( float freq );

  float getFreq( float n );

  float getFreq( int n );
}
