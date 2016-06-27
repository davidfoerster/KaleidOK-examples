package kaleidok.examples.kaleidoscope;

import kaleidok.io.platform.PlatformPaths;
import kaleidok.processing.export.itext.ITextExport;
import kaleidok.processing.export.itext.ParagraphHook;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.LoggingUtils.logThrown;


public class ExportService extends ITextExport
{
  private final SchedulingHandler schedulingHandler = new SchedulingHandler();


  public ExportService( PApplet sketch )
  {
    super(sketch);
    pageHook = new ParagraphHook(schedulingHandler);
  }


  @SuppressWarnings("resource")
  public static ExportService fromConfiguration( PApplet sketch )
  {
    if (!DefaultValueParser.parseBoolean(sketch,
      ITextExport.class.getPackage().getName(), false))
    {
      return null;
    }

    // TODO: Implement a configurable/selectable target path
    Path outputPath =
      PlatformPaths.getTempDir().resolve("kaleidok-screenshot.pdf");
    OutputStream outputStream;
    try
    {
      outputStream = Files.newOutputStream(outputPath);
    }
    catch (IOException ex)
    {
      logThrown(logger, Level.SEVERE,
        "Couldn't instantiate PDF export to {0}",
        ex, outputPath);
      return null;
    }

    ExportService es = new ExportService(sketch);
    es.setOutput(outputStream);
    return es;
  }


  public Consumer<String> getCallback()
  {
    return schedulingHandler;
  }


  private class SchedulingHandler
    implements Consumer<String>, Supplier<String>
  {
    private String text = null;

    @Override
    public String get()
    {
      return text;
    }

    @Override
    public void accept( String text )
    {
      this.text = text;
      schedule();
    }
  }
}
