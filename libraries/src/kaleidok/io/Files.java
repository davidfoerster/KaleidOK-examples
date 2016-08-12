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


  public static final List<PosixFilePermission> posixFilePermissions =
    Arrays.asImmutableList(PosixFilePermission.values());


  @SuppressWarnings("OctalInteger")
  public static Set<PosixFilePermission> permissionsFromMask( int mask )
  {
    final EnumSet<PosixFilePermission> dst =
      EnumSet.noneOf(PosixFilePermission.class);
    if ((mask & 0777) != 0)
    {
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
