package com.kapok.model;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.*;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

  private final Map<String, String> headerMap = new HashMap<>();
  private byte[] content;

  public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    ByteArrayOutputStream bodyInOutputStream = new ByteArrayOutputStream();
    copy(request.getInputStream(), bodyInOutputStream);
    content = bodyInOutputStream.toByteArray();
  }

  public static void copy(InputStream in, OutputStream out) throws IOException {

    byte[] buffer = new byte[1024];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) {
        break;
      }
      out.write(buffer, 0, bytesRead);
    }
  }

  /**
   * add a header with given name and value.
   *
   * @param name
   * @param value
   */
  public void addHeader(String name, String value) {
    headerMap.put(name, value);
  }

  @Override
  public String getHeader(String name) {
    String headerValue = super.getHeader(name);
    if (headerMap.containsKey(name)) {
      headerValue = headerMap.get(name);
    }
    return headerValue;
  }

  /**
   * get the header names.
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    List<String> names = Collections.list(super.getHeaderNames());
    for (String name : headerMap.keySet()) {
      names.add(name);
    }
    return Collections.enumeration(names);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = Collections.list(super.getHeaders(name));
    if (headerMap.containsKey(name)) {
      values.add(headerMap.get(name));
    }
    return Collections.enumeration(values);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }

      public int read() throws IOException {
        return byteArrayInputStream.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(this.getInputStream(), "utf-8"));
  }
}
