package kaleidok.io.platform;

import java.nio.file.Path;


class OsxPaths extends UnixPaths
{
  @Override
  protected Path getCacheDirImpl()
  {
    return getDataDir().resolve("Caches");
  }


  @Override
  protected Path getDataDirImpl()
  {
    return getHomeDir().resolve("Library");
  }
}
