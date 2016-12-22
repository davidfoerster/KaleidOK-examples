package kaleidok.util.prefs;

import kaleidok.util.function.InstanceSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaleidok.util.AssertionUtils.fastAssert;


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
    fastAssert(clazz != null || classLoader != null);

    Stream<URI> resourceURIs = Stream.of(resourcePaths)
      .map((clazz != null) ? clazz::getResource : classLoader::getResource)
      .filter(Objects::nonNull)
      .map((url) -> {
          try {
            return url.toURI();
          } catch (URISyntaxException ex) {
            // Cross our fingers that #getRsources() only returns simple, RFC-2396-compliant URLs
            throw new AssertionError(ex);
          }
        });
    Stream<URI> filesystemURIs = Stream.of(filesystemPaths)
      .map(File::new)
      .filter(File::exists)
      .map(File::toURI);

    URL[] urls = Stream.concat(resourceURIs, filesystemURIs).sequential()
      .distinct()
      .map((uri) -> {
          try {
            return uri.toURL();
          } catch (MalformedURLException ex) {
            /*
             * All URIs here either come from Class#getResource(),
             * ClassLoader#getResource() or File#toUri(), all of which
             * (should) return results that have a canonical URL
             * representation.
             */
            throw new AssertionError(ex);
          }
        })
      .toArray(URL[]::new);

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

    String[] a = { resourceAndFilePath };
    return load(prop, charset, classLoaderReference, a, a);
  }


  public static Map<String, String> toMap( Properties src,
    Map<String, String> dst )
  {
    if (dst == null)
      dst = new HashMap<>(Math.multiplyExact(src.size() / 3, 4));

    return src.stringPropertyNames().stream().sequential()
      .collect(Collectors.toMap(
        Function.identity(), src::getProperty,
        (a, b) -> (b != null) ? b : a,
        new InstanceSupplier<>(dst)));
  }
}
