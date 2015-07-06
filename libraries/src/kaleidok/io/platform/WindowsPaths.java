package kaleidok.io.platform;

import java.nio.file.Path;
import java.nio.file.Paths;


class WindowsPaths extends PlatformPaths
{
  @Override
  protected Path getTempDirImpl()
  {
    String dir = System.getenv("TEMP");
    return (dir != null && !dir.isEmpty()) ?
      Paths.get(dir) :
      super.getTempDirImpl();
  }


  @Override
  protected Path getCacheDirImpl()
  {
    String dir = System.getenv("LOCALAPPDATA");
    return (dir != null && !dir.isEmpty()) ? Paths.get(dir) : getHomeDir();
  }
}
