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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Rhino scope for CirrusServlet.  Based on {@link Global}.
 * Each servlet thread has its own instance of this class.
 * Provides helper methods such as load to load js libs.  Uses
 * caching to only reload files that have modified.
 * @author Joel Hockey
 */
public class CirrusScope extends ImporterTopLevel {
    private static final String TRIMPATH_PARSETEMPLATE = "parseTemplate";
    private static final String TRIMPATH_VARNAME = "TrimPath";
    private static final String TRIMPATH_JS = "/WEB-INF/app/trimpath.js";
	private static final Log log = LogFactory.getLog(CirrusScope.class);
	private static final Log jsLog = LogFactory.getLog("com.joelhockey.cirrus.js");
    public static final long RELOAD_WAIT = 3000;
    private ServletConfig sconf;
    private Map<String, CacheEntry> fileCache = new HashMap<String, CacheEntry>();
    private Map<String, CacheEntry> templateCache = new HashMap<String, CacheEntry>();
    private Map<String, CacheEntry> lastModCache = new HashMap<String, CacheEntry>();

    /**
     * Create CirrusScope instance.  Adds methods {@link #load(String)},
     * {@link #parseFile(String)}, {@link #readFile(String)}, {@link #print(Context, Scriptable, Object[], Function)},
     * {@link #template(String)} to scope, and also add commons-logger 'log' var.
     * @param cx context
     * @param sconf servlet config used for looking real paths from URL paths
     */
    public CirrusScope(Context cx, ServletConfig sconf) {
        super(cx);
        this.sconf = sconf;
        String[] names = {
            "lastModified",
            "load",
            "parseFile",
            "print",
            "readFile",
            "template",
        };
        defineFunctionProperties(names, CirrusScope.class, ScriptableObject.DONTENUM);
        put("log", this, new NativeJavaObject(this, jsLog, null));
    }

    /**
     * Return last modified date of given file.  Caches results and stores for
     * {@link #RELOAD_WAIT} before checking again.  Adds item to cache if
     * not already exists.
     * @param path filename
     * @return last modified date or -1 if file not exists.
     */
    public long lastModified(String path) {
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
      * @param path URL path will be converted to real path
     * @return true if file was (re)loaded, false if no change
     * @throws IOException if error reading file
     */
    public boolean parseFile(String path) throws IOException {
        log.info("parsing file: " + path);
        // check cache again inside synchronized method
        CacheEntry entry = fileCache.get(path);
        if (entry != null && new File(path).lastModified() == entry.lastModified) {
            log.debug("file already parsed: " + path);
            return false;
        }
        File file = new File(path);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        Context cx = Context.enter();
        try {
            Object obj = cx.evaluateReader(this, reader, path, 1, null);
            reader.close();
            fileCache.put(path, new CacheEntry(path, file.lastModified(), System.currentTimeMillis(), obj));
            return true;
        } finally {
            Context.exit();
        }
    }

    public String readFile(String path, Object objOuts) throws IOException {
        if (objOuts == null) {
            log.info("readFile: " + path);
            return readFile(path);
        } else {
            log.info("readFile(stream): " + path);
            Context cx = Context.enter();
            try {
                OutputStream outs = (OutputStream) cx.jsToJava(objOuts, OutputStream.class);
                readFileIntoStream(path, outs);
                return null;
            } finally {
                cx.exit();
            }
        }
    }

    private String readFile(String path) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readFileIntoStream(path, baos);
        return new String(baos.toByteArray());
    }

    private void readFileIntoStream(String path, OutputStream outs) throws IOException {
        String rpath = sconf.getServletContext().getRealPath(path);
        FileInputStream fis = new FileInputStream(rpath);
        byte[] buf = new byte[4096];
        for (int l = 0; (l = fis.read(buf)) != -1; ) {
            outs.write(buf, 0, l);
        }
    }

    /**
     * Print objects to sysout.  Better to use commons-logging var 'log'.
     * @param cx javascript context
     * @param thisObj scope - ignored
     * @param args args to print
     * @param funObj function - ignored
     */
    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (int i=0; i < args.length; i++) {
            sb.append(sep);
            sep = " ";
            sb.append(Context.toString(args[i]));
        }
        log.info(sb);
    }

    /**
     * Load {@link TrimPath http://code.google.com/p/trimpath/} template.
     * @param path path of template starting with '/WEB-INF/app/view/'
     * @return template
     * @throws IOException if error loading template
     */
    public Object template(String path) throws IOException {
        if (load(TRIMPATH_JS)) {
            templateCache.clear();
            // modifiers from trimpath also get stored within servlets
        }
        String rpath = sconf.getServletContext().getRealPath(path);
        CacheEntry entry = templateCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null) {
            if (entry.lastChecked + RELOAD_WAIT > now) {
                return entry.object;
            } else if (new File(rpath).lastModified() == entry.lastModified) {
                entry.lastChecked = now;
                return entry.object;
            }
        }
        ScriptableObject tp = (ScriptableObject) get(TRIMPATH_VARNAME, this);
        Function parseTemplate = (Function) tp.get(TRIMPATH_PARSETEMPLATE, tp);
        Context cx = Context.enter();
        try {
            log.info("loading template: " + path);
            Object template = parseTemplate.call(cx, this, this, new Object[] {readFile(path), rpath});
            entry = new CacheEntry(rpath, new File(rpath).lastModified(), now, template);
            templateCache.put(rpath, entry);
        } finally {
            Context.exit();
        }
        return entry.object;
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
