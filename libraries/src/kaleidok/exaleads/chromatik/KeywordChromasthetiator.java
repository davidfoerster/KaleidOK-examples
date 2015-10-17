package kaleidok.exaleads.chromatik;


public class KeywordChromasthetiator<Flickr extends kaleidok.flickr.Flickr>
  extends ChromasthetiatorBase<Flickr>
{
  private String keywords;

  KeywordChromasthetiator( ChromasthetiatorBase<?> other )
  {
    super(other);
    keywords = other.getQueryKeywords();
  }

  @Override
  protected String getQueryKeywords()
  {
    return keywords;
  }
}
