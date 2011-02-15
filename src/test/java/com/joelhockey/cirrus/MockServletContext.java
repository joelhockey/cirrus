// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.MimeTypes;

/**
 * Default implementation of ServletContext for testing.
 */
public class MockServletContext implements ServletContext {
    private static final Log log = LogFactory.getLog(MockServletContext.class);
    private static final MimeTypes MIME_TYPES = new MimeTypes();

    private Map<String, Object> attribs = new HashMap<String, Object>();
    private MockServletConfig config;

    public MockServletContext(MockServletConfig config) {
        this.config = config;
        File servletTemp = new File("target/servlet-temp");
        servletTemp.mkdir();
        attribs.put("javax.servlet.context.tempdir", servletTemp);
    }
    public Map<String, Object> getAttributes() { return attribs; }

    public Object getAttribute(String key) { return attribs.get(key); }
    public Enumeration getAttributeNames() { return new Vector(attribs.keySet()).elements(); }
    public ServletContext getContext(String path) { return this; }
    public String getInitParameter(String key) { return config.getInitParameter(key); }
    public Enumeration getInitParameterNames() { return config.getInitParameterNames(); }
    public int getMajorVersion() { return 2; }
    public String getMimeType(String filename) { return MIME_TYPES.getMimeByExtension(filename).toString(); }
    public int getMinorVersion() { return 5; }
    public RequestDispatcher getNamedDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public String getRealPath(String path) {
        try {
            return new File(getResource(path).toURI()).getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
    public RequestDispatcher getRequestDispatcher(String arg0) { throw new UnsupportedOperationException(); }
    public URL getResource(String path) throws MalformedURLException {
        File f = new File("src/test/webapp" + path);
        return f.exists() ? f.toURL() : null;
    }
    public InputStream getResourceAsStream(String path) {
        try {
            return getResource(path).openStream();
        } catch (Exception e) {
            return null;
        }
    }
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
    public String getServerInfo() { return "mock server v1.0"; }
    public Servlet getServlet(String arg0) throws ServletException { throw new UnsupportedOperationException(); }
    public String getServletContextName() { return "cirrus"; }
    public Enumeration getServletNames() { return new Vector().elements(); }
    public Enumeration getServlets() { return new Vector().elements(); }
    public void log(String msg) { log.info(msg); }
    public void log(Exception error, String msg) { log.error(msg, error); }
    public void log(String msg, Throwable error) { log.error(msg, error); }
    public void removeAttribute(String key) { attribs.remove(key); }
    public void setAttribute(String key, Object value) { attribs.put(key, value); }
    public String getContextPath() { return "/"; }
}
