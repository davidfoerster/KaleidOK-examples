package kaleidok.net.http;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import kaleidok.net.http.responsehandler.JsonMimeTypeChecker;
import kaleidok.net.http.util.MimeTypeMap;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;


public class HttpConnectionTest
{
  public static final String
    PATH = "/",
    MIME_TYPE = ContentType.TEXT_PLAIN.getMimeType(),
    BODY = "¿Föøbår…‽";

  public static final Charset CHARSET = StandardCharsets.UTF_8;

  public static final String CONTENT_TYPE =
    MIME_TYPE + ";charset=" + CHARSET.name();

  private static final byte[] BODY_BYTES = BODY.getBytes(CHARSET);


  @Rule
  public WireMockRule wireMockRule = new WireMockRule();

  public HttpConnection con;


  public void setUp(ResponseDefinitionBuilder r) throws IOException
  {
    setUp(r, CONTENT_TYPE);
  }


  public void setUp(ResponseDefinitionBuilder r, String contentType) throws IOException
  {
    r.withHeader(ContentTypeHeader.KEY, contentType);
    stubFor(get(urlEqualTo(PATH)).willReturn(r));

    con = HttpConnection.openURL(
      new URL("http", "localhost", wireMockRule.port(), PATH));
  }


  @Test
  public void testGetBody() throws IOException
  {
    setUp(aResponse().withBody(BODY_BYTES));
    assertEquals(BODY, con.getBody());
  }


  @Test
  public void testGetBodyDeflate() throws IOException
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
  public void testGetBodyGzip() throws IOException
  {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try (GZIPOutputStream comp = new GZIPOutputStream(buf))
    {
      comp.write(BODY_BYTES);
    }

    setUp(aResponse()
      .withBody(buf.toByteArray())
      .withHeader("Content-Encoding", "gzip"));
    assertEquals(BODY, con.getBody());
  }


  @Test
  public void testGetResponseContentType() throws IOException
  {
    setUp(aResponse());
    assertEquals(MIME_TYPE, con.getResponseMimeType());
    assertEquals(CHARSET, con.getResponseCharset(false));
    assertTrue(con.c.getContentLengthLong() <= 0);
  }


  @Test
  public void testGetResponseMimeType1() throws IOException
  {
    setUp(aResponse());
    con.getAcceptedMimeTypes().put(MIME_TYPE, null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }

  @Test
  public void testGetResponseMimeType2() throws IOException
  {
    setUp(aResponse());
    con.getAcceptedMimeTypes().put("text/*", null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }


  @Test
  public void testGetResponseMimeType3() throws IOException
  {
    setUp(aResponse());
    con.getAcceptedMimeTypes().put(MimeTypeMap.WILDCARD, null);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }


  @Test
  public void testGetResponseMimeType4() throws IOException
  {
    setUp(aResponse());
    con.setAcceptedMimeTypes(JsonMimeTypeChecker.MIME_TYPE_MAP);
    assertEquals(MIME_TYPE, con.getResponseMimeType());
  }
}
