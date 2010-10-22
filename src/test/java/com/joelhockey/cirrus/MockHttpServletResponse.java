package com.joelhockey.cirrus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {
    public ByteArrayOutputStream baos = new ByteArrayOutputStream();
    public ServletOutputStream sos = new MockServletOutputStream(baos);
    public PrintWriter pw = new PrintWriter(baos);
    public Map<String, String> headers = new HashMap<String, String>();
    public int status = 200;
    public String redirect;
    private SimpleDateFormat dateHeaderFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    {
    	dateHeaderFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    private String contentType;
    public String getResponse() {
        pw.flush();
        return new String(baos.toByteArray());
    }

    public void addCookie(Cookie arg0) { throw new UnsupportedOperationException(); }
    public void addDateHeader(String header, long value) { headers.put(header, dateHeaderFormat.format(new Date(value))); }
    public void addHeader(String header, String value) { headers.put(header, value); }
    public void addIntHeader(String header, int value) { headers.put(header, "" + value); }
    public boolean containsHeader(String header) { return headers.containsKey(header); }
    public String encodeRedirectURL(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeRedirectUrl(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeURL(String arg0) { throw new UnsupportedOperationException(); }
    public String encodeUrl(String arg0) { throw new UnsupportedOperationException(); }
    public void sendError(int arg0) throws IOException { throw new UnsupportedOperationException(); }
    public void sendError(int arg0, String arg1) throws IOException { throw new UnsupportedOperationException(); }
    public void sendRedirect(String redirect) throws IOException {
        this.redirect = redirect;
    }
    public void setDateHeader(String header, long value) {
    	headers.put(header, dateHeaderFormat.format(new Date(value)));
    }
    public void setHeader(String header, String value) { headers.put(header, value); }
    public void setIntHeader(String header, int value) { headers.put(header, "" + value); }
    public void setStatus(int status) { this.status = status; }
    public void setStatus(int status, String s) { this.status = status; }
    public void flushBuffer() throws IOException { throw new UnsupportedOperationException(); }
    public int getBufferSize() { throw new UnsupportedOperationException(); }
    public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
    public String getContentType() { throw new UnsupportedOperationException(); }
    public Locale getLocale() { throw new UnsupportedOperationException(); }
    public ServletOutputStream getOutputStream() throws IOException { return sos; }
    public PrintWriter getWriter() throws IOException { return pw; }
    public boolean isCommitted() { throw new UnsupportedOperationException(); }
    public void reset() {
    	baos.reset();
    	headers.clear();
    	status = 200;
    }
    public void resetBuffer() { throw new UnsupportedOperationException(); }
    public void setBufferSize(int arg0) { throw new UnsupportedOperationException(); }
    public void setCharacterEncoding(String arg0) { throw new UnsupportedOperationException(); }
    public void setContentLength(int arg0) { throw new UnsupportedOperationException(); }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setLocale(Locale arg0) { throw new UnsupportedOperationException(); }
}
