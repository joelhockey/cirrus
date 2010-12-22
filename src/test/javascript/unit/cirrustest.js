// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

CirrusTest = {
    setUp: function() {
        load("/setup.js");
    },
    
    testService: function() {
        // status
        this.assertResponse("/cirrustest/status_servlet", 201, {}, "");
        this.assertResponse("/cirrustest/status_env", 202, {}, "");
        this.assertResponse("/cirrustest/status_result", 203, {}, "");
        this.assertResponse("/cirrustest/status_env_result", 203, {}, "");
        this.assertResponse("/cirrustest/status_servlet_env", 202, {}, "");
        this.assertResponse("/cirrustest/status_servlet_result", 203, {}, "");
        this.assertResponse("/cirrustest/status_servlet_env_result", 203, {}, "");
        
        // headers
        this.assertResponse("/cirrustest/headers_servlet", 200, {servlet: "servlet"}, "");
        this.assertResponse("/cirrustest/headers_env", 200, {env: "env"}, "");
        this.assertResponse("/cirrustest/headers_result", 200, {result: "result"}, "");
        this.assertResponse("/cirrustest/headers_env_result", 200, {common: "result"}, "");
        this.assertResponse("/cirrustest/headers_servlet_env", 200, {common: "env"}, "");
        this.assertResponse("/cirrustest/headers_servlet_result", 200, {common: "result"}, "");
        this.assertResponse("/cirrustest/headers_servlet_env_result", 200, {common: "result"}, "");
        
        // body
        this.assertResponse("/cirrustest/body_servlet", 200, {}, "servlet.");
        this.assertResponse("/cirrustest/body_env", 200, {}, "env.");
        this.assertResponse("/cirrustest/body_result_str", 200, {}, "result.");
        this.assertResponse("/cirrustest/body_result_array", 200, {}, "result1.result2.");
        this.assertResponse("/cirrustest/body_result_notarray", 200, {}, "result.");
        this.assertResponse("/cirrustest/body_env_result", 200, {}, "env.result.");
        this.assertResponse("/cirrustest/body_servlet_env", 200, {}, "servlet.env.");
        this.assertResponse("/cirrustest/body_servlet_result", 200, {}, "servlet.result.");
        this.assertResponse("/cirrustest/body_servlet_env_result", 200, {}, "servlet.env.result.");
    },
    
    assertResponse: function(path, expectedStatus, expectedHeaders, expectedBody) {
        var request = new com.joelhockey.cirrus.MockHttpServletRequest("GET", path);
        var response = new com.joelhockey.cirrus.MockHttpServletResponse();
        
        cirrus.service(request, response);
        assertEquals(path + ": status", expectedStatus, response.getStatus());
        for (var name in expectedHeaders) {
            assertEquals(path + ": header: " + name, expectedHeaders[name], response.headers[name])
        }
        assertEquals(path + ": body", expectedBody, response.getResponse());
    },
    
    testForwardOptionalParams: function() {
        var env = {
            method: "GET",
            path: "/cirrustest/action1",
        };

        assertEquals("GET /cirrustest/action1", cirrus.forward(env));
        assertEquals("POST /cirrustest/action1", cirrus.forward(env, "POST"));
        assertEquals("GET /cirrustest/action2", cirrus.forward(env, "GET /cirrustest/action2"));
    },
    
    testForwardBeforeAfter: function() {
        var env = {method: "GET", path: "/cirrustest/before", body: []};
        cirrus.forward(env);
        assertEquals("before,$", env.body);
        
        env = {method: "GET", path: "/cirrustest/after", body: []};
        cirrus.forward(env);
        assertEquals("$,after", env.body);

        env = {method: "GET", path: "/cirrustest/before.after", body: []};
        cirrus.forward(env);
        assertEquals("before,$,after", env.body);
    }
}
