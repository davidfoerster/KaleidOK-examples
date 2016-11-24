package kaleidok.exaleads.chromatik;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.AspectedObjectProperty;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.IntegerPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringConversionPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.javafx.util.converter.UriStringConverter;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;


public class PropertyChromatikQuery extends ChromatikQuery
  implements PreferenceBean
{
  private final AspectedIntegerProperty start;

  private final AspectedIntegerProperty nHits;

  private final AspectedStringProperty keywords;

  private final AspectedObjectProperty<URI> baseUri;


  public PropertyChromatikQuery()
  {
    this(QUERY_NHITS_DEFAULT, null, (int[]) null);
  }


  public PropertyChromatikQuery( int nHits, String keywords, int... colors )
  {
    super(colors);

    start = new AspectedIntegerProperty(this, "result start index", 0);
    IntegerSpinnerValueFactory startBounds =
      new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
    startBounds.setAmountToStepBy(10);
    start.addAspect(BoundedIntegerTag.getIntegerInstance(), startBounds);
    start.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(start, ChromatikQuery.class));

    this.nHits = new AspectedIntegerProperty(this, "result set size", nHits);
    IntegerSpinnerValueFactory nHitsBounds =
      new IntegerSpinnerValueFactory(1, 200);
    nHitsBounds.setAmountToStepBy(10);
    this.nHits.addAspect(BoundedIntegerTag.getIntegerInstance(), nHitsBounds);
    this.nHits.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new IntegerPropertyPreferencesAdapter<>(this.nHits, ChromatikQuery.class));

    this.keywords =
      new AspectedStringProperty(this, "keywords",
        (keywords != null) ? keywords : "");
    this.keywords.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringPropertyPreferencesAdapter<>(this.keywords, ChromatikQuery.class));

    baseUri = new AspectedObjectProperty<>(this, "API base URI", DEFAULT_URI);
    baseUri.addAspect(StringConverterAspectTag.getInstance(),
      UriStringConverter.INSTANCE);
    baseUri.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringConversionPropertyPreferencesAdapter<>(baseUri,
        ChromatikQuery.class, UriStringConverter.INSTANCE));
  }


  public IntegerProperty startProperty()
  {
    return start;
  }

  @Override
  public int getStart()
  {
    return start.get();
  }

  @Override
  public void setStart( int start )
  {
    this.start.set(start);
  }


  public IntegerProperty getNHitsProperty()
  {
    return nHits;
  }

  @Override
  public int getNHits()
  {
    return nHits.get();
  }

  @Override
  public void setNHits( int nHits )
  {
    this.nHits.set(nHits);
  }


  public StringProperty keywordProperty()
  {
    return keywords;
  }

  @Override
  public String getKeywords()
  {
    return keywords.get();
  }

  @Override
  public void setKeywords( String keywords )
  {
    this.keywords.set((keywords != null) ? keywords : "");
  }


  public ObjectProperty<URI> baseUriProperty()
  {
    return baseUri;
  }

  @Override
  public URI getBaseUri()
  {
    return baseUri.get();
  }

  @Override
  public void setBaseUri( URI baseUri )
  {
    this.baseUri.set(Objects.requireNonNull(baseUri));
  }


  @Override
  public ChromatikQuery toSimple()
  {
    return new SimpleChromatikQuery(this);
  }


  @Override
  public String getName()
  {
    return "Chromatik query";
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
    return Stream.of(start, nHits, keywords, baseUri)
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny);
  }
}
