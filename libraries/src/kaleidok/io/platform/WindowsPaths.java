package kaleidok.io.platform;

import java.nio.file.Path;
import java.nio.file.Paths;


class WindowsPaths extends PlatformPaths
{
  @Override
  protected Path getTempDirImpl()
  {
    return getEnvDir("TEMP", super.getTempDirImpl());
  }


  @Override
  protected Path getCacheDirImpl()
  {
    return getEnvDir("LOCALAPPDATA", getHomeDir());
  }


  @Override
  protected Path getDataDirImpl()
  {
    return getEnvDir("APPDATA", getHomeDir());
  }


  private Path getEnvDir( String envName, Path defaultPath )
  {
    String dir = System.getenv(envName);
    return (dir != null && !dir.isEmpty()) ? Paths.get(dir) : defaultPath;
  }
}
