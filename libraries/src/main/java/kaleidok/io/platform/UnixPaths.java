package kaleidok.io.platform;

import kaleidok.io.FilePermissionAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.EnumSet;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;


@SuppressWarnings("OctalInteger")
public class UnixPaths extends PlatformPathsBase
{
  @Override
  protected Path getTempDirImpl()
  {
    String dir = System.getenv("TMPDIR");
    if (dir == null || dir.isEmpty())
      dir = System.getenv("TMP");
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir, EMPTY_STRING_ARRAY) :
      super.getTempDirImpl();
  }


  private static final FileAttribute<?>[]
    DEFAULT_TEMPFILE_ATTRIBUTES = {
        new FilePermissionAttributes(EnumSet.of(OWNER_READ, OWNER_WRITE))
      },
    DEFAULT_TEMPDIR_ATTRIBUTES = {
        new FilePermissionAttributes(EnumSet.of(
          OWNER_READ, OWNER_WRITE, OWNER_EXECUTE))
      };

  @Override
  protected FileAttribute<?>[] getTempDirectoryDefaultAttributes()
  {
    return DEFAULT_TEMPDIR_ATTRIBUTES;
  }

  @Override
  protected FileAttribute<?>[] getTempFileDefaultAttributes()
  {
    return DEFAULT_TEMPFILE_ATTRIBUTES;
  }


  @Override
  protected Path getCacheDirImpl()
  {
    return pathGetXdgHome("XDG_CACHE_HOME", ".cache");
  }


  @Override
  protected Path getDataDirImpl()
  {
    return pathGetXdgHome("XDG_DATA_HOME", ".local/share");
  }


  private Path pathGetXdgHome( String type, String defaultPath )
  {
    String dir = System.getenv(type);
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir, EMPTY_STRING_ARRAY) :
      getHomeDir().resolve(defaultPath);
  }
}
