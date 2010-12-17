// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * Default ServletConfig implementation used for testing.
 */
public class MockServletConfig implements ServletConfig {
    private ServletContext context = new MockServletContext(this);
    private Map<String, String> params = new HashMap<String, String>();
    public Map<String, String> getInitParameters() { return params; }

    public String getInitParameter(String paramString) { return params.get(paramString); }
    public Enumeration getInitParameterNames() { return new Vector(params.keySet()).elements(); }
    public ServletContext getServletContext() { return context; }
    public String getServletName() { return "CirrusServlet"; }
}
