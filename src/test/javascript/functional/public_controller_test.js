// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

PublicControllerTest = {
    setUp: function() {
        load("/setup.js");
    },

    testGetFavicon: function() {
        var response = cirrus.test("GET /favicon.ico")
        assertEquals(200, response.status);
        assertEquals("image/x-icon", response.contentType);

        response = cirrus.test("GET /favicon.ico", {"If-Modified-Since": response.headers["Last-Modified"]});
        assertEquals(304, response.status);
    },
    
    testPostFavicon: function() {
        var response = cirrus.test("POST /favicon.ico");
        assertEquals(405, response.status);
        assertEquals("GET", response.headers["Allow"]);
    }    
}
