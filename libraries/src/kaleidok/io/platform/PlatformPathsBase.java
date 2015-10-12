package kaleidok.io.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

import static kaleidok.io.platform.PlatformPaths.NO_ATTRIBUTES;


class PlatformPathsBase
{
  protected PlatformPathsBase() { }


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
    return createTempDirectory( prefix, (FileAttribute[]) null );
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
    return createTempFile(prefix, suffix, (FileAttribute[]) null);
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
    return getCacheDir(name, (FileAttribute[]) null);
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
    return getDataDir(name, (FileAttribute[]) null);
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
