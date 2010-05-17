package com.joelhockey.cirrus;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MockServletContext implements ServletContext {
    public Map<String, String> attribs = new HashMap<String, String>();

    public Object getAttribute(String key) { return attribs.get(key); }
    public Enumeration getAttributeNames() { throw new UnsupportedOperationException(); }
    public ServletContext getContext(String arg0) { throw new UnsupportedOperationException(); }
    public String getInitParameter(String arg0) { throw new UnsupportedOperationException(); }
    public Enumeration getInitParameterNames() { throw new UnsupportedOperationException(); }
    public int getMajorVersion() { throw new UnsupportedOperationException(); }
    public String getMimeType(String arg0) { throw new UnsupportedOperationException(); }
    public int getMinorVersion() { throw new UnsupportedOperationException(); }
    public RequestDispatcher getNamedDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public String getRealPath(String path) {
        if (path.charAt(0) != '/') {
            path = "/" + path;
        }
System.out.println("looking up: " + path);
        URL url = MockServletContext.class.getResource(path);
        if (url == null) {
            throw new NullPointerException("could not find real (or any) path for: [" + path + "]");
        }
        try {
            return new File(url.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public RequestDispatcher getRequestDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public URL getResource(String arg0) throws MalformedURLException { throw new UnsupportedOperationException(); }
    public InputStream getResourceAsStream(String arg0) { throw new UnsupportedOperationException(); }
    public Set getResourcePaths(String arg0) { throw new UnsupportedOperationException(); }
    public String getServerInfo() { throw new UnsupportedOperationException(); }
    public Servlet getServlet(String arg0) throws ServletException { throw new UnsupportedOperationException(); }
    public String getServletContextName() { throw new UnsupportedOperationException(); }
    public Enumeration getServletNames() { throw new UnsupportedOperationException(); }
    public Enumeration getServlets() { throw new UnsupportedOperationException(); }
    public void log(String msg) {}
    public void log(Exception arg0, String arg1) { throw new UnsupportedOperationException(); }
    public void log(String arg0, Throwable arg1) { throw new UnsupportedOperationException(); }
    public void removeAttribute(String arg0) { throw new UnsupportedOperationException(); }
    public void setAttribute(String arg0, Object arg1) { throw new UnsupportedOperationException(); }
}
