package kaleidok.chromatik;

class KeywordChromasthetiator extends ChromasthetiatorBase
{
  private String keywords;

  KeywordChromasthetiator( ChromasthetiatorBase other )
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
