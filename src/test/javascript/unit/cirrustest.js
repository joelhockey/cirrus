// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

CirrusTest = {
    setUp: function() {
        load("/setup.js");
    },
    
    testOptionalParams: function() {
        var env = {
            method: "GET",
            path: "/test/action1",
        };

        cirrus.forward(env);
        assertEquals("GET /test/action1", env.body);
        
        cirrus.forward(env, "POST");
        assertEquals("POST /test/action1", env.body);
        
        cirrus.forward(env, "GET", "/test/action2");
        assertEquals("GET /test/action2", env.body);
    },
    
}
