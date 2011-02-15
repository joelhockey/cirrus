// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// setup JNDI datasource and cirrus
(function(global) {
    var dburl = "jdbc:hsqldb:mem:cirrus";
    var dbname = "jdbc/cirrus";
    var ic = new javax.naming.InitialContext();
    var ds = ic.lookup(dbname);
    
    // put hsqldb/c3p0 datasource into JNDI (if not already there)
    if (!ds) {
        var hsqldb = new org.hsqldb.jdbc.jdbcDataSource();
        hsqldb.setDatabase(dburl);
        hsqldb.setUser("sa");
        ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(hsqldb);
        ic.bind("jdbc/cirrus", ds);
    }

    // create global cirrus object if not exists
    if (!global.cirrus) {
        var cirrus = new com.joelhockey.cirrus.Cirrus(
                global, new com.joelhockey.cirrus.MockServletConfig(), ds);
        cirrus.log("setting db to: " + dburl)
        cirrus.log("setting up cirrus with test objects");
        
        // no version means we will migrate all available
        cirrus.migrate(); 

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
        global.cirrus = cirrus;
    }
})(this);
