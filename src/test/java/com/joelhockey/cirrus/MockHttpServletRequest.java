// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

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
    public String method;
    public Map<String, String> headers = new HashMap<String, String>();
    public String requestURI;
    public MockHttpSession session = new MockHttpSession();

    private SimpleDateFormat dateHeaderFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    {
    	dateHeaderFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public MockHttpServletRequest(String method, String requestURI) {
        this.method = method;
        this.requestURI = requestURI;
    }
    public String getAuthType() { return null; }
    public String getContextPath() { return ""; }
    public Cookie[] getCookies() { return null; }
    public long getDateHeader(String header) {
    	String value = headers.get(header);
    	try {
    		return value != null ? dateHeaderFormat.parse(value).getTime() : -1;
		} catch (ParseException pe) {
			return -1;
		}
    }
    public String getHeader(String name) { throw new UnsupportedOperationException(); }
    public Enumeration getHeaderNames() { return new Vector().elements(); }
    public Enumeration getHeaders(String name) { throw new UnsupportedOperationException(); }
    public int getIntHeader(String arg0) { throw new UnsupportedOperationException(); }
    public String getMethod() { return method; }
    public String getPathInfo() { return null; }
    public String getPathTranslated() { return null; }
    public String getQueryString() { return null; }
    public String getRemoteUser() { return null; }
    public String getRequestURI() { return requestURI; }
    public StringBuffer getRequestURL() { return new StringBuffer("http://localhost" + getRequestURI()); }
    public String getRequestedSessionId() { return null; }
    public String getServletPath() { return "/*"; }
    public HttpSession getSession() { return session; }
    public HttpSession getSession(boolean create) { throw new UnsupportedOperationException(); }
    public Principal getUserPrincipal() { return null; }
    public boolean isRequestedSessionIdFromCookie() { return true; }
    public boolean isRequestedSessionIdFromURL() { return false; }
    public boolean isRequestedSessionIdFromUrl() { return false; }
    public boolean isRequestedSessionIdValid() { return true; }
    public boolean isUserInRole(String arg0) { throw new UnsupportedOperationException(); }
    public Object getAttribute(String key) { return attribs.get(key); }
    public Enumeration getAttributeNames() { return new Vector().elements(); }
    public String getCharacterEncoding() { return "UTF-8"; }
    public int getContentLength() { return 0; }
    public String getContentType() { return "text/plain"; }
    public ServletInputStream getInputStream() throws IOException { return null; }
    public String getLocalAddr() { return "127.0.0.1"; }
    public String getLocalName() { return "localhost"; }
    public int getLocalPort() { return 80; }
    public Locale getLocale() { return Locale.ENGLISH; }
    public Enumeration getLocales() { return new Vector().elements(); }
    public String getParameter(String key) { return params.get(key); }
    public Map getParameterMap() { return params; }
    public Enumeration getParameterNames() { return new Vector(params.keySet()).elements(); }
    public String[] getParameterValues(String arg0) { throw new UnsupportedOperationException(); }
    public String getProtocol() { return "HTTP/1.1"; }
    public BufferedReader getReader() throws IOException { return null; }
    public String getRealPath(String arg0) { throw new UnsupportedOperationException(); }
    public String getRemoteAddr() { return "127.0.0.1"; }
    public String getRemoteHost() { return "localhost"; }
    public int getRemotePort() { return -1; }
    public RequestDispatcher getRequestDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public String getScheme() { return "http"; }
    public String getServerName() { return "localhost"; }
    public int getServerPort() { return 80; }
    public boolean isSecure() { return false; }
    public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
    public void setAttribute(String name, Object value) { attribs.put(name, value); }
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException { throw new UnsupportedOperationException(); }
}
