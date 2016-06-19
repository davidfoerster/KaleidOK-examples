package kaleidok.processing.export.itext;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import kaleidok.processing.export.AbstractFrameRecorder;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.awt.Image;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;


public class ITextExport extends AbstractFrameRecorder
{
  private Document doc;

  public Consumer<Document> pageHook = null;


  public ITextExport( PApplet sketch )
  {
    super(sketch);
  }

  public void close()
  {
    Document doc = this.doc;
    if (doc != null && doc.getPdfDocument().getNumberOfPages() > 0)
    {
      System.out.println("Closing document...");
      doc.close();
    }
  }


  public Document getDocument()
  {
    return doc;
  }


  public void setDocument( Document doc )
  {
    close();
    this.doc = doc;
  }


  public void setOutput( OutputStream ost )
  {
    setDocument(new Document(new PdfDocument(new PdfWriter(ost))));
  }


  @Override
  public boolean isReady()
  {
    return doc != null && !doc.getPdfDocument().isClosed();
  }


  @Override
  protected void doBeginRecord()
  {
    // no initialization necessary
  }


  @Override
  protected void doEndRecord()
  {
    Document doc = this.doc;
    if (doc != null)
    {
      System.out.format("Exporting frame %d...%n", p.frameCount);

      PGraphics g = this.p.g;
      PdfDocument pdfDoc = doc.getPdfDocument();
      PageSize pageSize = new PageSize(g.width, g.height);
      PdfPage page = (pdfDoc.getNumberOfPages() == 0) ?
        pdfDoc.addNewPage(pageSize) :
        doc.add(new AreaBreak(pageSize)).getPdfDocument().getLastPage();
      //doc.setMargins(0, 0, 0, 0);

      PdfCanvas canvas = new PdfCanvas(page);
      try
      {
        canvas.addImage(
          ImageDataFactory.create((Image) g.getNative(), null), 0, 0, false);
      } catch (IOException ex)
      {
        ex.printStackTrace();
      }

      Consumer<Document> pageHook = this.pageHook;
      if (pageHook != null)
        pageHook.accept(doc);
    }
  }


  @Override
  public void dispose()
  {
    super.dispose();
    close();
  }
}
