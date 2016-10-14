package kaleidok.processing;

import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.event.TouchEvent;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Objects;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;


public class Plugin<P extends PApplet>
{
  public enum HookMethod {
    pre, draw, post, pause, resume, dispose,
    mouseEvent(new Class<?>[]{ MouseEvent.class }),
    keyEvent(new Class<?>[]{ KeyEvent.class }),
    touchEvent(new Class<?>[]{ TouchEvent.class });


    private final Class<?>[] argumentTypes;


    HookMethod( Class<?>[] argumentTypes )
    {
      this.argumentTypes = Objects.requireNonNull(argumentTypes);
    }


    HookMethod()
    {
      this(EMPTY_CLASS_ARRAY);
    }


    public Class<?>[] getArgumentTypes()
    {
      return (argumentTypes.length == 0) ? argumentTypes : argumentTypes.clone();
    }


    public Method getMethodReference( Plugin<?> plugin )
    {
      try
      {
        return plugin.getClass().getMethod(name(), argumentTypes);
      }
      catch (NoSuchMethodException ex)
      {
        throw new AssertionError(ex);
      }
    }


    public boolean isOverriddenOn( Plugin<?> plugin )
    {
      return getMethodReference(plugin).getDeclaringClass() != Plugin.class;
    }
  }


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
    for (HookMethod method: HookMethod.values())
    {
      if (method == HookMethod.dispose || method.isOverriddenOn(this))
        registerHook(method);
    }
  }


  @OverridingMethodsMustInvokeSuper
  protected void registerHook( HookMethod hookMethod )
  {
    if (registeredHooks.add(hookMethod))
      p.registerMethod(hookMethod.name(), this);
  }


  @OverridingMethodsMustInvokeSuper
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


  @OverridingMethodsMustInvokeSuper
  public void dispose()
  {
    if (!registeredHooks.isEmpty() && !p.finished)
    {
      for (HookMethod m: registeredHooks)
        p.unregisterMethod(m.name(), this);
      registeredHooks.clear();
    }
  }


  public void pre() { }

  public void draw() { }

  public void post() { }

  public void resume() { }

  public void pause() { }

  @SuppressWarnings("unused")
  public void mouseEvent( MouseEvent ev ) { }

  public void keyEvent( KeyEvent ev ) { }

  @SuppressWarnings("unused")
  public void touchEvent( TouchEvent ev ) { }
}
