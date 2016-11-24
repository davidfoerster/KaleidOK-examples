package kaleidok.exaleads.chromatik;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.flickr.Flickr;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.adapter.preference.IntegerPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;

import java.util.function.Function;
import java.util.stream.Stream;


public class PropertyChromasthetiator<F extends Flickr> extends Chromasthetiator<F>
  implements PreferenceBean
{
  private final AspectedIntegerProperty maxColors;

  private final AspectedIntegerProperty maxKeywords;

  private final ObjectProperty<ChromatikQuery> chromatikQuery;


  public PropertyChromasthetiator()
  {
    maxColors =
      new AspectedIntegerProperty(this, "max. colors", 2);
    maxColors.addAspect(BoundedIntegerTag.getIntegerInstance(),
      new IntegerSpinnerValueFactory(0, 16));
    maxColors.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(maxColors, Chromasthetiator.class));

    maxKeywords =
      new AspectedIntegerProperty(this, "max. keywords", 0);
    maxKeywords.addAspect(BoundedIntegerTag.getIntegerInstance(),
      new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
    maxKeywords.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(maxKeywords, Chromasthetiator.class));

    chromatikQuery =
      new SimpleObjectProperty<>(this, "chromatik query",
        new PropertyChromatikQuery()
        {
          @Override
          public Object getParent()
          {
            return PropertyChromasthetiator.this;
          }
        });
  }


  /**
   * Maximum amount of colors to use in the query to Chromatik
   */
  public IntegerProperty maxColorsProperty()
  {
    return maxColors;
  }

  @Override
  public int getMaxColors()
  {
    return maxColors.get();
  }

  @Override
  public void setMaxColors( int maxColors )
  {
    this.maxColors.set(maxColors);
  }


  /**
   * Maximum amount of keywords to select from affect words, if no search terms
   * are specified in {@link SimpleChromatikQuery#keywords}.
   */
  public IntegerProperty maxKeywordsProperty()
  {
    return maxKeywords;
  }

  @Override
  public int getMaxKeywords()
  {
    return maxKeywords.get();
  }

  @Override
  public void setMaxKeywords( int n )
  {
    maxKeywords.set(n);
  }


  public ObjectProperty<? extends ChromatikQuery> chromatikQueryProperty()
  {
    return chromatikQuery;
  }

  @Override
  public ChromatikQuery getChromatikQuery()
  {
    return chromatikQuery.get();
  }

  @Override
  public void setChromatikQuery( ChromatikQuery chromatikQuery )
  {
    this.chromatikQuery.set(chromatikQuery);
  }


  @Override
  public SimpleChromasthetiator<F> toSimple()
  {
    SimpleChromasthetiator<F> copy = new SimpleChromasthetiator<>(this);
    copy.setChromatikQuery(copy.getChromatikQuery().toSimple());
    return copy;
  }


  @Override
  public String getName()
  {
    return "Chromasthetiator";
  }


  @Override
  public Object getParent()
  {
    return null;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    Stream<? extends PropertyPreferencesAdapter<?, ?>> s = Stream.of(
      maxColors.getAspect(PropertyPreferencesAdapterTag.getWritableInstance()),
      maxKeywords.getAspect(PropertyPreferencesAdapterTag.getWritableInstance()));

    ChromatikQuery chromatikQuery = getChromatikQuery();
    if (chromatikQuery instanceof PropertyChromatikQuery)
    {
      s =
        Stream.of(
          s, ((PropertyChromatikQuery) chromatikQuery).getPreferenceAdapters(),
          flickr.getPreferenceAdapters())
        .flatMap(Function.identity());
    }

    return s;
  }
}
