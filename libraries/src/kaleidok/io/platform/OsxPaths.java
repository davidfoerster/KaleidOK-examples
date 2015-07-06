package kaleidok.io.platform;

import java.nio.file.Path;


class OsxPaths extends UnixPaths
{
  @Override
  protected Path getCacheDirImpl()
  {
    return getHomeDir().resolve("Library/Caches");
  }
}
