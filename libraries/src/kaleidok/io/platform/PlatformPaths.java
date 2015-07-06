package kaleidok.io.platform;

import processing.core.PApplet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;


public class PlatformPaths
{
  protected PlatformPaths() { }


  public static final PlatformPaths INSTANCE;
  static {
    switch (PApplet.platform) {
    case PApplet.WINDOWS:
      INSTANCE = new WindowsPaths();
      break;

    case PApplet.MACOSX:
    case PApplet.LINUX:
      INSTANCE = new UnixPaths();
      break;

    case PApplet.OTHER:
    default:
      INSTANCE = new PlatformPaths();
      break;
    }
  }


  private Path tempDir;

  public Path getTempDir()
  {
    if (tempDir == null)
      tempDir = getTempDirImpl();
    return tempDir;
  }

  protected Path getTempDirImpl()
  {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }


  public Path createTempDirectory( String prefix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    return Files.createTempDirectory(getTempDir(), prefix, attrs);
  }

  public Path createTempFile( String prefix, String suffix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    return Files.createTempFile(getTempDir(), prefix, suffix, attrs);
  }
}
