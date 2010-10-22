// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Rhino scope for CirrusServlet.  Based on {@link Global}.
 * Each servlet thread has its own instance of this class.
 * Provides helper methods such as load to load js libs.  Uses
 * caching to only reload files that have modified.
 * @author Joel Hockey
 */
public class CirrusScope extends ImporterTopLevel {
    private static final long serialVersionUID = 0xDC7C4EC5275394BL;
    private static final Log log = LogFactory.getLog(CirrusScope.class);

    /** Time to wait before reloading changed js file. */
    public static final long RELOAD_WAIT = 10000;

    // static maps hold all compiled scripts
    private static Map<String, CacheEntry<Script>> SCRIPT_CACHE = new HashMap<String, CacheEntry<Script>>();
    private static Map<String, CacheEntry<Object>> LAST_MOD_CACHE = new HashMap<String, CacheEntry<Object>>();

    // local maps get populated when compiled scripts are executed in current scope
    private Map<String, CacheEntry<Script>> localScriptCache = new HashMap<String, CacheEntry<Script>>();
    private Map<String, CacheEntry<NativeObject>> templateCache = new HashMap<String, CacheEntry<NativeObject>>();
    private ServletConfig sconf;

    /**
     * Create CirrusScope instance.  Adds various global methods.
     * @param sconf servlet config used to access files within web context
     */
    public CirrusScope(ServletConfig sconf) {
        this.sconf = sconf;
        put("sconf", this, sconf);
        Context cx = Context.enter();
        initStandardObjects(cx, false);
        String[] names = {
            "fileLastModified",
            "getResource",
            "getResourcePaths",
            "jst",
            "load",
            "log",
            "logf",
            "logwarn",
            "logerror",
            "print",
            "printf",
            "readFile",
        };
        defineFunctionProperties(names, CirrusScope.class, ScriptableObject.DONTENUM);
        put("JSON", this, new com.joelhockey.cirrus.RhinoJSON(this));
        Context.exit();
    }

    /**
     * Return last modified date of given file.  Caches results and stores for
     * {@link #RELOAD_WAIT} before checking again.  Adds item to cache if
     * not already exists.
     * @param path filename
     * @return last modified date or -1 if file not exists.
     * @throws IOException if file not exists
     */
    public long fileLastModified(String path) throws IOException {
            CacheEntry<Object> entry = cacheLookup(LAST_MOD_CACHE, path);
            if (entry == null) {
                // no in cache, or stale
                entry = new CacheEntry<Object>(
                        getResource(path).getLastModified(),
                        System.currentTimeMillis(), null);
                LAST_MOD_CACHE.put(path, entry);
            }
            return entry.lastModified;
    }

    /**
     * Return {@link URLConnection} to given path or throw
     * {@link IOException} if file not exists.  First looks for file under
     * '/WEB-INF' using {@link ServletContext#getResource(String)}, then
     * tries to find file in classpath using {@link Class#getResource(String)}.
     * @param path file to get.  Should start with leading slash and use
     * forward slash for pathsep.
     * @return {@link URLConnection} to file or {@link IOException}
     * if file not exists
     * @throws IOException if file not exists
     */
    public URLConnection getResource(String path) throws IOException {
        // look in '/WEB-INF' first
        URL resource = sconf.getServletContext().getResource("/WEB-INF" + path);
        if (resource == null) {
            // not found in '/WEB-INF', try classloader
            resource = CirrusScope.class.getResource(path);
            if (resource == null) {
                // does not exist!
                throw new IOException("File not found: " + path);
            }
        }
        return resource.openConnection();
    }

    /**
     * Return set of files in given path or empty set for invalid path.
     * Uses files returned from {@link ServletContext#getResourcePaths(String)}
     * then adds any files from classloader.
     * Looks up URL of '/app/cirrus.js' with {@link Class#getResource(String)}.
     * If found in classloader, adds file in same filesystem or jar file as
     * '/app/cirrus.js' that match the given path.
     * @param path dir to get all files within
     * @return Set of files in given path or empty set for invalid path.
     * Should start with leading slash and  use forward slash for pathsep.
     * @throws IOException if error reading files
     */
    public Set<String> getResourcePaths(String path) throws IOException {
        // start with files in /WEB-INF/...
        Set<String> result = sconf.getServletContext().getResourcePaths("/WEB-INF" + path);
        if (result == null) {
            result = new HashSet<String>();
        }

        // load via classpath.  Use '/app/cirrus.js' to detect file or jar path
        String cirrusjs = CirrusScope.class.getResource("/app/cirrus.js").toString();

        // file is in classpath - e.g. file:/.../app/WEB-INF/classes
        if (cirrusjs.startsWith("file:")) {
            try {
                File appdir = new File(new URI(cirrusjs)).getParentFile();
                File dir = new File(appdir.getParentFile(), path);
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File file : list) {
                        // add trailing slash for dirs
                        result.add(path + file.getName() +
                                (file.isDirectory() ? "/" : ""));
                    }
                }
            } catch (URISyntaxException e) {
                log.warn("Unexpected error converting URI: " + cirrusjs, e);
            }

        // file is in jar - e.g. jar:file:/.../app/WEB-INF/lib/cirrus.jar!...
        } else if (cirrusjs.startsWith("jar:")) {
            String jarFileName = cirrusjs.substring(4); // skip 'jar:'
            int bang = jarFileName.lastIndexOf('!');
            if (bang != -1) {
                jarFileName = jarFileName.substring(0, bang);
            }
            try {
                File jarFile = new File(new URI(jarFileName));

                // read entries of zip file to find files in same dir
                ZipFile zip = new ZipFile(jarFile);
                for (Enumeration<? extends ZipEntry> en = zip.entries();
                        en.hasMoreElements(); ) {

                    ZipEntry entry = en.nextElement();
                    String zipFile = entry.getName();
                    // canonicalize filename
                    zipFile = zipFile.replace('\\', '/');
                    if (!zipFile.startsWith("/")) {
                        zipFile = "/" + zipFile;
                    }
                    if (zipFile.startsWith(path)) {
                        // only match files in same dir, not subdirs
                        int slash = zipFile.indexOf('/', path.length());
                        if (slash != -1) {
                            // keep trailing slash
                            zipFile = zipFile.substring(0, slash + 1);
                        }
                        result.add(zipFile);
                    }
                }
            } catch (URISyntaxException e) {
                log.warn("Unexpected error converting URI: " + cirrusjs, e);
            }
        }
        return result;
    }


    /**
     * Load javascript file into this scope.  File will only be executed if it doesn't
     * already exist, or if it has been modified since it was last loaded.
     * @param path file to load
     * @return true if file was (re)loaded, false if no change
     * @throws IOException if error reading file
     */
    public boolean load(String path) throws IOException {
        CacheEntry<Script> entry = cacheLookup(localScriptCache, path);
        if (entry != null) {
            return false;
        }

        // execute script in current scope
        Context cx = Context.enter();
        try {
            entry = cacheLookup(SCRIPT_CACHE, path);
            if (entry == null) {
                // compile script and store in static SCRIPT_CACHE
                URLConnection urlc = getResource(path);
                Reader reader = new BufferedReader(new InputStreamReader(
                        urlc.getInputStream()));
                try {
                    log.info("compiling script: " + path);
                    Script script = cx.compileReader(
                            reader, urlc.getURL().toString(), 1, null);
                    entry = new CacheEntry<Script>(urlc.getLastModified(),
                            System.currentTimeMillis(), script);
                    SCRIPT_CACHE.put(path, entry);
                } finally {
                    reader.close();
                }
            }
            Script script = (Script) entry.object;
            log.info("executing script: " + path);
            script.exec(cx, this);
            // save to local scope cache
            localScriptCache.put(path, entry);
            return true;
        } finally {
            Context.exit();
        }
    }

    /**
     * Read file.  2nd arg is optional stream for reading into.
     * If not supplied return string result.
     * @param path file to read
     * @param objOuts optional output stream
     * @return string result if no output stream supplied else null
     * @throws IOException if error reading
     */
    public String readFile(String path, Object objOuts) throws IOException {
        if (objOuts == null || objOuts == Undefined.instance) {
            log.info("readFile: " + path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            readFileIntoStream(path, baos);
            return new String(baos.toByteArray());
        } else {
            log.info("readFile(stream): " + path);
            OutputStream outs = (OutputStream) Context.jsToJava(objOuts, OutputStream.class);
            readFileIntoStream(path, outs);
            return null;
        }
    }

    private void readFileIntoStream(String path, OutputStream outs) throws IOException {
        InputStream ins = getResource(path).getInputStream();
        try {
            byte[] buf = new byte[4096];
            for (int l = 0; (l = ins.read(buf)) != -1; ) {
                outs.write(buf, 0, l);
            }
        } finally {
            ins.close();
        }
    }

    /** Print objects to log */
    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isInfoEnabled()) log.info(dump(args));
    }
    /** printf to log */
    public static void printf(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isInfoEnabled()) log.info(printf(args));
    }
    /** Print objects to log */
    public static void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isInfoEnabled()) log.info(dump(args));
    }
    /** printf to log */
    public static void logf(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isInfoEnabled()) log.info(printf(args));
    }
    /** Print objects to log */
    public static void logwarn(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.warn(dump(args));
    }
    /** Print objects to log */
    public static void logerror(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.error(dump(args));
    }

    // format using printf, first arg is string, other args get substitued in
    private static String printf(Object[] args) {
        if (args == null) return null;
        String fstr = (String) args[0];
        fstr = fstr.replace("%d", "%.0f"); // all numbers will be doubles, so convert here
        Object[] fargs = new Object[args.length - 1];
        System.arraycopy(args, 1, fargs, 0, fargs.length);
        return String.format(fstr, fargs);
    }

    // return JSON formatted
    private static String dump(Object[] args) {
        if (args == null) return null;
        if (args.length == 1) {
            if (args[0] instanceof String) {
                return (String) args[0];
            } else {
                return RhinoJSON.stringify(args[0]);
            }
        }
        NativeArray array = new NativeArray(args);
        return RhinoJSON.stringify(array);
    }

    /**
     * Render specified JST template.  Optional parameters are ctlr, action and context.
     * If not specified, ctlr uses global variable 'controller', action uses global 'action'
     * and context uses the controller.
     * @param args is either [], [action], [context], [action, context], [ctlr, action], [ctlr, action, context]
     * @throws IOException if error loading template
     */
    public void jst(Object arg1, Object arg2, Object arg3) throws IOException {
        // shift all args to the right until we get a string in arg2
        // then arg1=ctlr, arg2=action, arg3=context
        for (int i = 0; i < 2; i++) {
            if (!(arg2 instanceof String)) {
                arg3 = arg2;
                arg2 = arg1;
                arg1 = Undefined.instance; // bears in the bed - they all roll over and one falls out
            }
        }
        String controller = (String) (arg1 == Undefined.instance ? get("controller", this) : arg1);
        String action = (String) (arg2 == Undefined.instance ? get("action", this) : arg2);
        Object context = arg3;
        if (arg3 == Undefined.instance) {
            Scriptable controllers = (Scriptable) get("controllers", this);
            context = controllers.get(controller, controllers);
        }

        // reload 'jst.js'.  Clear templateCache if jst.js has changed
        if (load("/app/jst.js")) {
            templateCache.clear();
        }

        String name = controller + "." + action;
        Context cx = Context.enter();
        try {
            NativeObject template = loadjst(cx, name).object;
            NativeJavaObject njoRes = (NativeJavaObject) get("res", this);
            HttpServletResponse res = (HttpServletResponse) njoRes.unwrap();
            res.setContentType("text/html");
            // template.render(res.getWriter(), context)
            Object[] args = {Context.javaToJS(res.getWriter(), template), context};
            ScriptableObject.callMethod(cx, template, "render", args);
        } finally {
            Context.exit();
        }
    }

    private CacheEntry<NativeObject> loadjst(Context cx,
            String name) throws IOException {

        String path = "/app/views/" + name.replace('.', '/') + ".jst";
        CacheEntry<NativeObject> result = cacheLookup(templateCache, path);
        if (result != null) {
            return result;  // found in cache
        }

        // not found in cache, must compile and execute
        log.info("loadjst: " + path);
        String jstFile = readFile(path, null);
        // if prototype declared, then load it
        Pattern p = Pattern.compile("^\\s*\\{[ \\t]*prototype[ \\t]+([^\\s{}]+)[ \\t]*}");
        Matcher m = p.matcher(jstFile);
        if (m.find()) {
            loadjst(cx, m.group(1));
        }

        CacheEntry<Script> entry = cacheLookup(SCRIPT_CACHE, path);
        if (entry == null) {
            // call JST.parse(<jst file contents>)
            ScriptableObject jstObj = (ScriptableObject) get("JST", this);
            Function parse = (Function) jstObj.get("parse", jstObj);
            try {
                log.info("JST.parse(" + name + ".jst)");
                String source = (String) parse.call(cx, this, this, new Object[] {jstFile, name});
                File tempDir = (File) sconf.getServletContext().getAttribute("javax.servlet.context.tempdir");
                String sourceName = "views/" + name + ".js";
                if (tempDir != null) {
                    File jstDir = new File(tempDir, "jst");
                    jstDir.mkdir();
                    File compiledJstFile = new File(jstDir, name + ".js");
                    sourceName = compiledJstFile.toURI().toString();
                    log.info("Writing compiled jst file to tmp file: " + compiledJstFile);
                    FileOutputStream fos = new FileOutputStream(compiledJstFile);
                    try {
                        fos.write(source.getBytes());
                    } finally {
                        fos.close();
                    }
                }
                Script script = cx.compileString(source, sourceName, 1, null);
                URLConnection urlc = getResource(path);
                entry = new CacheEntry<Script>(urlc.getLastModified(),
                        System.currentTimeMillis(), script);
                SCRIPT_CACHE.put(path, entry);
            } catch (JavaScriptException jse) {
                    IOException ioe = new IOException("Error loading views/" + name + ".js: " + jse.getMessage());
                    ioe.initCause(jse);
                    throw ioe;
            }
        }

        // execute compiled JST code in this scope
        entry.object.exec(cx, this);

        // return JST.templates[name]
        ScriptableObject jstObj = (ScriptableObject) get("JST", this);
        ScriptableObject templates = (ScriptableObject) jstObj.get("templates", jstObj);
        Function f = (Function) templates.get(name, templates);
        NativeObject template = (NativeObject) f.construct(cx, this, new Object[0]);
        result = new CacheEntry<NativeObject>(entry.lastModified,
                entry.lastChecked, template);
        templateCache.put(path, result);
        return result;
    }

    /**
     * Return cache entry if valid, or null if item not in cache or stale.
     * @param <T> type of cache object
     * @param cache cache to search
     * @param path cache lookup key used with {@link ServletContext#getResource(String)}
     * @return cache entry or null if item not in cache or stale
     * @throws IOException if resource not found
     */
    private <T> CacheEntry<T> cacheLookup(Map<String, CacheEntry<T>> cache,
            String path) throws IOException {

        CacheEntry<T> entry = cache.get(path);
        if (entry != null) {
            long now = System.currentTimeMillis();
            if (entry.lastChecked + RELOAD_WAIT > now) {
                // we have recently checked file timestamp
                return entry;
            } else {
                // check file timestamp and compare with cache value
                if (getResource(path).getLastModified() == entry.lastModified) {
                    // file hasn't changed - update entry.lastChecked
                    entry.lastChecked = now;
                    return entry;
                }
            }
        }
        // cache not exists or is stale
        return null;
    }

    static class CacheEntry<T> {
        long lastModified;
        long lastChecked;
        T object;
        CacheEntry(long lastModified, long lastcheck, T object) {
            this.lastModified = lastModified;
            this.lastChecked = lastcheck;
            this.object = object;
        }
    }
}
