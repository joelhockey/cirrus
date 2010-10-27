// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.hsqldb.jdbc.jdbcDataSource;

import com.mchange.v2.c3p0.DataSources;

public class CirrusTest extends TestCase {
    private CirrusServlet servlet;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;
    private MockServletConfig sconf;

    public void setUp() throws Exception {
//System.setProperty("debugjs", "true");
        InitialContext ic = new InitialContext();
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:file:hsqldb/cirrus");
        ds.setUser("sa");
        ic.bind("jdbc/cirrus", DataSources.pooledDataSource(ds));

        sconf = new MockServletConfig();
        sconf.params.put("dbname", "jdbc/cirrus");
        servlet = new CirrusServlet();
        servlet.init(sconf);
    }

    public void testIndex() throws Exception {
        req = new MockHttpServletRequest("GET", "/");
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        assertEquals(200, res.status);

        req = new MockHttpServletRequest("GET", "/");
        req.headers.put("If-Modified-Since", res.headers.get("Last-Modified"));
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        assertEquals(304, res.status);
    }

    public void testTestHello() throws Exception {
        req = new MockHttpServletRequest("GET", "/test/hello");
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        String response = res.getResponse();
        assertTrue("Expected res: 'Hello...', got: [" + response + "]", response.contains("hello page"));
    }
}
