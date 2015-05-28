package com.getflourish.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class ClientHttpRequest
{
  private final URLConnection connection;

  private OutputStream os = null;

  private Map<String, String> cookies = new HashMap<>();

  private String boundary;

  protected void connect() throws IOException
  {
    if (os == null)
      os = connection.getOutputStream();
  }

  protected void write(char c) throws IOException
  {
    connect();
    os.write(c);
  }

  protected void write(String s) throws IOException
  {
    connect();
    os.write(s.getBytes());
  }

  private static final byte[] NEWLINE = new byte[]{'\r', '\n'};

  protected void newline() throws IOException
  {
    connect();
    os.write(NEWLINE);
  }

  protected void writeln(String s) throws IOException
  {
    write(s);
    newline();
  }

  private void boundary() throws IOException
  {
    write("--");
    write(boundary);
  }

  public ClientHttpRequest(URLConnection connection) throws IOException
  {
    boundary = "---------------------------";
    this.connection = connection;
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "audio/x-flac; rate=44100");
  }

  public ClientHttpRequest(URL url) throws IOException
  {
    this(url.openConnection());
  }

  public ClientHttpRequest(String urlString) throws IOException
  {
    this(new URL(urlString));
  }

  public void setCookie(String name, String value) throws IOException
  {
    this.cookies.put(name, value);
  }

  public void setCookies(Map<String, String> cookies) throws IOException
  {
    if(cookies != null)
      this.cookies.putAll(cookies);
  }

  public void setCookies(String[] cookies) throws IOException
  {
    if(cookies != null) {
      for (int i = 1; i < cookies.length; i += 2)
        this.setCookie(cookies[i - 1], cookies[i]);
    }
  }

  private void writeName(String name) throws IOException
  {
    newline();
    write("Content-Disposition: form-data; name=\"");
    write(name);
    write('\"');
  }

  public void setParameter(String name, String value) throws IOException
  {
    boundary();
    writeName(name);
    newline();
    newline();
    writeln(value);
  }

  private static void pipe(InputStream in, OutputStream out) throws IOException
  {
    byte[] buf = new byte[1 << 19];
    int nread;
    while ((nread = in.read(buf)) >= 0)
      out.write(buf, 0, nread);

    out.flush();
    in.close();
  }

  public void setParameter(String name, String filename, InputStream is) throws IOException
  {
    boundary();
    writeName(name);
    write("; filename=\"");
    write(filename);
    write('\"');
    newline();
    write("Content-Type: ");
    String type = URLConnection.guessContentTypeFromName(filename);
    if(type == null)
      type = "application/octet-stream";

    writeln(type);
    newline();
    pipe(is, this.os);
    newline();
  }

  public void setParameter(String name, File file) throws IOException
  {
    setParameter(name, file.getPath(), new FileInputStream(file));
  }

  public void setParameter(String name, Object object) throws IOException
  {
    if (object instanceof File) {
      setParameter(name, (File) object);
    } else {
      setParameter(name, object.toString());
    }

  }

  public void setParameters(Map<String, ?> parameters) throws IOException
  {
    if (parameters != null) {
      for (Entry<String, ?> e: parameters.entrySet())
        setParameter(e.getKey(), e.getValue());
    }
  }

  public void setParameters(Object[] parameters) throws IOException
  {
    if (parameters != null) {
      for (int i = 1; i < parameters.length; i += 2)
        setParameter(parameters[i - 1].toString(), parameters[i]);
    }
  }

  public InputStream post()
  {
    try {
      boundary();
      writeln("--");
      os.close();
      return connection.getInputStream();
    } catch (IOException var2) {
      return null;
    }
  }

  public InputStream post(Map<String, ?> parameters) throws IOException
  {
    setParameters(parameters);
    return post();
  }

  public InputStream post(Object[] parameters) throws IOException
  {
    setParameters(parameters);
    return post();
  }

  public InputStream post(Map<String, String> cookies, Map<String, ?> parameters) throws IOException
  {
    setCookies(cookies);
    return post(parameters);
  }

  public InputStream post(String[] cookies, Object[] parameters) throws IOException
  {
    setCookies(cookies);
    return post(parameters);
  }

  public InputStream post(String name, Object value) throws IOException
  {
    setParameter(name, value);
    return post();
  }

  public InputStream post(String name1, Object value1, String name2, Object value2) throws IOException
  {
    setParameter(name1, value1);
    return post(name2, value2);
  }

  public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException
  {
    setParameter(name1, value1);
    return post(name2, value2, name3, value3);
  }

  public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException
  {
    setParameter(name1, value1);
    return post(name2, value2, name3, value3, name4, value4);
  }

  public InputStream post(URL url, Map<String, ?> parameters) throws IOException
  {
    return (new ClientHttpRequest(url)).post(parameters);
  }

  public InputStream post(URL url, Object[] parameters) throws IOException
  {
    return (new ClientHttpRequest(url)).post(parameters);
  }

  public InputStream post(URL url, Map<String, String> cookies, Map<String, ?> parameters) throws IOException
  {
    return (new ClientHttpRequest(url)).post(cookies, parameters);
  }

  public InputStream post(URL url, String[] cookies, Object[] parameters) throws IOException
  {
    return (new ClientHttpRequest(url)).post(cookies, parameters);
  }

  public InputStream post(URL url, String name1, Object value1) throws IOException
  {
    return (new ClientHttpRequest(url)).post(name1, value1);
  }

  public InputStream post(URL url, String name1, Object value1, String name2, Object value2) throws IOException
  {
    return (new ClientHttpRequest(url)).post(name1, value1, name2, value2);
  }

  public InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException
  {
    return (new ClientHttpRequest(url)).post(name1, value1, name2, value2, name3, value3);
  }

  public InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException
  {
    return (new ClientHttpRequest(url)).post(name1, value1, name2, value2, name3, value3, name4, value4);
  }
}
