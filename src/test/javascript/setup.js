// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// create global cirrus object if not exists
var cirrus = cirrus || new com.joelhockey.cirrus.Cirrus(
        this, new com.joelhockey.cirrus.MockServletConfig());

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
        hsqldb.setDatabase("jdbc:hsqldb:file:hsqldb/dev/cirrus");
        hsqldb.setUser("sa");
        ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(hsqldb);
        ic.bind("jdbc/cirrus", ds);
    }
})();
