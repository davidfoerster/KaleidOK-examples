package kaleidok.exaleads.chromatik;


import kaleidok.flickr.Flickr;


public class KeywordChromasthetiator<F extends Flickr>
  extends ChromasthetiatorBase<F>
{
  private String keywords;

  /**
   * Analogous to
   * {@link ChromasthetiatorBase#ChromasthetiatorBase(ChromasthetiatorBase)}
   *
   * @param other  The original chromasthetiator
   * @see ChromasthetiatorBase#ChromasthetiatorBase(ChromasthetiatorBase)
   */
  KeywordChromasthetiator( ChromasthetiatorBase<? extends Flickr> other )
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
