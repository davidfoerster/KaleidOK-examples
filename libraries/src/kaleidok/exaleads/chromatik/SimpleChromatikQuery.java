package kaleidok.exaleads.chromatik;

import java.io.Serializable;
import java.net.URI;

import static java.util.Objects.requireNonNull;


/**
 * Holds all the parameters to build a Chromatik image search query.
 */
public class SimpleChromatikQuery extends ChromatikQuery
  implements Serializable, Cloneable
{
  private static final long serialVersionUID = -4020430402877695173L;

  /**
   * The start index of the requested section of the result set
   */
  private int start;

  /**
   * Maximum result set section size
   */
  private int nHits;

  /**
   * Search keywords (if any) separated by spaces
   */
  private String keywords;

  /**
   * The protocol, host, and path component of the query URL
   */
  private URI baseUri;


  /**
   * Constructs a query object with the default result set size and no
   * keywords.
   */
  public SimpleChromatikQuery()
  {
    this(QUERY_NHITS_DEFAULT, null, (int[]) null);
  }


  /**
   * Constructs a query object with preset parameters.
   *
   * @param nHits  Result set size
   * @param keywords  Query keywords
   * @param colors  RGB color values to search for; the weight is the inverse
   *   of the amount of colors
   */
  public SimpleChromatikQuery( int nHits, String keywords, int... colors )
  {
    super(colors);
    this.start = 0;
    this.nHits = nHits;
    this.keywords = (keywords != null) ? keywords : "";
    this.baseUri = DEFAULT_URI;
  }


  SimpleChromatikQuery( ChromatikQuery other )
  {
    super(other.copyOptionMap());
    this.start = other.getStart();
    this.nHits = other.getNHits();
    this.keywords = other.getKeywords();
    this.baseUri = other.getBaseUri();
  }


  @SuppressWarnings({ "unchecked", "CloneCallsConstructors" })
  @Override
  public SimpleChromatikQuery clone()
  {
    SimpleChromatikQuery other;
    try
    {
      other = (SimpleChromatikQuery) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }
    other.optionMap = other.copyOptionMap();
    return other;
  }


  @Override
  public int getStart()
  {
    return start;
  }

  @Override
  public void setStart( int start )
  {
    this.start = start;
  }


  @Override
  public int getNHits()
  {
    return nHits;
  }

  @Override
  public void setNHits( int nHits )
  {
    this.nHits = nHits;
  }


  @Override
  public String getKeywords()
  {
    return keywords;
  }

  @Override
  public void setKeywords( String keywords )
  {
    this.keywords = (keywords != null) ? keywords : "";
  }


  @Override
  public URI getBaseUri()
  {
    return baseUri;
  }

  @Override
  public void setBaseUri( URI baseUri )
  {
    this.baseUri = requireNonNull(baseUri);
  }


  @Override
  public ChromatikQuery toSimple()
  {
    return clone();
  }
}
