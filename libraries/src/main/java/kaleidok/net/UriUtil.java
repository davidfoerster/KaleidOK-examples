package kaleidok.net;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;


public final class UriUtil
{
  private UriUtil() { }


  public static Function<URL, URI> toUriFunction(
    final Function<? super URISyntaxException, ? extends Error> throwCallback )
  {
    Objects.requireNonNull(throwCallback);
    return (url) ->
      {
        try
        {
          return url.toURI();
        }
        catch (URISyntaxException ex)
        {
          throw throwCallback.apply(ex);
        }
      };
  }


  public static Function<URI, URL> toUrlFunction(
    final Function<? super MalformedURLException, ? extends Error> throwCallback )
  {
    Objects.requireNonNull(throwCallback);
    return (uri) ->
      {
        try
        {
          return uri.toURL();
        }
        catch (MalformedURLException ex)
        {
          throw throwCallback.apply(ex);
        }
      };
  }
}
