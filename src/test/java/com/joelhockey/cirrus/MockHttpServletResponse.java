package com.joelhockey.cirrus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {
    public ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private ServletOutputStream sos = new MockServletOutputStream(baos);
    public PrintWriter pw = new PrintWriter(baos);

    private String contentType;

    public void addCookie(Cookie arg0) { throw new UnsupportedOperationException(); }
    public void addDateHeader(String arg0, long arg1) { throw new UnsupportedOperationException(); }
    public void addHeader(String arg0, String arg1) { throw new UnsupportedOperationException(); }
    public void addIntHeader(String arg0, int arg1) { throw new UnsupportedOperationException(); }
    public boolean containsHeader(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeRedirectURL(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeRedirectUrl(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeURL(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeUrl(String arg0) { throw new UnsupportedOperationException(); }
    public void sendError(int arg0) throws IOException { throw new UnsupportedOperationException(); }
    public void sendError(int arg0, String arg1) throws IOException { throw new UnsupportedOperationException(); }
    public void sendRedirect(String arg0) throws IOException { throw new UnsupportedOperationException(); }
    public void setDateHeader(String arg0, long arg1) { throw new UnsupportedOperationException(); }
    public void setHeader(String arg0, String arg1) { throw new UnsupportedOperationException(); }
    public void setIntHeader(String arg0, int arg1) { throw new UnsupportedOperationException(); }
    public void setStatus(int arg0) { throw new UnsupportedOperationException(); }
    public void setStatus(int arg0, String arg1) { throw new UnsupportedOperationException(); }
    public void flushBuffer() throws IOException { throw new UnsupportedOperationException(); }
    public int getBufferSize() { throw new UnsupportedOperationException(); }
    public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
    public String getContentType() { throw new UnsupportedOperationException(); }
    public Locale getLocale() { throw new UnsupportedOperationException(); }
    public ServletOutputStream getOutputStream() throws IOException { return sos; }
    public PrintWriter getWriter() throws IOException { return pw; }
    public boolean isCommitted() { throw new UnsupportedOperationException(); }
    public void reset() { baos.reset(); }
    public void resetBuffer() { throw new UnsupportedOperationException(); }
    public void setBufferSize(int arg0) { throw new UnsupportedOperationException(); }
    public void setCharacterEncoding(String arg0) { throw new UnsupportedOperationException(); }
    public void setContentLength(int arg0) { throw new UnsupportedOperationException(); }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setLocale(Locale arg0) { throw new UnsupportedOperationException(); }
}
