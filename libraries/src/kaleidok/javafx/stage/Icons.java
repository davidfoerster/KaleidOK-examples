package kaleidok.javafx.stage;

import javafx.scene.image.Image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static kaleidok.io.Files.FOLLOW_LINKS;


public final class Icons
{
  private Icons() { }


  public static int ICON_FILE_SIZE_MAX = 16 << 20;


  @SuppressWarnings("ProhibitedExceptionDeclared")
  public static List<Image> makeIcons( URL iconUrl ) throws Exception
  {
    int markSize = ICON_FILE_SIZE_MAX;
    if ("file".equals(iconUrl.getProtocol()))
    {
      Path iconPath = Paths.get(iconUrl.toURI());
      if (Files.isRegularFile(iconPath, FOLLOW_LINKS))
      {
        long iconFileSize = Files.size(iconPath);
        if (iconFileSize <= 0 || iconFileSize > markSize)
        {
          throw new IOException(String.format(
            "Icon file is empty or too large (%d bytes): %s",
            iconFileSize, iconPath));
        }
        markSize = (int) iconFileSize;
      }
    }

    try (InputStream is = new BufferedInputStream(iconUrl.openStream()))
    {
      is.mark(markSize);
      List<Image> icons = new ArrayList<>(6);
      for (int n = 4; n <= 9; n++)
      {
        is.reset();
        double dim = 1 << n;
        Image img = new Image(is, dim, dim, true, true);
        if (img.isError())
          throw img.getException();
        icons.add(img);
      }
      return icons;
    }
  }
}
