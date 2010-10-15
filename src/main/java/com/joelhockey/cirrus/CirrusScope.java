/**
 * The MIT Licence
 *
 * Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.joelhockey.cirrus;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
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
    public static final long RELOAD_WAIT = 3000;

    private ServletConfig sconf;
    private Map<String, CacheEntry> fileCache = new HashMap<String, CacheEntry>();
    private Map<String, CacheEntry> templateCache = new HashMap<String, CacheEntry>();
    private Map<String, CacheEntry> lastModCache = new HashMap<String, CacheEntry>();

    /**
     * Create CirrusScope instance.  Adds various global methods.
     * @param cx context
     * @param sconf servlet config used for looking real paths from URL paths
     */
    public CirrusScope(Context cx, ServletConfig sconf) {
        super(cx);
        this.sconf = sconf;
        String[] names = {
            "fileLastModified",
            "jst",
            "load",
            "log",
            "logf",
            "logwarn",
            "logerror",
            "parseFile",
            "print",
            "printf",
            "readFile",
        };
        defineFunctionProperties(names, CirrusScope.class, ScriptableObject.DONTENUM);
        put("sconf", this, Context.javaToJS(sconf, this));
    }

    /**
     * Return last modified date of given file.  Caches results and stores for
     * {@link #RELOAD_WAIT} before checking again.  Adds item to cache if
     * not already exists.
     * @param path filename
     * @return last modified date or -1 if file not exists.
     */
    public long fileLastModified(String path) {
        String rpath = sconf.getServletContext().getRealPath(path);
        CacheEntry entry = lastModCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null && entry.lastChecked + RELOAD_WAIT > now) {
            return entry.lastModified;
        }
        File f = new File(rpath);
        long lastMod = f.exists() ? f.lastModified() : -1;
        if (entry == null) {
            entry = new CacheEntry(rpath, lastMod, now, null);
            lastModCache.put(rpath, entry);
        } else {
            entry.lastChecked = now;
        }
        return entry.lastModified;
    }

    /**
     * Load javascript file into this context.  File will only be loaded if it doesn't
     * already exist, or if it has been modified since it was last loaded.
     * @param path URL path will be converted to real path
     * @return true if file was (re)loaded, false if no change
     * @throws IOException if error reading file
     */
    public boolean load(String path) throws IOException {
        String rpath = sconf.getServletContext().getRealPath(path);
        CacheEntry entry = fileCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null) {
            if (entry.lastChecked + RELOAD_WAIT > now) {
                return false;
            } else if (new File(rpath).lastModified() == entry.lastModified) {
                entry.lastChecked = now;
                return false;
            }
        }
        return parseFile(rpath);
    }

    /**
     * Parse file and put into local cache.
     * @param fullpath full path to file
     * @return true if file was (re)loaded, false if no change
     * @throws IOException if error reading file
     */
    public boolean parseFile(String fullpath) throws IOException {
        log.info("parsing file: " + fullpath);
        CacheEntry entry = fileCache.get(fullpath);
        if (entry != null && new File(fullpath).lastModified() == entry.lastModified) {
            log.debug("file already parsed: " + fullpath);
            return false;
        }
        File file = new File(fullpath);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        Context cx = Context.enter();
        try {
            Object obj = cx.evaluateReader(this, reader, fullpath, 1, null);
            fileCache.put(fullpath, new CacheEntry(fullpath, file.lastModified(), System.currentTimeMillis(), obj));
            return true;
        } finally {
            Context.exit();
            reader.close();
        }
    }

    /**
     * Read file.  2nd arg is optional stream for reading into.  If not supplied return string result.
     * @param path file to read
     * @param objOuts optional output stream
     * @return string result if no output stream supplied else null
     * @throws IOException if error reading
     */
    public String readFile(String path, Object objOuts) throws IOException {
        if (objOuts == null || objOuts == Undefined.instance) {
            log.info("readFile: " + path);
            return readFile(path);
        } else {
            log.info("readFile(stream): " + path);
            OutputStream outs = (OutputStream) Context.jsToJava(objOuts, OutputStream.class);
            readFileIntoStream(path, outs);
            return null;
        }
    }

    private String readFile(String path) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readFileIntoStream(path, baos);
        return new String(baos.toByteArray());
    }

    private void readFileIntoStream(String path, OutputStream outs) throws IOException {
        String rpath = sconf.getServletContext().getRealPath(path);
        if (rpath == null) {
            throw new IOException("No path for file: " + path);
        }
        FileInputStream fis = new FileInputStream(rpath);
        try {
            byte[] buf = new byte[4096];
            for (int l = 0; (l = fis.read(buf)) != -1; ) {
                outs.write(buf, 0, l);
            }
        } finally {
            fis.close();
        }
    }

    /** Print objects to log */
    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.info(dump(args));
    }
    /** printf to log */
    public static void printf(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.info(printf(args));
    }
    /** Print objects to log */
    public static void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.info(dump(args));
    }
    /** printf to log */
    public static void logf(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.info(printf(args));
    }
    /** Print objects to log */
    public static void logwarn(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.warn(dump(args));
    }
    /** Print objects to log */
    public static void logerror(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log.error(dump(args));
    }

    private static String printf(Object[] args) {
        if (args == null) return null;
        String fstr = (String) args[0];
        fstr = fstr.replace("%d", "%.0f"); // all numbers will be doubles, so convert here
        Object[] fargs = new Object[args.length - 1];
        System.arraycopy(args, 1, fargs, 0, fargs.length);
        return String.format(fstr, fargs);
    }

    public static String dump(Object[] args) {
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
     * and context uses the global scope (this).
     * @param args is either [], [action], [context], [action, context], [ctlr, action], [ctlr, action, context]
     * @throws IOException if error loading template
     */
    public void jst(Object arg1, Object arg2, Object arg3) throws IOException {
        // shift all args to the right until we get a string in arg2
        // then arg1=ctlr, arg2=action, arg3=cx
        for (int i = 0; i < 2; i++) {
            if (!(arg2 instanceof String)) {
                arg3 = arg2;
                arg2 = arg1;
                arg1 = Undefined.instance; // bears in the bed - they all roll over and one falls out
            }
        }
        String controller = (String) (arg1 == Undefined.instance ? get("controller", this) : arg1);
        String action = (String) (arg2 == Undefined.instance ? get("action", this) : arg2);
        Object context = arg3 == Undefined.instance ? this : arg3;

        // reload 'jst.js' if changed
        if (load("/WEB-INF/app/jst.js")) {
            templateCache.clear();
        }

        String path = "/WEB-INF/app/views/" + controller + "/" + action + ".jst";

        // get template from cache
        String rpath = sconf.getServletContext().getRealPath(path);
        if (rpath == null) {
            throw new IOException("Could not load jst template for controller: [" + controller + "], view: ["
                    + action + "] at path: [" + path + "]");
        }

        NativeObject template = null;
        CacheEntry entry = templateCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null) {
            if (entry.lastChecked + RELOAD_WAIT > now) {
                template = (NativeObject) entry.object;
            } else if (new File(rpath).lastModified() == entry.lastModified) {
                entry.lastChecked = now;
                template = (NativeObject) entry.object;
            }
        }

        HttpServletResponse res = (HttpServletResponse) ((NativeJavaObject) get("res", this)).unwrap();
        res.setContentType("text/html");
        Context cx = Context.enter();
        try {
            // load template now if not already loaded
            if (template == null) {
                template = loadjst(controller + "." + action);
                entry = new CacheEntry(rpath, new File(rpath).lastModified(), now, template);
                templateCache.put(rpath, entry);
            }
            ScriptableObject.callMethod(cx, template, "render",
                    new Object[] {Context.javaToJS(res.getWriter(), template), context});
        } finally {
            Context.exit();
        }
    }

    private NativeObject loadjst(String name) throws IOException {
        String path = "/WEB-INF/app/views/" + name.replace(".", "/") + ".jst";
        log.info("loadjst: " + path);
        String jstFile = readFile(path);
        // if prototype declared, then load it first
        Pattern p = Pattern.compile("^\\s*\\{[ \\t]*prototype[ \\t]+([^\\s{}]+)[ \\t]*}");
        Matcher m = p.matcher(jstFile);
        if (m.find()) {
            loadjst(m.group(1));
        }
        ScriptableObject jstObj = (ScriptableObject) get("JST", this);
        Function parse = (Function) jstObj.get("parse", jstObj);
        Context cx = Context.enter();
        try {
            log.info("JST.parse(" + name + ".jst)");
            String source = (String) parse.call(cx, this, this, new Object[] {jstFile, name});
            File tempDir = (File) sconf.getServletContext().getAttribute("javax.servlet.context.tempdir");
            if (tempDir != null) {
                File jstDir = new File(tempDir, "jst");
                jstDir.mkdir();
                File compiledJstFile = new File(jstDir, name + ".js");
                log.info("Writing compiled jst file to tmp file: " + compiledJstFile);
                FileOutputStream fos = new FileOutputStream(compiledJstFile);
                try {
                    fos.write(source.getBytes());
                } finally {
                    fos.close();
                }
            }
            cx.evaluateString(this, source, "views/" + name + ".js", 1, null);
            ScriptableObject templates = (ScriptableObject) jstObj.get("templates", jstObj);
            Function f = (Function) templates.get(name, templates);
            return (NativeObject) f.construct(cx, this, new Object[0]);
        } catch (JavaScriptException jse) {
                IOException ioe = new IOException("Error loading views/" + name + ".js: " + jse.getMessage());
                ioe.initCause(jse);
                throw ioe;
        } finally {
            Context.exit();
        }
    }

    private static class CacheEntry {
        public String filename;
        public long lastModified;
        public long lastChecked;
        public Object object;

        CacheEntry(String filename, long date, long lastcheck, Object object) {
            this.filename = filename;
            this.lastModified = date;
            this.lastChecked = lastcheck;
            this.object = object;
        }
    }
}
