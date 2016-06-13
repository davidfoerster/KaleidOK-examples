package kaleidok.processing;

import javax.swing.JApplet;
import java.lang.reflect.InvocationTargetException;


public interface PAppletFactory<T extends ExtPApplet>
{
  T createInstance( JApplet parent ) throws InvocationTargetException;
}
