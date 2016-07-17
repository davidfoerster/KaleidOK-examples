package kaleidok.processing;

import processing.core.PApplet;

import javax.swing.JApplet;
import java.lang.reflect.InvocationTargetException;


public interface PAppletFactory<T extends PApplet>
{
  T createInstance( JApplet parent ) throws InvocationTargetException;
}
