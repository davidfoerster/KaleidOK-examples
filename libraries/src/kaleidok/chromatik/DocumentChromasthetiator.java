package kaleidok.chromatik;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.applet.Applet;


public class DocumentChromasthetiator extends ChromasthetiatorBase
{
  /**
   * A document object tha holds the currently defined search terms
   */
  public Document keywordsDoc;


  public DocumentChromasthetiator( Applet parent )
  {
    super(parent);
  }


  @Override
  protected String getQueryKeywords()
  {
    Document keywordsDoc = this.keywordsDoc;
    if (keywordsDoc != null) {
      try {
        String keywords =  keywordsDoc.getText(0, keywordsDoc.getLength());
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
