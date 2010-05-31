package com.joelhockey.cirrus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class MockHttpServletRequest implements HttpServletRequest {
    public Map<String, String> params = new HashMap<String, String>() {{
        put("param", "param");
        put("hex", "0123456789abcdef");
    }};
    public Map<String, Object> attribs = new HashMap<String, Object>();
    public String method = "GET";
    public Map<String, String> headers = new HashMap<String, String>();
    public String path;
    private SimpleDateFormat dateHeaderFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    {
    	dateHeaderFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public MockHttpServletRequest(String path) {
        attribs.put("javax.servlet.include.servlet_path", path);
        this.path = path;
    }
    public String getAuthType() { throw new UnsupportedOperationException(); }
    public String getContextPath() { throw new UnsupportedOperationException(); }
    public Cookie[] getCookies() { throw new UnsupportedOperationException(); }
    public long getDateHeader(String header) {
    	String value = headers.get(header);
    	try {
    		return value != null ? dateHeaderFormat.parse(value).getTime() : -1;
		} catch (ParseException pe) {
			return -1;
		}
    }
    public String getHeader(String arg0) { throw new UnsupportedOperationException(); }
    public Enumeration getHeaderNames() { throw new UnsupportedOperationException(); }
    public Enumeration getHeaders(String arg0) { throw new UnsupportedOperationException(); }
    public int getIntHeader(String arg0) { throw new UnsupportedOperationException(); }
    public String getMethod() { return method; }
    public String getPathInfo() { throw new UnsupportedOperationException(); }
    public String getPathTranslated() { throw new UnsupportedOperationException(); }
    public String getQueryString() { throw new UnsupportedOperationException(); }
    public String getRemoteUser() { throw new UnsupportedOperationException(); }
    public String getRequestURI() { throw new UnsupportedOperationException(); }
    public StringBuffer getRequestURL() { throw new UnsupportedOperationException(); }
    public String getRequestedSessionId() { throw new UnsupportedOperationException(); }
    public String getServletPath() { return path; }
    public HttpSession getSession() { throw new UnsupportedOperationException(); }
    public HttpSession getSession(boolean arg0) { throw new UnsupportedOperationException(); }
    public Principal getUserPrincipal() { throw new UnsupportedOperationException(); }
    public boolean isRequestedSessionIdFromCookie() { throw new UnsupportedOperationException(); }
    public boolean isRequestedSessionIdFromURL() { throw new UnsupportedOperationException(); }
    public boolean isRequestedSessionIdFromUrl() { throw new UnsupportedOperationException(); }
    public boolean isRequestedSessionIdValid() { throw new UnsupportedOperationException(); }
    public boolean isUserInRole(String arg0) { throw new UnsupportedOperationException(); }
    public Object getAttribute(String key) { return attribs.get(key); }
    public Enumeration getAttributeNames() { throw new UnsupportedOperationException(); }
    public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
    public int getContentLength() { throw new UnsupportedOperationException(); }
    public String getContentType() { throw new UnsupportedOperationException(); }
    public ServletInputStream getInputStream() throws IOException { throw new UnsupportedOperationException(); }
    public String getLocalAddr() { throw new UnsupportedOperationException(); }
    public String getLocalName() { throw new UnsupportedOperationException(); }
    public int getLocalPort() { throw new UnsupportedOperationException(); }
    public Locale getLocale() { throw new UnsupportedOperationException(); }
    public Enumeration getLocales() { throw new UnsupportedOperationException(); }
    public String getParameter(String key) { return params.get(key); }
    public Map getParameterMap() { return params; }
    public Enumeration getParameterNames() { return new Vector(params.keySet()).elements(); }
    public String[] getParameterValues(String arg0) { throw new UnsupportedOperationException(); }
    public String getProtocol() { throw new UnsupportedOperationException(); }
    public BufferedReader getReader() throws IOException { throw new UnsupportedOperationException(); }
    public String getRealPath(String arg0) { throw new UnsupportedOperationException(); }
    public String getRemoteAddr() { throw new UnsupportedOperationException(); }
    public String getRemoteHost() { throw new UnsupportedOperationException(); }
    public int getRemotePort() { throw new UnsupportedOperationException(); }
    public RequestDispatcher getRequestDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public String getScheme() { throw new UnsupportedOperationException(); }
    public String getServerName() { throw new UnsupportedOperationException(); }
    public int getServerPort() { throw new UnsupportedOperationException(); }
    public boolean isSecure() { throw new UnsupportedOperationException(); }
    public void removeAttribute(String arg0) { throw new UnsupportedOperationException(); }
    public void setAttribute(String key, Object value) { attribs.put(key, value); }
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException { throw new UnsupportedOperationException(); }
}
