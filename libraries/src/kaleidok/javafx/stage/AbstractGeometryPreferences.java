package kaleidok.javafx.stage;

import kaleidok.javafx.beans.property.AspectedBooleanProperty;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.adapter.preference.BooleanPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.DoublePropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.HiddenAspectTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.util.Reflection;

import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static kaleidok.util.prefs.PreferenceUtils.KEY_PART_DELIMITER;


public abstract class AbstractGeometryPreferences<G> implements PreferenceBean
{
  public static final int
    POSITION = 1,
    SIZE = 2,
    SHOW = 4,
    ALL = POSITION | SIZE | SHOW;

  private static final String NAME = "geometry";


  private final Object parent;

  public final AspectedDoubleProperty x, y, w, h;

  public final AspectedBooleanProperty show;


  protected AbstractGeometryPreferences( Object parent, boolean hideProperties,
    int mode )
  {
    this(parent, Objects.requireNonNull(parent, "parent").getClass(),
      hideProperties, mode);
  }


  protected AbstractGeometryPreferences( Object parent, Class<?> beanClass,
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
    String geometryKeyPrefix =
      Reflection.getAnonymousClassSimpleName(beanClass) +
        KEY_PART_DELIMITER + NAME;

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
      if (hideProperties)
        show.addAspect(HiddenAspectTag.getInstance());
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
    if (hide)
      prop.addAspect(HiddenAspectTag.getInstance());
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
  public Object getParent()
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
