package kaleidok.io.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;


@SuppressWarnings("unused")
public final class PlatformPaths
{
  private PlatformPaths() { }


  static final PlatformPathsBase platform;
  static {
    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
    String os = System.getProperty("os.name").replace(" ", "");
    platform =
      os.startsWith("Windows") ? new WindowsPaths() :
      os.startsWith("MacOSX") ? new OsxPaths() :
      (os.startsWith("Linux") || os.startsWith("Solaris") || os.endsWith("BSD")) ?
        new UnixPaths() :
        new PlatformPathsBase();
  }


  public static Path getHomeDir()
  {
    return platform.getHomeDir();
  }


  public static Path getTempDir()
  {
    return platform.getTempDir();
  }


  public static Path createTempDirectory( String prefix ) throws IOException
  {
    return createTempDirectory( prefix, (FileAttribute<?>[]) null);
  }

  public static Path createTempDirectory( String prefix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    return platform.createTempDirectory(prefix, attrs);
  }


  public static Path createTempFile( String prefix, String suffix ) throws IOException
  {
    return createTempFile(prefix, suffix, (FileAttribute<?>[]) null);
  }

  public static Path createTempFile( String prefix, String suffix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    return platform.createTempFile(prefix, suffix, attrs);
  }


  public static Path getCacheDir()
  {
    return platform.getCacheDir();
  }

  public static Path getCacheDir( String name ) throws IOException
  {
    return getCacheDir(name, (FileAttribute<?>[]) null);
  }

  public static Path getCacheDir( String name, FileAttribute<?>... attrs )
    throws IOException
  {
    return platform.getCacheDir(name, attrs);
  }


  public static Path getDataDir()
  {
    return platform.getDataDir();
  }

  public static Path getDataDir( String name ) throws IOException
  {
    return getDataDir(name, (FileAttribute<?>[]) null);
  }

  public static Path getDataDir( String name, FileAttribute<?>... attrs )
    throws IOException
  {
    return platform.getDataDir(name, attrs);
  }
}
