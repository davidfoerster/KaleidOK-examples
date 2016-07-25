package kaleidok.processing;

import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.event.TouchEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;


@SuppressWarnings("unused")
public class Plugin<P extends PApplet>
{
  public enum HookMethod {
    pre, draw, post, pause, resume, dispose,
    mouseEvent, keyEvent, touchEvent
  }

  private static final Map<HookMethod, Class<?>[]> HOOK_METHODS =
    new EnumMap<HookMethod, Class<?>[]>(HookMethod.class) {{
      put(HookMethod.pre, EMPTY_CLASS_ARRAY);
      put(HookMethod.draw, EMPTY_CLASS_ARRAY);
      put(HookMethod.post, EMPTY_CLASS_ARRAY);
      put(HookMethod.pause, EMPTY_CLASS_ARRAY);
      put(HookMethod.resume, EMPTY_CLASS_ARRAY);
      put(HookMethod.dispose, EMPTY_CLASS_ARRAY);
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


  protected void registerHook( HookMethod hookMethod )
  {
    if (registeredHooks.add(hookMethod))
      p.registerMethod(hookMethod.name(), this);
  }


  protected void unregisterHook( HookMethod hookMethod )
  {
    switch (hookMethod)
    {
    case dispose:
      throw new UnsupportedOperationException(
        "Cannot remove " + hookMethod.name() + " hook");

    default:
      if (registeredHooks.remove(hookMethod))
        p.unregisterMethod(hookMethod.name(), this);
      break;
    }
  }


  public final void dispose()
  {
    if (!registeredHooks.isEmpty() && !p.finished)
    {
      for (HookMethod m: registeredHooks)
        p.unregisterMethod(m.name(), this);
      registeredHooks.clear();
    }

    onDispose();
  }


  protected void onDispose() { }

  public void pre() { }

  public void draw() { }

  public void post() { }

  public void resume() { }

  public void pause() { }

  public void mouseEvent( MouseEvent ev ) { }

  public void keyEvent( KeyEvent ev ) { }

  public void touchEvent( TouchEvent ev ) { }
}
