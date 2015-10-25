package kaleidok.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;


public final class PropertyLoader
{
  private PropertyLoader() { }


  public static int load( Properties prop, Charset charset,
    URL... sources ) throws IOException
  {
    if (charset == null)
      charset = Charset.defaultCharset();

    int count = 0;
    for (URL src: sources) {
      if (src != null) {
        try (Reader r = new InputStreamReader(src.openStream(), charset)) {
          prop.load(r);
          count++;
        }
      }
    }

    return count;
  }


  public static int load( Properties prop, Charset charset,
    Object classLoaderReference, String[] resourcePaths,
    String[] filesystemPaths )
    throws IOException
  {
    Class<?> clazz = null;
    ClassLoader classLoader = null;
    if (classLoaderReference == null) {
      classLoader = ClassLoader.getSystemClassLoader();
    } else if (classLoaderReference instanceof Class) {
      clazz = (Class<?>) classLoaderReference;
    } else if (classLoaderReference instanceof ClassLoader) {
      classLoader = (ClassLoader) classLoaderReference;
    } else {
      clazz = classLoaderReference.getClass();
    }
    assert clazz != null || classLoader != null;

    URL[] urls = new URL[resourcePaths.length + filesystemPaths.length];
    int i = 0;
    for (String rp: resourcePaths) {
      URL url = (clazz != null) ? clazz.getResource(rp) : classLoader.getResource(rp);
      if (url != null)
        urls[i++] = url;
    }
    for (String fp: filesystemPaths) {
      File f = new File(fp);
      if (f.exists())
        urls[i++] = f.toURI().toURL();
    }
    return load(prop, charset, urls);
  }


  public static int load( Properties prop, Charset charset,
    Object classLoaderReference, String resourceAndFilePath )
    throws IOException
  {
    if (!resourceAndFilePath.isEmpty() &&
      (resourceAndFilePath.charAt(0) == '/' ||
         resourceAndFilePath.charAt(0) == File.separatorChar))
    {
      throw new IllegalArgumentException(
        "Can't use an absolute resource and path name here");
    }

    String[] a = new String[]{ resourceAndFilePath };
    return load(prop, charset, classLoaderReference, a, a);
  }
}
