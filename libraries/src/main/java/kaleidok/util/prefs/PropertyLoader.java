package kaleidok.util.prefs;

import kaleidok.net.UriUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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


  public static URL[] load( Properties prop, Charset charset,
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

    Stream<URI> resourceURIs = Stream.of(resourcePaths)
      .map((clazz != null) ? clazz::getResource : classLoader::getResource)
      .filter(Objects::nonNull)
      // Cross our fingers that #getRsource() only returns simple, RFC-2396-compliant URLs.
      .map(UriUtil.toUriFunction(AssertionError::new));
    Stream<URI> filesystemURIs = Stream.of(filesystemPaths)
      .map(File::new)
      .filter(File::exists)
      .map(File::toURI);

    URL[] urls = Stream.concat(resourceURIs, filesystemURIs).sequential()
      .distinct()
      /*
       * All URIs here either come from Class#getResource(),
       * ClassLoader#getResource() or File#toUri(), all of which (should)
       * return results that have a canonical URL representation.
       */
      .map(UriUtil.toUrlFunction(AssertionError::new))
      .toArray(URL[]::new);

    load(prop, charset, urls);
    return urls;
  }


  public static URL[] load( Properties prop, Charset charset,
    Object classLoaderReference, String resourceAndFilePath )
    throws IOException
  {
    if (!resourceAndFilePath.isEmpty() &&
      (resourceAndFilePath.charAt(0) == '/' ||
         resourceAndFilePath.charAt(0) == File.separatorChar))
    {
      throw new IllegalArgumentException(
        "Can’t use an absolute resource and path name here");
    }

    String[] a = { resourceAndFilePath };
    return load(prop, charset, classLoaderReference, a, a);
  }


  public static Map<String, String> toMap( Properties src )
  {
    return src.isEmpty() ?
      Collections.emptyMap() :
      src.stringPropertyNames().stream().sequential()
        .collect(Collectors.toMap(
          Function.identity(), src::getProperty,
          (a, b) -> (b != null) ? b : a));
  }
}
