package kaleidok.processing.export.itext;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import kaleidok.util.Strings;

import java.util.function.Consumer;
import java.util.function.Supplier;


public class ParagraphHook implements Consumer<Document>
{
  public Supplier<String> textSource;


  public ParagraphHook()
  {
    this(null);
  }

  public ParagraphHook( Supplier<String> textSource )
  {
    this.textSource = textSource;
  }


  @Override
  public void accept( Document doc )
  {
    String text = (textSource != null) ? textSource.get() : null;
    if (!Strings.isEmpty(text))
    {
      doc.add(new Paragraph(text));
    }
  }
}
