package kaleidok.io;

import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;


public class FilePermissionAttributes implements FileAttribute<Set<PosixFilePermission>>
{
  private final Set<PosixFilePermission> permissions;


  public FilePermissionAttributes( Set<PosixFilePermission> permissions )
  {
    if (permissions.contains(null))
    {
      throw new NullPointerException(
        "The permission set contains a null element.");
    }
    this.permissions = Collections.unmodifiableSet(permissions);
  }


  @Override
  public String name()
  {
    return "posix:permissions";
  }


  @Override
  public Set<PosixFilePermission> value()
  {
    return permissions;
  }
}
