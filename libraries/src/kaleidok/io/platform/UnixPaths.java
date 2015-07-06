package kaleidok.io.platform;

import java.nio.file.Path;
import java.nio.file.Paths;


class UnixPaths extends PlatformPaths
{
  @Override
  protected Path getTempDirImpl()
  {
    String dir = System.getenv("TMPDIR");
    if (dir == null || dir.isEmpty())
      dir = System.getenv("TMP");
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir) :
      super.getTempDirImpl();
  }


  @Override
  protected Path getCacheDirImpl()
  {
    String dir = System.getenv("XDG_CACHE_HOME");
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir) :
      getHomeDir().resolve(".cache");
  }
}
