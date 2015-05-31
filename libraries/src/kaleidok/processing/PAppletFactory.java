package kaleidok.processing;

import javax.swing.JApplet;


public interface PAppletFactory<T extends ExtPApplet>
{
  T createInstance( JApplet parent ) throws InstantiationException;
}
