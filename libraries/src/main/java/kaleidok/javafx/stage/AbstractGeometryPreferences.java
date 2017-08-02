package kaleidok.javafx.stage;

import kaleidok.javafx.beans.property.AspectedBooleanProperty;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.adapter.preference.BooleanPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.DoublePropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.HiddenAspectTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.value.ConstantBooleanValue;
import kaleidok.util.Reflection;

import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static kaleidok.util.prefs.PreferenceUtils.KEY_PART_DELIMITER;


/**
 * Abstract base class to help save and restore (window) geometries to and from
 * the Preferences interface.
 *
 * @param <G> The graphics object with the geometry to store
 * @param <P> The “parent” application holding the preferences
 */
public abstract class AbstractGeometryPreferences<G, P> implements PreferenceBean
{
  public static final int
    POSITION = 1,
    SIZE = 2,
    SHOW = 4,
    ALL = POSITION | SIZE | SHOW;

  private static final String NAME = "geometry";


  private final P parent;

  public final AspectedDoubleProperty x, y, w, h;

  public final AspectedBooleanProperty show;


  protected AbstractGeometryPreferences( P parent, boolean hideProperties,
    int mode )
  {
    this(parent, Objects.requireNonNull(parent, "parent").getClass(),
      hideProperties, mode);
  }


  protected AbstractGeometryPreferences( P parent, Class<?> beanClass,
    boolean hideProperties, int mode )
  {
    if ((mode & ALL) == 0)
    {
      throw new IllegalArgumentException(
        "At least one valid mode flag must be set");
    }

    this.parent = parent;
    Preferences preferences = Preferences.userNodeForPackage(beanClass);
    String beanClassName =
      Reflection.getAnonymousClassSimpleName(beanClass);
    String geometryKeyPrefix = beanClassName + KEY_PART_DELIMITER + NAME;

    if ((mode & POSITION) != 0)
    {
      x = makeGeometryProperty(
        "left", preferences, geometryKeyPrefix, hideProperties);
      y = makeGeometryProperty(
        "top", preferences, geometryKeyPrefix, hideProperties);
    }
    else
    {
      x = null;
      y = null;
    }

    if ((mode & SIZE) != 0)
    {
      w = makeGeometryProperty(
        "width", preferences, geometryKeyPrefix, hideProperties);
      h = makeGeometryProperty(
        "height", preferences, geometryKeyPrefix, hideProperties);
    }
    else
    {
      w = null;
      h = null;
    }

    if ((mode & SHOW) != 0)
    {
      show = new AspectedBooleanProperty(this, "show");
      show.addAspect(HiddenAspectTag.getInstance(),
        ConstantBooleanValue.of(hideProperties));
      show.addAspect(PropertyPreferencesAdapterTag.getInstance(),
        new BooleanPropertyPreferencesAdapter<>(show, preferences, beanClassName));
    }
    else
    {
      show = null;
    }
  }


  private AspectedDoubleProperty makeGeometryProperty( String name,
    Preferences preferences, String preferencesKeyPrefix, boolean hide )
  {
    AspectedDoubleProperty prop =
      new AspectedDoubleProperty(this, name, Double.NaN);
    prop.addAspect(HiddenAspectTag.getInstance(),
      ConstantBooleanValue.of(hide));
    prop.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new DoublePropertyPreferencesAdapter<>(prop, preferences,
        preferencesKeyPrefix));
    return prop;
  }


  public boolean isShowing()
  {
    return isShowing(true);
  }

  public boolean isShowing( boolean boundStatus )
  {
    return show.isBound() == boundStatus && show.get();
  }


  public abstract void applyGeometry( G graphics );


  public abstract void bind( G graphics );


  public void applyGeometryAndBind( G graphics )
  {
    applyGeometry(graphics);
    bind(graphics);
  }


  @Override
  public String getName()
  {
    return NAME;
  }


  @Override
  public P getParent()
  {
    return parent;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.of(x, y, w, h, show)
      .filter(Objects::nonNull)
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny);
  }
}
