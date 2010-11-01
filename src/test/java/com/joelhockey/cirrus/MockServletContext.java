package com.joelhockey.cirrus;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.mortbay.jetty.MimeTypes;

public class MockServletContext implements ServletContext {
    private static MimeTypes MIME_TYPES = new MimeTypes();

    public Map<String, Object> attribs = new HashMap<String, Object>();
    {
        File servletTemp = new File("target/servlet-temp");
        servletTemp.mkdir();
        attribs.put("javax.servlet.context.tempdir", servletTemp);
    }

    public Object getAttribute(String key) { return attribs.get(key); }
    public Enumeration getAttributeNames() { throw new UnsupportedOperationException(); }
    public ServletContext getContext(String arg0) { throw new UnsupportedOperationException(); }
    public String getInitParameter(String arg0) { throw new UnsupportedOperationException(); }
    public Enumeration getInitParameterNames() { throw new UnsupportedOperationException(); }
    public int getMajorVersion() { throw new UnsupportedOperationException(); }
    public String getMimeType(String filename) {
        return MIME_TYPES.getMimeByExtension(filename).toString();
    }
    public int getMinorVersion() { throw new UnsupportedOperationException(); }
    public RequestDispatcher getNamedDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public String getRealPath(String path) { throw new UnsupportedOperationException(); }
    public RequestDispatcher getRequestDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public URL getResource(String path) throws MalformedURLException {
        File f = new File("src/test/webapp" + path);
        return f.exists() ? f.toURL() : null;
    }
    public InputStream getResourceAsStream(String arg0) { throw new UnsupportedOperationException(); }
    public Set getResourcePaths(String path) {
        File dir = new File("src/test/webapp" + path);
        if (!dir.isDirectory()) {
            return null;
        }
        Set<String> result = new HashSet<String>();
        for (String f : dir.list()) {
            result.add(path + f);
        }
        return result;
    }
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
    public String getContextPath() { throw new UnsupportedOperationException(); }
}
