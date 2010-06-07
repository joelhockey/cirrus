package com.joelhockey.cirrus;

import junit.framework.TestCase;

public class CirrusTest extends TestCase {
    private CirrusServlet servlet;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;
    private MockServletConfig sconf;

    public void setUp() throws Exception {
        sconf = new MockServletConfig();
        servlet = new CirrusServlet();
        servlet.init(sconf);
    }

    public void testIndex() throws Exception {
        req = new MockHttpServletRequest("/");
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        assertEquals(200, res.status);

        req = new MockHttpServletRequest("/");
        req.headers.put("If-Modified-Since", res.headers.get("Last-Modified"));
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        assertEquals(304, res.status);
    }

    public void testTestHello() throws Exception {
        req = new MockHttpServletRequest("/test/hello");
        res = new MockHttpServletResponse();
        servlet.service(req, res);
        String response = res.getResponse();
        assertTrue("Expected res: 'Hello...', got: [" + response + "]", response.contains("hello page"));

//        System.out.println(res.getResponse());
    }
}
