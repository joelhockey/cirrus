// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

IndexControllerTest = {
    setUp: function() {
        load("/setup.js");
    },

    testGet: function() {
        var response = cirrus.test("GET /");
        assertEquals(302, response.status);
        assertEquals("/login", response.redirect);
    },

    testPost: function() {
        var response = cirrus.test("POST /");
        assertEquals(405, response.status);
        assertEquals("GET", response.headers["Allow"]);
    }    
}
