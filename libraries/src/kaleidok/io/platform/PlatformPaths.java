package kaleidok.io.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;


public class PlatformPaths
{
  protected PlatformPaths() { }


  public static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];

  public static final PlatformPaths INSTANCE;
  static {
    String os = System.getProperty("os.name").replace(" ", "");
    INSTANCE =
      os.startsWith("Windows") ? new WindowsPaths() :
      os.startsWith("Linux") ? new LinuxPaths() :
      os.startsWith("MacOSX") ? new OsxPaths() :
      (os.startsWith("Solaris") || os.endsWith("BSD")) ? new UnixPaths() :
        new PlatformPaths();
  }


  private Path homeDir;

  public Path getHomeDir()
  {
    if (homeDir == null)
      homeDir = getHomeDirImpl();
    return homeDir;
  }

  protected Path getHomeDirImpl()
  {
    return Paths.get(System.getProperty("user.home"));
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


  private Path cacheDir;

  public Path getCacheDir()
  {
    if (cacheDir == null)
      cacheDir = getCacheDirImpl();
    return cacheDir;
  }

  protected Path getCacheDirImpl()
  {
    throw new UnsupportedOperationException();
  }


  public Path getCacheDir( String name, FileAttribute<?>... attrs )
    throws IOException
  {
    if (name.isEmpty())
      throw new IllegalArgumentException("Empty cache directory name");
    return Files.createDirectories(
      getCacheDir().resolve(name), attrs);
  }
}
