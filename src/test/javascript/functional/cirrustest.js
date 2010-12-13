CirrusTest = {
    setUp: function() {
        var ic = new javax.naming.InitialContext();
        var ds = new org.hsqldb.jdbc.jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:file:hsqldb/dev/cirrus");
        ds.setUser("sa");
        ic.bind("jdbc/cirrus", com.mchange.v2.c3p0.DataSources.pooledDataSource(ds));

        var sconf = new com.joelhockey.cirrus.MockServletConfig();
        sconf.initParameters.put("dbname", "jdbc/cirrus");
        sconf.initParameters.put("dbversion", "1");
        this.servlet = new com.joelhockey.cirrus.CirrusServlet();
        this.servlet.init(sconf);
    },

    testIndex: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("GET", "/");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(302, res.status);
        assertEquals("/login", res.redirect);
    },
    
    testFavicon: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("GET", "/favicon.ico");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(200, res.status);
        assertEquals("image/x-icon", res.getContentType());

        req = new com.joelhockey.cirrus.MockHttpServletRequest("GET", "/favicon.ico");
        req.headers["If-Modified-Since"] = res.headers["Last-Modified"];
        res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(304, res.status);
    },
    
    testHello: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("GET", "/test/hello");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(200, res.status);
        assertEquals("text/html", res.getContentType());
        var html = res.getResponse();
        assertMatches(/This is start of layout/, html);
        assertMatches(/body of hello page/, html);
        assertMatches(/_form partial _form partial _recurse partial/, html);
    }
}
