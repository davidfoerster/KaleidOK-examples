package kaleidok.processing.export.itext;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.commons.lang.StringUtils;

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
    if (!StringUtils.isEmpty(text))
    {
      doc.add(new Paragraph(text));
    }
  }
}
