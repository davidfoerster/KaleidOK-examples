package kaleidok.exaleads.chromatik;

import kaleidok.flickr.Flickr;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;


public class DocumentChromasthetiator<F extends Flickr>
  extends ChromasthetiatorBase<F>
{
  /**
   * A document object tha holds the currently defined search terms
   */
  public Document keywordsDoc;


  @Override
  protected String getQueryKeywords()
  {
    Document keywordsDoc = this.keywordsDoc;
    if (keywordsDoc != null) {
      try {
        String keywords = keywordsDoc.getText(0, keywordsDoc.getLength());
        if (!keywords.isEmpty())
          return keywords;
      } catch (BadLocationException ex) {
        // this really shouldn't happen with the chosen location
        throw new AssertionError(ex);
      }
    }
    return null;
  }
}
