package kaleidok.io.platform;

import java.nio.file.Path;
import java.nio.file.Paths;


public class WindowsPaths extends PlatformPathsBase
{
  @Override
  protected Path getTempDirImpl()
  {
    return getEnvDir("TEMP", super.getTempDirImpl());
  }


  @Override
  protected Path getCacheDirImpl()
  {
    //noinspection SpellCheckingInspection
    return getEnvDir("LOCALAPPDATA", getHomeDir());
  }


  @Override
  protected Path getDataDirImpl()
  {
    //noinspection SpellCheckingInspection
    return getEnvDir("APPDATA", getHomeDir());
  }


  private Path getEnvDir( String envName, Path defaultPath )
  {
    String dir = System.getenv(envName);
    return (dir != null && !dir.isEmpty()) ? Paths.get(dir) : defaultPath;
  }
}
