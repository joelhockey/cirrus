package com.joelhockey.cirrus;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {
    public ServletContext context = new MockServletContext();
    public Map<String, String> params = new HashMap<String, String>();

    public String getInitParameter(String paramString) { return params.get(paramString); }
    public Enumeration getInitParameterNames() { return new Vector(params.keySet()).elements(); }
    public ServletContext getServletContext() { return context; }
    public String getServletName() { return "MockServlet"; }
}
