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
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import com.joelhockey.cirrus.RhinoJava.RhinoList;

/**
 * Main cirrus object.
 * @author Joel Hockey
 */
public class Cirrus extends NativeObject {
    private static final long serialVersionUID = 0xDC7C4EC5275394BL;
    private static final Log log = LogFactory.getLog(Cirrus.class);
    private static enum LogLevel { INFO, WARN, ERROR };

    /** Time to wait before reloading changed js file. */
    public static final long RELOAD_WAIT = 10000;

    // static maps hold all compiled scripts
    private static Map<String, CacheEntry<Script>> SCRIPT_CACHE = new HashMap<String, CacheEntry<Script>>();
    private static Map<String, CacheEntry<Object>> LAST_MOD_CACHE = new HashMap<String, CacheEntry<Object>>();

    // local maps get populated when compiled scripts are executed in current scope
    private Map<String, CacheEntry<Script>> localScriptCache = new HashMap<String, CacheEntry<Script>>();
    private Map<String, CacheEntry<NativeObject>> templateCache = new HashMap<String, CacheEntry<NativeObject>>();
    private ServletConfig servletConfig;
    private Timer timer = new Timer();
    private Scriptable controllers;

    /**
     * Create Cirrus instance.  Adds various global methods.
     * @param global global scope
     * @param servletConfig servlet config used to access files within web context
     */
    public Cirrus(Scriptable global, ServletConfig servletConfig) {
        this.servletConfig = servletConfig;

        Context cx = Context.enter();
        ScriptRuntime.setObjectProtoAndParent(this, global);
        controllers = cx.newObject(global);
        put("controllers", this, controllers);

        // cirrus functions
        String[] names = {
            "dir",
            "fileLastModified",
            "getResource",
            "getResourcePaths",
            "h",
            "jst",
            "load",
            "log",
            "logwarn",
            "logerror",
            "print",
            "readFile",
        };
        defineFunctionProperties(names, Cirrus.class, ScriptableObject.DONTENUM);
        put("servletConfig", this, servletConfig);
        put("servletContext", this, servletConfig.getServletContext());
        put("timer", this, timer);
        Context.exit();
    }

    /**
     * Similar to python dir() function.
     * @return list of properties of ob prototype chain
     */
    public static Scriptable dir(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {

        RhinoList result = new RhinoList(thisObj, new ArrayList<Object>());
        if (args == null || args.length == 0
                || !(args[0] instanceof Scriptable)) {
            return result;
        }
        Scriptable s = (Scriptable) args[0];
        while (true) {
            Object[] ids = s instanceof ScriptableObject
                ? ((ScriptableObject)s).getAllIds() : s.getIds();
            Arrays.sort(ids);
            for (Object id : ids) {
                result.add(id);
            }
            s = s.getPrototype();
            if (s == null) {
                break;
            } else {
                result.add("->");
            }
        }
        return result;
    }

    /**
     * Return last modified date of given file.  Caches results and stores for
     * {@link #RELOAD_WAIT} before checking again.  Adds item to cache if
     * not already exists.
     * @param path filename
     * @return last modified date
     * @throws IOException if file not exists
     */
    public long fileLastModified(String path) throws IOException {
            CacheEntry<Object> entry = cacheLookup(LAST_MOD_CACHE, path);
            if (entry == null) {
                // not in cache, or stale
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
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // look in '/WEB-INF' first
        URL resource = servletConfig.getServletContext().getResource(
                "/WEB-INF" + path);
        if (resource == null) {
            // not found in '/WEB-INF', try classloader
            resource = Cirrus.class.getResource(path);
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
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        // start with files in /WEB-INF/...
        Set<String> result = new HashSet<String>();
        Set<String> webinf = servletConfig.getServletContext()
                .getResourcePaths("/WEB-INF" + path);

        if (webinf != null) {
            // strip '/WEB-INF' from front of string
            for (String webinfFile : webinf) {
                result.add(webinfFile.substring("/WEB-INF".length()));
            }
        }

        // load via classpath.  Use '/app/cirrus.js' to detect file or jar path
        String cirrusjs = Cirrus.class.getResource("/app/cirrus.js").toString();

        // file is in regular file - file:/.../app/WEB-INF/classes
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
            } catch (Exception e) {
                log.warn("Unexpected error converting URI: " + cirrusjs, e);
            }

        // file is in jar - jar:file:/.../app/WEB-INF/lib/cirrus.jar!...
        } else if (cirrusjs.startsWith("jar:")) {
            String jarFileName = cirrusjs.substring(4); // skip 'jar:'
            int bang = jarFileName.lastIndexOf('!');
            if (bang != -1) {
                jarFileName = jarFileName.substring(0, bang);
            }
            ZipFile zipFile = null;
            try {
                // read entries of zip file to find files in same dir
                zipFile = new ZipFile(new File(new URI(jarFileName)));
                for (Enumeration<? extends ZipEntry> en = zipFile.entries();
                        en.hasMoreElements(); ) {

                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    // canonicalize filename (start with slash, only forward slash)
                    entryName = entryName.replace('\\', '/');
                    if (!entryName.startsWith("/")) {
                        entryName = "/" + entryName;
                    }
                    if (entryName.startsWith(path)) {
                        // only match files in same dir, not subdirs
                        int slash = entryName.indexOf('/', path.length());
                        if (slash != -1) {
                            // keep trailing slash
                            entryName = entryName.substring(0, slash + 1);
                        }
                        result.add(entryName);
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected error converting URI: " + cirrusjs, e);
            }
            if (zipFile != null) {
                zipFile.close();
            }
        }
        return result;
    }

    /** @return timer */
    public Timer getTimer() { return timer; }

    /**
     * HTML escape.  If writer included, escaped string written to writer
     * and empty string returned, else escaped string returned
     * @param s string to escape
     * @param writer optional writer
     * @return if writer included, escaped string written to writer and empty
     * string returned, else escaped string returned
     * @throws IOException if error
     */
    public static String h(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) throws IOException {

        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        String s = ScriptRuntime.toString(args[0]);

        Object writer = args.length > 1 ? args[1] : null;
        if (writer instanceof NativeJavaObject) {
            writer = ((NativeJavaObject)writer).unwrap();
            if (writer instanceof Writer) {
                StringEscapeUtils.escapeHtml((Writer) writer, s);
                return "";
            }
        }
        return StringEscapeUtils.escapeHtml(s);
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
            // ensure we are using our WrapFactory
            cx.setWrapFactory(CirrusServlet.WRAP_FACTORY);
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
            log.info("executing script: " + path);
            Script script = entry.object;
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
    /** Print objects to log */
    public static void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isInfoEnabled()) log.info(dump(args));
    }
    /** Print objects to log */
    public static void logwarn(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (log.isWarnEnabled()) logImpl(LogLevel.WARN, args);
    }
    /** Print objects to log */
    public static void logerror(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        logImpl(LogLevel.ERROR, args);
    }
    // log with last arg converted to Throwable when detected
    private static void logImpl(LogLevel level, Object[] args) {
        // check if last arg is exception
        if (args.length > 0 && args[args.length - 1] instanceof Scriptable) {
            Scriptable lastArg = (Scriptable) args[args.length - 1];
            Object javaException = lastArg.get("javaException", lastArg);
            if (javaException == Scriptable.NOT_FOUND) {
                javaException = lastArg.get("rhinoException", lastArg);
            }
            if (javaException instanceof NativeJavaObject) {
                NativeJavaObject javaOb = (NativeJavaObject) javaException;
                Object unwrapped = javaOb.unwrap();
                if (unwrapped instanceof Throwable) {
                    Throwable t = (Throwable) unwrapped;
                    Object[] argsExceptLast = new Object[args.length - 1];
                    System.arraycopy(args, 0, argsExceptLast, 0, argsExceptLast.length);
                    if (level == LogLevel.INFO) {
                        log.info(dump(argsExceptLast), t);
                    } else if (level == LogLevel.WARN) {
                        log.warn(dump(argsExceptLast), t);
                    } else if (level == LogLevel.ERROR) {
                        log.error(dump(argsExceptLast), t);
                    }
                    return;
                }
            }
        }

        // last arg was not exception
        if (level == LogLevel.INFO) {
            log.info(dump(args));
        } else if (level == LogLevel.WARN) {
            log.warn(dump(args));
        } else if (level == LogLevel.ERROR) {
            log.error(dump(args));
        }
    }

    // return printf or JSON formatted
    private static String dump(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        } else if (args.length == 1) {
            if (args[0] instanceof String) {
                return (String) args[0];
            } else {
                return RhinoJSON.stringify(args[0]);
            }
        }

        // attempt printf and fall back to JSON
        if (args[0] instanceof String) {
            // all JS numbers are doubles, so change '%d' to equivalent
            String format = ((String)args[0]).replace("%d", "%.0f");
            Object[] formatArgs = new Object[args.length - 1];
            System.arraycopy(args, 1, formatArgs, 0, formatArgs.length);
            try {
                return String.format(format, formatArgs);
            } catch (IllegalFormatException ife) {} // ignore
        }

        return RhinoJSON.stringify(args);
    }

    /**
     * Render specified JST template.  Optional parameters are ctlr, action and context.
     * If not specified, ctlr uses global variable 'controller', action uses global 'action'
     * and context uses the controller.
     * @param args is either [], [action], [context], [action, context], [ctlr, action], [ctlr, action, context]
     * @throws IOException if error loading template
     */
    public void jst(Object arg1, Object arg2, Object arg3) throws IOException {
        timer.mark("action");
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
            // 'var context = cirrus.controllers[controller] || cirrus'
            context = controllers.get(controller, controllers);
            if (context == Scriptable.NOT_FOUND) {
                context = this;
            }
        }

        // reload 'jst.js'.  Clear templateCache if jst.js has changed
        if (load("/app/jst.js")) {
            templateCache.clear();
        }

        String name = controller + "." + action;
        Context cx = Context.enter();
        try {
            NativeObject template = loadjst(name, cx, null).object;
            NativeJavaObject njoRes = (NativeJavaObject) get("response", this);
            HttpServletResponse res = (HttpServletResponse) njoRes.unwrap();
            res.setContentType("text/html");
            // call template.render(res.getWriter(), context)
            Object[] args = {Context.javaToJS(res.getWriter(), template), context};
            ScriptableObject.callMethod(cx, template, "render", args);
        } finally {
            Context.exit();
            timer.mark("view");
        }
    }

    private CacheEntry<NativeObject> loadjst(String name, Context cx,
            Set<String> deps) throws IOException {

        String path = "/app/views/" + name.replace('.', '/') + ".jst";
        CacheEntry<NativeObject> result = cacheLookup(templateCache, path);
        if (result != null) {
            return result;  // found in cache
        }

        // not found in cache, must compile and execute
        log.info("loadjst: " + path);
        String jstFile = readFile(path, null);
        // if prototype or render/partial declared, then try to load deps
        if (deps == null) {
            deps = new HashSet<String>();
        }
        Pattern p = Pattern.compile("\\{[ \\t]*(prototype|render)[ \\t]+([^\\s{}]+)[ \\t]*\\}");
        Matcher m = p.matcher(jstFile);
        while (m.find()) {
            String dep = m.group(2);
            if (!deps.contains(dep)) {
                deps.add(dep);
                log.debug("loading jst " + name + " dependency " + dep);
                loadjst(dep, cx, deps);
            } else {
                log.debug("ignoring circular dependency " + name + " > " + dep);
            }
        }

        CacheEntry<Script> entry = cacheLookup(SCRIPT_CACHE, path);
        if (entry == null) {
            // call JST.parse(<jst file contents>)
            ScriptableObject jstObj = (ScriptableObject) get("JST", this);
            Function parse = (Function) jstObj.get("parse", jstObj);
            try {
                log.info("JST.parse(" + name + ".jst)");
                String source = (String) parse.call(cx, this, this, new Object[] {name, jstFile});
                File tempDir = (File) servletConfig.getServletContext().getAttribute("javax.servlet.context.tempdir");
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

        // return 'JST.templates[name]'
        ScriptableObject jstObj = (ScriptableObject) get("JST", this);
        ScriptableObject templates = (ScriptableObject) jstObj.get("templates", jstObj);
        NativeObject template = (NativeObject) templates.get(name, templates);
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

    public static class Timer {
        private static final Log log = LogFactory.getLog("cirrus.timer");
        private long[] times = new long[4];
        private String[] descs = new String[4];
        private int len = 0;
        /** start timer */
        public void start() {
            times[0] = System.currentTimeMillis();
            len = 1;
        }
        /** save current time and desc to be printed at end. */
        public void mark(String desc) {
            if (len == times.length && len < 256) { // grow storage up to 256
                long[] tmpTimes = new long[times.length * 2];
                System.arraycopy(times, 0, tmpTimes, 0, times.length);
                times = tmpTimes;
                String[] tmpDescs = new String[descs.length * 2];
                System.arraycopy(descs, 0, tmpDescs, 0, descs.length);
                descs = tmpDescs;
            }
            times[len] = System.currentTimeMillis();
            descs[len++] = desc;
        }
        /** print total time and desc, as well as any marks */
        public void end(String desc) {
            if (!log.isInfoEnabled()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(System.currentTimeMillis() - times[0]).append(": ").append(desc);
            for (int i = 1; i < len; i++) {
                sb.append(", ").append(times[i] - times[i - 1]).append(": ").append(descs[i]);
            }
            log.info(sb.toString());
        }
    }

}