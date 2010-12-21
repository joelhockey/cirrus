// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// create global cirrus object if not exists
var cirrus = cirrus || new com.joelhockey.cirrus.Cirrus(
        this, new com.joelhockey.cirrus.MockServletConfig());

cirrus.load("/app/cirrus.js");

// set wrap factory for current context
org.mozilla.javascript.Context.enter().setWrapFactory(
        com.joelhockey.cirrus.Cirrus.WRAP_FACTORY);
org.mozilla.javascript.Context.exit();

(function() {
    // put hsqldb/c3p0 datasource into JNDI if not already there
    var ic = new javax.naming.InitialContext();
    var ds = ic.lookup("jdbc/cirrus");
    if (!ds) {
        var hsqldb = new org.hsqldb.jdbc.jdbcDataSource();
        var dburl = "jdbc:hsqldb:file:hsqldb/dev/cirrus";
        cirrus.log("setting db to: " + dburl);
        hsqldb.setDatabase(dburl);
        hsqldb.setUser("sa");
        ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(hsqldb);
        ic.bind("jdbc/cirrus", ds);
    }
    // add 'dataSource' property to cirus
    cirrus.dataSource = ds;
})();

var setup = setup || {
    servlet: function(method, path) {
        var servlet = new com.joelhockey.cirrus.CirrusServlet();
        var sconf = new com.joelhockey.cirrus.MockServletConfig();
        sconf.initParameters.dbname = "jdbc/cirrus";
        sconf.initParameters.dbversion = "1";
        servlet.init(sconf);
        return servlet;
    }
}