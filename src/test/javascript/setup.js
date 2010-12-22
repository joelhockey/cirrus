// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// set wrap factory for current context
org.mozilla.javascript.Context.enter().setWrapFactory(
        com.joelhockey.cirrus.Cirrus.WRAP_FACTORY);
org.mozilla.javascript.Context.exit();

// add JSON
var JSON = JSON || new com.joelhockey.cirrus.RhinoJSON();

// create global cirrus object if not exists
if (typeof cirrus === "undefined") {
    var cirrus = new com.joelhockey.cirrus.Cirrus(
            this, new com.joelhockey.cirrus.MockServletConfig());
    cirrus.log("setting up cirrus with test objects");
    
    // put hsqldb/c3p0 datasource into cirrus and JNDI (if not already there)
    (function() {
        var ic = new javax.naming.InitialContext();
        var ds = ic.lookup("jdbc/cirrus");
        
        // add to JNDI if not already exists
        if (!ds) {
            var hsqldb = new org.hsqldb.jdbc.jdbcDataSource();
            var dburl = "jdbc:hsqldb:mem:cirrus";
            cirrus.log("setting db to: " + dburl);
            hsqldb.setDatabase(dburl);
            hsqldb.setUser("sa");
            ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(hsqldb);
            ic.bind("jdbc/cirrus", ds);
        }

        // add 'dataSource' property to cirus
        cirrus.dataSource = ds;
    })();
    
    cirrus.load("/app/cirrus.js"); // load javascript-defined cirrus
    cirrus.migrate(); // no version means we will migrate all available

    // convenience to test HTTP request
    cirrus.test = function(requestLine, headers, params, body) {
        methodPath = requestLine.split(" ");
        var request = new com.joelhockey.cirrus.MockHttpServletRequest(methodPath[0], methodPath[1]);
        for (var name in headers) {
            request.headers[name] = headers[name];
        }
        for (var name in params) {
            request.params[name] = params[name];
        }
        var response = new com.joelhockey.cirrus.MockHttpServletResponse();
        cirrus.service(request, response);
        return response;
    }
}
