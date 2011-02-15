cirrus.controllers.cirrustest = {
    before: function() {
        if (this.path.match(/before/)) {
            this.body.push("before");
        }
    },
    after: function() {
        if (this.path.match(/after/)) {
            this.body.push("after");
        }
    },
    GET: {
        $: function() {
            this.body.push("$");
        },
        action1: function() {
            return "GET /cirrustest/action1";
        },
        action2: function() {
            return "GET /cirrustest/action2";
        },
        status_servlet: function() {
            this.response.setStatus(201);
        },
        status_env: function() {
            this.status = 202;
        },
        status_result: function() {
            return {status: 203};
        },
        status_env_result: function() {
            this.status = 202;
            return {status: 203};
        },
        status_servlet_env: function() {
            this.response.setStatus(201);
            this.status = 202;
        },
        status_servlet_result: function() {
            this.response.setStatus(201);
            return {status: 203};
        },
        status_servlet_env_result: function() {
            this.response.setStatus(201);
            this.status = 202;
            return {status: 203};
        },
        headers_servlet: function() {
            this.response.addHeader("servlet", "servlet");
        },
        headers_env: function() {
            this.headers.env = "env";
        },
        headers_result: function() {
            return {headers: {result: "result"}};
        },
        headers_env_result: function() {
            this.headers.common = "env";
            return {headers: {common: "result"}};
        },
        headers_servlet_env: function() {
            this.response.addHeader("common", "servlet");
            this.headers.common = "env";
        },
        headers_servlet_result: function() {
            this.response.addHeader("common", "servlet");
            return {headers: {common: "result"}}
        },
        headers_servlet_env_result: function() {
            this.response.addHeader("common", "servlet");
            this.headers.common = "servlet";
            return {headers: {common: "result"}}
        },
        body_servlet: function() {
            this.response.getWriter().write("servlet.");
        },
        body_env: function() {
            this.body.push("env.");
        },
        body_result_str: function() {
            return "result.";
        },
        body_result_array: function() {
            return {body: ["result1.", "result2."]};
        },
        body_result_notarray: function() {
            return {body: "result."};
        },
        body_env_result: function() {
            this.body.push("env.");
            return "result.";
        },
        body_servlet_env: function() {
            this.response.getWriter().write("servlet.");
            this.body.push("env.");
        },
        body_servlet_result: function() {
            this.response.getWriter().write("servlet.");
            return "result.";
        },
        body_servlet_env_result: function() {
            this.response.getWriter().write("servlet.");
            this.body.push("env.");
            return "result.";
        },
    },
    POST: {
        action1: function() {
            return "POST /cirrustest/action1";
        },
    }
    
};
