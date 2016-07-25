package kaleidok.io.platform;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

import static kaleidok.io.platform.PlatformPaths.NO_ATTRIBUTES;
import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;


@SuppressWarnings({ "unused", "MethodMayBeStatic" })
public class PlatformPathsBase
{
  protected PlatformPathsBase() { }


  static Path getEnvDir( String envName, Path defaultPath )
  {
    String dir = System.getenv(envName);
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir, EMPTY_STRING_ARRAY) :
      defaultPath;
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
    return Paths.get(System.getProperty("user.home"), EMPTY_STRING_ARRAY);
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
    return Paths.get(System.getProperty("java.io.tmpdir"), EMPTY_STRING_ARRAY);
  }


  protected FileAttribute<?>[] getTempDirectoryDefaultAttributes()
  {
    return NO_ATTRIBUTES;
  }

  protected FileAttribute<?>[] getTempFileDefaultAttributes()
  {
    return NO_ATTRIBUTES;
  }


  public Path createTempDirectory( String prefix ) throws IOException
  {
    //noinspection ConfusingArgumentToVarargsMethod
    return createTempDirectory( prefix, (FileAttribute<?>[]) null );
  }

  public Path createTempDirectory( String prefix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    if (attrs == null || attrs.length == 0)
      attrs = getTempDirectoryDefaultAttributes();
    return Files.createTempDirectory(getTempDir(), prefix, attrs);
  }


  public Path createTempFile( String prefix, String suffix ) throws IOException
  {
    //noinspection ConfusingArgumentToVarargsMethod
    return createTempFile(prefix, suffix, (FileAttribute<?>[]) null);
  }

  public Path createTempFile( String prefix, String suffix,
    FileAttribute<?>... attrs )
    throws IOException
  {
    if (attrs == null || attrs.length == 0)
      attrs = getTempFileDefaultAttributes();
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


  public Path getCacheDir( String name ) throws IOException
  {
    //noinspection ConfusingArgumentToVarargsMethod
    return getCacheDir(name, (FileAttribute<?>[]) null);
  }

  public Path getCacheDir( String name, FileAttribute<?>... attrs )
    throws IOException
  {
    if (name.isEmpty())
      throw new IllegalArgumentException("Empty cache directory name");
    if (attrs == null)
      attrs = NO_ATTRIBUTES;
    return Files.createDirectories(
      getCacheDir().resolve(name), attrs);
  }


  private Path dataDir;

  public Path getDataDir()
  {
    if (dataDir == null)
      dataDir = getDataDirImpl();
    return dataDir;
  }

  protected Path getDataDirImpl()
  {
    throw new UnsupportedOperationException();
  }


  public Path getDataDir( String name ) throws IOException
  {
    //noinspection ConfusingArgumentToVarargsMethod
    return getDataDir(name, (FileAttribute<?>[]) null);
  }

  public Path getDataDir( String name, FileAttribute<?>... attrs )
    throws IOException
  {
    if (name.isEmpty())
      throw new IllegalArgumentException("Empty data directory name");
    if (attrs == null)
      attrs = NO_ATTRIBUTES;
    return Files.createDirectories(
      getDataDir().resolve(name), attrs);
  }
}
