package kaleidok.io;

import kaleidok.util.Arrays;

import java.nio.file.LinkOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public final class Files
{
  private Files() { }


  public static final FileAttribute<?>[] NO_ATTRIBUTES = {};

  public static final LinkOption[] FOLLOW_LINKS = {};


  private static List<PosixFilePermission> posixFilePermissions = null;

  public static List<PosixFilePermission> getPosixFilePermissions()
  {
    List<PosixFilePermission> l;
    if ((l = posixFilePermissions) == null)
    {
      posixFilePermissions = l =
        Arrays.asImmutableList(PosixFilePermission.values());
    }
    return l;
  }


  @SuppressWarnings("OctalInteger")
  public static Set<PosixFilePermission> permissionsFromMask( int mask )
  {
    mask &= 0777;

    if (mask == 0777)
      return EnumSet.allOf(PosixFilePermission.class);

    final EnumSet<PosixFilePermission> dst =
      EnumSet.noneOf(PosixFilePermission.class);
    if (mask != 0)
    {
      final List<PosixFilePermission> posixFilePermissions =
        getPosixFilePermissions();
      int maxOrdinal = posixFilePermissions.size() - 1;
      for (PosixFilePermission p: posixFilePermissions)
      {
        if ((mask & (1 << (maxOrdinal - p.ordinal()))) != 0)
          dst.add(p);
      }
    }
    return dst;
  }
}
