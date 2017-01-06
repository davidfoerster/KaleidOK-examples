package kaleidok.io.platform;

import java.nio.file.Path;


public class OsxPaths extends UnixPaths
{
  @Override
  protected Path getCacheDirImpl()
  {
    return getHomeDir().resolve("Library/Caches");
  }


  @Override
  protected Path getDataDirImpl()
  {
    return getHomeDir().resolve("Library");
  }
}
