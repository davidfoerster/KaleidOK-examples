package kaleidok.processing;

import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.event.TouchEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;


@SuppressWarnings("unused")
public class Plugin<P extends PApplet>
{
  public enum HookMethod {
    pre, draw, post, pause, resume, dispose,
    mouseEvent, keyEvent, touchEvent
  }

  private static final Map<HookMethod, Class<?>[]> HOOK_METHODS =
    new EnumMap<HookMethod, Class<?>[]>(HookMethod.class) {{
      final Class<?>[] emptyArgs = new Class<?>[0];
      put(HookMethod.pre, emptyArgs);
      put(HookMethod.draw, emptyArgs);
      put(HookMethod.post, emptyArgs);
      put(HookMethod.pause, emptyArgs);
      put(HookMethod.resume, emptyArgs);
      put(HookMethod.dispose, emptyArgs);
      put(HookMethod.mouseEvent, new Class<?>[]{ MouseEvent.class });
      put(HookMethod.keyEvent, new Class<?>[]{ KeyEvent.class });
      put(HookMethod.touchEvent, new Class<?>[]{ TouchEvent.class });
      assert this.size() == HookMethod.values().length;
    }};


  protected final P p;

  private final EnumSet<HookMethod> registeredHooks =
    EnumSet.noneOf(HookMethod.class);


  protected Plugin( P sketch )
  {
    this.p = sketch;
    registerHooks();
  }


  private void registerHooks()
  {
    @SuppressWarnings("unchecked")
    final Class<? extends Plugin<P>> clazz =
      (Class<? extends Plugin<P>>) this.getClass();

    for (Map.Entry<HookMethod, Class<?>[]> e: HOOK_METHODS.entrySet())
    {
      HookMethod method = e.getKey();
      Class<?>[] argsTypes = e.getValue();
      try
      {
        if (method == HookMethod.dispose ||
          clazz.getMethod(method.name(), argsTypes).getDeclaringClass() !=
            Plugin.class)
        {
          p.registerMethod(method.name(), this);
          registeredHooks.add(method);
        }
      }
      catch (NoSuchMethodException ex)
      {
        throw new AssertionError(ex);
      }
    }
  }


  public void dispose()
  {
    if (!p.finished)
    {
      for (HookMethod m: registeredHooks)
        p.unregisterMethod(m.name(), this);
    }
  }


  public void pre() { }

  public void draw() { }

  public void post() { }

  public void resume() { }

  public void pause() { }

  public void mouseEvent( MouseEvent ev ) { }

  public void keyEvent( KeyEvent ev ) { }

  public void touchEvent( TouchEvent ev ) { }
}
