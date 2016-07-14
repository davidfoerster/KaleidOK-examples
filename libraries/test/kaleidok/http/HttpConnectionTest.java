package kaleidok.http;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import kaleidok.http.responsehandler.JsonMimeTypeChecker;
import kaleidok.http.util.MimeTypeMap;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;


public class HttpConnectionTest
{
  public static final String
    PATH = "/",
    MIME_TYPE = "text/plain",
    BODY = "¿Föøbår…‽";

  public static final Charset CHARSET = Charset.forName("utf-8");

  private static final byte[] BODY_BYTES = BODY.getBytes(CHARSET);


  @Rule
  public WireMockRule wireMockRule = new WireMockRule();

  public HttpConnection con;

  public void setUp(ResponseDefinitionBuilder r) throws Exception
  {
    setUp(r, MIME_TYPE + ";charset=" + CHARSET.name());
  }

  public void setUp(ResponseDefinitionBuilder r, String contentType) throws Exception
  {
    r.withHeader("Content-Type", contentType);
    stubFor(get(urlEqualTo(PATH)).willReturn(r));

    con = HttpConnection.openURL(
      new URL("http", "localhost", wireMockRule.port(), PATH));
  }


  @Test
  public void testGetBody() throws Exception
  {
    setUp(aResponse().withBody(BODY_BYTES));
    assertEquals(BODY, con.getBody());
  }


  @Test
  public void testGetBodyDeflate() throws Exception
  {
    setUp(aResponse()
      .withBody(deflate(BODY_BYTES, true))
      .withHeader("Content-Encoding", "deflate"));
    assertEquals(BODY, con.getBody());
  }


  /*
  @Test
  public void testGetBodyDeflateRaw() throws Exception
  {
    setUp(aResponse()
      .withBody(deflate(BODY_BYTES, false))
      .withHeader("Content-Encoding", "deflate"));
    assertEquals(BODY, con.getBody());
  }
  */


  private static byte[] deflate( byte[] data, boolean wrapped )
  {
    Deflater comp = new Deflater(Deflater.DEFAULT_COMPRESSION, !wrapped);
    comp.setInput(data);
    comp.finish();

    ByteArrayOutputStream dst = new ByteArrayOutputStream();
    byte[] buf = new byte[1 << 10];
    while (!comp.finished()) {
      int count = comp.deflate(buf);
      dst.write(buf, 0, count);
    }
    comp.end();

    return dst.toByteArray();
  }


  @Test
  public void testGetBodyGzip() throws Exception
  {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    GZIPOutputStream comp = new GZIPOutputStream(buf);
    comp.write(BODY_BYTES);
    comp.finish();

    setUp(aResponse()
      .withBody(buf.toByteArray())
      .withHeader("Content-Encoding", "gzip"));
    assertEquals(BODY, con.getBody());
  }


  @Test
  public void testGetResponseContentType() throws Exception
  {
    setUp(aResponse());
    assertEquals(MIME_TYPE, con.getResponseMimeType());
    assertEquals(CHARSET, con.getResponseCharset(false));
    assertTrue(con.c.getContentLengthLong() <= 0);
  }


  @Test
  public void testGetResponseMimeType1() throws Exception
  {
    setUp(aResponse());
    con.acceptedMimeTypes.put(MIME_TYPE, null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }

  @Test
  public void testGetResponseMimeType2() throws Exception
  {
    setUp(aResponse());
    con.acceptedMimeTypes.put("text/*", null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }


  @Test
  public void testGetResponseMimeType3() throws Exception
  {
    setUp(aResponse());
    con.acceptedMimeTypes.put(MimeTypeMap.WILDCARD, null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }


  @Test
  public void testGetResponseMimeType4() throws Exception
  {
    setUp(aResponse());
    con.acceptedMimeTypes = JsonMimeTypeChecker.MIME_TYPE_MAP;
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }
}
