package com.joelhockey.cirrus;

import junit.framework.TestCase;

public class CirrusTest extends TestCase {
    private CirrusServlet servlet;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;
    private MockServletConfig sconf;

    public void setUp() throws Exception {
        req = new MockHttpServletRequest("/");
        res = new MockHttpServletResponse();
        sconf = new MockServletConfig();
        servlet = new CirrusServlet();
        servlet.init(sconf);
    }

    public void testIndex() throws Exception {
        req.path = "/";
        servlet.service(req, res);
    }

    public void testTestHello() throws Exception {
        req.path = "/test/hello";
        servlet.service(req, res);
//        System.out.println(res.getResponse());
    }
}
