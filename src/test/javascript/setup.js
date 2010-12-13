(function() {
    // load JNDI
    var ic = new javax.naming.InitialContext();
    
    // check if ds exists
    var ds = ic.lookup("jdbc/cirrus");
    if (!ds) {
        // put datasource into JNDI
        var hsqldb = new org.hsqldb.jdbc.jdbcDataSource();
        hsqldb.setDatabase("jdbc:hsqldb:file:hsqldb/dev/cirrus");
        hsqldb.setUser("sa");
        ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(hsqldb);
        ic.rebind("jdbc/cirrus", ds);
    }
    
    DB = new com.joelhockey.cirrus.DB(ds);
})();
