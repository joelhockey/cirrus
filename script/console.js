// put test classes on classpath
var addURL = java.net.URLClassLoader.__javaObject__.getDeclaredMethod("addURL", [java.net.URL]);
addURL.accessible = true;
var classpaths = ["target/test-classes", "target/classes"];
for each (var dir in ["lib/compile", "lib/runtime", "lib/test"]) {
    for each (var jar in new java.io.File(dir).list()) {
        classpaths.push(dir + "/" + jar);
    }
}
for each (var path in classpaths) {
    addURL.invoke(java.lang.ClassLoader.getSystemClassLoader(), [new java.io.File(path).toURL()]);
}

com.joelhockey.cirrus.Console.main(null);
