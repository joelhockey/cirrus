(function() {
    // load JNDI
    var ic = new javax.naming.InitialContext();
    
    // check if ds exists
    if (!ic.lookup("jdbc/cirrus")) {
        // put datasource into JNDI
        var ds = new org.hsqldb.jdbc.jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:file:hsqldb/dev/cirrus");
        ds.setUser("sa");
        var c3p0 = com.mchange.v2.c3p0.DataSources.pooledDataSource(ds);
        ic.rebind("jdbc/cirrus", c3p0);
    }
})();
