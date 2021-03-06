// add jetty to classpath
var addURL = java.net.URLClassLoader.__javaObject__.getDeclaredMethod("addURL", [java.net.URL]);
addURL.accessible = true;
var jettydir = new java.io.File("jetty");
for each (var file in jettydir.listFiles().concat(jettydir)) {
    addURL.invoke(java.lang.ClassLoader.getSystemClassLoader(), [file.toURL()]);
}
var tmpdir = new java.io.File(jettydir, "work");
tmpdir.mkdir();
print("Starting jetty on port 8080\nCtrl-C to exit");
var server = new org.mortbay.jetty.Server();
var connector=new org.mortbay.jetty.nio.SelectChannelConnector();
connector.setPort(8080);
server.setConnectors([connector]);
webapp = new org.mortbay.jetty.webapp.WebAppContext();
webapp.setContextPath("/");
webapp.setWar("src/test/webapp");
webapp.setTempDirectory(tmpdir);
var classLoader = new org.mortbay.jetty.webapp.WebAppClassLoader(webapp);
classLoader.addClassPath("target/test-classes");
classLoader.addClassPath("target/classes");
classLoader.addJars(org.mortbay.resource.Resource.newResource("lib/compile"));
classLoader.addJars(org.mortbay.resource.Resource.newResource("lib/runtime"));
webapp.setClassLoader(classLoader)
server.setHandler(webapp);

// logging
var requestLogHandler = new org.mortbay.jetty.handler.RequestLogHandler();
requestLogHandler.setRequestLog(new org.mortbay.jetty.NCSARequestLog("logs/jetty-yyyy_mm_dd.request.log"));
server.addHandler(requestLogHandler);

// jndi datasource
var ds = new org.hsqldb.jdbc.jdbcDataSource();
var dburl = "jdbc:hsqldb:file:hsqldb/cirrus";
print("setting db to: " + dburl)
ds.setDatabase(dburl);
ds.setUser("sa");
var c3p0 = com.mchange.v2.c3p0.DataSources.pooledDataSource(ds);
var resource = new org.mortbay.jetty.plus.naming.Resource("jdbc/cirrus", c3p0);

server.start();
if (java.lang.System.getProperty("debugjs")) {
    print("detected debug mode, using single-thread");
    server.getThreadPool().setMaxThreads(2); // useful when debugging
}
server.join();
