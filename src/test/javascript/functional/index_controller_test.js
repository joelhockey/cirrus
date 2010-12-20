// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

IndexControllerTest = {
    setUp: function() {
        load("/setup.js");
        this.servlet = setup.servlet();
    },

    testGet: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("GET", "/");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(302, res.status);
        assertEquals("/login", res.redirect);
    },

    testPost: function() {
        var req = new com.joelhockey.cirrus.MockHttpServletRequest("POST", "/");
        var res = new com.joelhockey.cirrus.MockHttpServletResponse();
        this.servlet.service(req, res);
        assertEquals(405, res.status);
        assertEquals("GET", res.headers["Allow"]);
    }    
}
