package kaleidok.io.platform;

import kaleidok.util.Arrays;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static kaleidok.util.Arrays.EMPTY_STRINGS;


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
      Paths.get(dir, EMPTY_STRINGS) :
      super.getTempDirImpl();
  }


  public static final List<PosixFilePermission> posixFilePermissions =
    Arrays.asImmutableList(PosixFilePermission.values());

  public static Set<PosixFilePermission> permissionsFromMask( int mask )
  {
    final EnumSet<PosixFilePermission> dst =
      EnumSet.noneOf(PosixFilePermission.class);
    if ((mask & 0777) != 0) {
      for (PosixFilePermission p: posixFilePermissions) {
        final int pMask = 1 << (posixFilePermissions.size() - p.ordinal() - 1);
        if ((mask & pMask) != 0)
          dst.add(p);
      }
    }
    return dst;
  }


  private static final FileAttribute<?>[]
    DEFAULT_TEMPFILE_ATTRIBUTES = { asFileAttribute(permissionsFromMask(0600)) },
    DEFAULT_TEMPDIR_ATTRIBUTES = { asFileAttribute(permissionsFromMask(0700)) };

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
    return pathGetXdgHome("CACHE", ".cache");
  }


  @Override
  protected Path getDataDirImpl()
  {
    return pathGetXdgHome("DATA", ".local/share");
  }


  private Path pathGetXdgHome( String type, String defaultPath )
  {
    String dir = System.getenv("XDG_" + type + "_HOME");
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir, EMPTY_STRINGS) :
      getHomeDir().resolve(defaultPath);
  }
}
