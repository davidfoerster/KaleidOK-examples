package kaleidok.processing;

import kaleidok.util.Reflection;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.event.TouchEvent;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;


public class Plugin<P extends PApplet>
{
  public enum HookMethod {
    pre, draw, post, pause, resume, dispose,
    mouseEvent(MouseEvent.class),
    keyEvent(KeyEvent.class),
    touchEvent(TouchEvent.class);


    private final Class<?>[] argumentTypes;


    HookMethod( Class<?>... argumentTypes )
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


    static final EnumSet<HookMethod> valueSet =
      EnumSet.allOf(HookMethod.class);

    static final Map<String, HookMethod> valueMap =
      valueSet.stream().collect(
        Collectors.toMap(Enum::name, Function.identity()));


    public static EnumSet<HookMethod> getAvailableHooks( Plugin<?> plugin )
    {
      EnumSet<HookMethod> result = EnumSet.noneOf(HookMethod.class);
      for (HookMethod hm: valueSet)
      {
        if (hm == dispose || hm.isOverriddenOn(plugin))
          result.add(hm);
      }
      return result;
    }


    public static EnumSet<HookMethod> getAvailableHooks( Object plugin )
    {
      if (plugin instanceof Plugin)
        return getAvailableHooks((Plugin<?>) plugin);

      EnumSet<HookMethod> result = EnumSet.noneOf(HookMethod.class);
      outer:
      for (Method m : plugin.getClass().getMethods())
      {
        HookMethod hm = valueMap.get(m.getName());
        if (hm != null && m.getParameterCount() == hm.argumentTypes.length)
        {
          Class<?>[] expectedArgumentTypes = hm.argumentTypes;
          if (expectedArgumentTypes.length != 0)
          {
            Class<?>[] actualArgumentTypes = m.getParameterTypes();
            for (int i = expectedArgumentTypes.length - 1; i >= 0; i--)
            {
              if (!Reflection.getWrapperType(expectedArgumentTypes[i])
                .isAssignableFrom(
                  Reflection.getWrapperType(actualArgumentTypes[i])))
              {
                continue outer;
              }
            }
          }
          result.add(hm);
        }
      }
      return result;
    }


    public static void register( PApplet applet, Object plugin,
      Set<HookMethod> methods )
    {
      for (HookMethod m: methods)
        applet.registerMethod(m.name(), plugin);
    }


    public static void registerAll( PApplet applet, Object plugin )
    {
      register(applet, plugin, getAvailableHooks(plugin));
    }


    public static void unregister( PApplet applet, Object plugin,
      Set<HookMethod> methods )
    {
      for (HookMethod m: methods)
        applet.unregisterMethod(m.name(), plugin);
    }


    public static void unregisterAll( PApplet applet, Object plugin )
    {
      unregister(applet, plugin, getAvailableHooks(plugin));
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
    for (HookMethod method: HookMethod.valueSet)
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


  public P getParent()
  {
    return p;
  }
}
