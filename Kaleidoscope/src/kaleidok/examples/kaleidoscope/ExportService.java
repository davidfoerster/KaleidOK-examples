package kaleidok.examples.kaleidoscope;

import kaleidok.io.platform.PlatformPaths;
import kaleidok.processing.export.itext.ITextExport;
import kaleidok.processing.export.itext.ParagraphHook;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

import java.io.IOException;
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


  public static ExportService fromConfiguration( PApplet sketch,
    Consumer<Consumer<String>> initiator )
  {
    if (!DefaultValueParser.parseBoolean(sketch,
      ITextExport.class.getPackage().getName(), false))
    {
      return null;
    }

    ExportService es = new ExportService(sketch);
    Path outputPath = PlatformPaths.getTempDir().resolve("kaleidok-screenshot.pdf");
    try
    {
      // TODO: Implement a configurable/selectable target path
      es.setOutput(Files.newOutputStream(outputPath));
    }
    catch (IOException ex)
    {
      logThrown(logger, Level.SEVERE,
        "Couldn't instantiate PDF export to {0}",
        ex, outputPath);
      return null;
    }

    if (initiator != null)
      initiator.accept(es.getCallback());

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
