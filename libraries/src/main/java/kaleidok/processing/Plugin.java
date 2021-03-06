package kaleidok.processing;

import kaleidok.util.Arrays;
import kaleidok.util.Reflection;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.event.TouchEvent;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;


@SuppressWarnings("EmptyMethod")
public class Plugin<P extends PApplet>
{
  public enum HookMethod {
    @SuppressWarnings("unused") pre,
    @SuppressWarnings("unused") draw,
    @SuppressWarnings("unused") post,
    @SuppressWarnings("unused") pause,
    @SuppressWarnings("unused") resume,
    dispose,
    @SuppressWarnings("unused") mouseEvent(MouseEvent.class),
    @SuppressWarnings("unused") keyEvent(KeyEvent.class),
    @SuppressWarnings("unused") touchEvent(TouchEvent.class);


    private final Class<?>[] argumentTypes;


    HookMethod( Class<?>... argumentTypes )
    {
      this.argumentTypes =
        (argumentTypes != null) ? argumentTypes : EMPTY_CLASS_ARRAY;
    }


    HookMethod()
    {
      this((Class<?>[]) null);
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


    public static EnumSet<HookMethod> getAvailableHooks( Plugin<?> plugin )
    {
      EnumSet<HookMethod> result = EnumSet.of(dispose);
      for (HookMethod hm: valueSet)
      {
        if (!result.contains(hm) && hm.isOverriddenOn(plugin))
          result.add(hm);
      }
      return result;
    }


    public static EnumSet<HookMethod> getAvailableHooks( Object plugin )
    {
      return (plugin instanceof Plugin) ?
        getAvailableHooks((Plugin<?>) plugin) :
        Stream.of(plugin.getClass().getMethods())
          .map((m) -> {
              HookMethod hm;
              try {
                hm = valueOf(m.getName());
              } catch (IllegalArgumentException ignored) {
                return null;
              }
              return
                (hm.argumentTypes.length == m.getParameterCount() &&
                  Arrays.equals(hm.argumentTypes, m.getParameterTypes(),
                    HookMethod::isCompatible))
                ? hm : null;
            })
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(() -> EnumSet.noneOf(HookMethod.class)));
    }


    private static boolean isCompatible( Class<?> expected, Class<?> actual )
    {
      return
        Reflection.getWrapperType(expected)
          .isAssignableFrom(Reflection.getWrapperType(actual));
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
