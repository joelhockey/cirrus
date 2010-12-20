// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

PublicControllerTest = {
    setUp: function() {
        load("/setup.js");
        this.servlet = setup.servlet();
    },

    testGetFavicon: function() {
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
    
    testPostFavicon: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("POST", "/favicon.ico");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(405, res.status);
        assertEquals("GET", res.headers["Allow"]);
    }    
}
