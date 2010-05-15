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
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class CirrusScope extends ImporterTopLevel {
    private static final Log log = LogFactory.getLog(CirrusScope.class);
    public static final int RELOAD_WAIT = 3000;
    private ServletConfig sconf;
    private Map<String, CacheEntry> fileCache = new HashMap<String, CacheEntry>();
    private Map<String, CacheEntry> templateCache = new HashMap<String, CacheEntry>();

    public void init(Context cx, ServletConfig sconf) {
        this.sconf = sconf;
        initStandardObjects(cx, false);
        String[] names = {
            "load",
            "parseFile",
            "print",
            "readFile",
            "template",
        };
        defineFunctionProperties(names, CirrusScope.class, ScriptableObject.DONTENUM);
    }

    public boolean load(String path) throws IOException {
        String rpath = sconf.getServletContext().getRealPath(path);
        CacheEntry entry = fileCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null) {
            if (entry.lastcheck + RELOAD_WAIT > now) {
                return false;
            } else if (new File(rpath).lastModified() == entry.filedate) {
                entry.lastcheck = now;
                return false;
            }
        }

        return parseFile(rpath);
    }

    public synchronized boolean parseFile(String path) throws IOException {
        log.info("parsing file: " + path);
        // check cache again inside synchronized method
        CacheEntry entry = fileCache.get(path);
        if (entry != null && new File(path).lastModified() == entry.filedate) {
            log.debug("file already parsed: " + path);
            return false;
        }
        File file = new File(path);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        Context cx = Context.enter();
        try {
            Object obj = cx.evaluateReader(this, reader, path, 1, null);
            reader.close();
            fileCache.put(path, new CacheEntry(obj, file.lastModified(), System.currentTimeMillis()));
            return true;
        } finally {
            Context.exit();
        }
    }

    public String readFile(String path) throws IOException {
        log.info("readFile: " + path);
        String rpath = sconf.getServletContext().getRealPath(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(rpath);
        byte[] buf = new byte[4096];
        for (int l = 0; (l = fis.read(buf)) != -1; ) {
            baos.write(buf, 0, l);
        }
        return new String(baos.toByteArray());
    }

    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        String sep = "";
        for (int i=0; i < args.length; i++) {
            System.out.print(sep);
            sep = " ";
            System.out.print(Context.toString(args[i]));
        }
        System.out.println();
    }

    public Object template(String path) throws IOException {
        if (load("/WEB-INF/app/trimpath.js")) {
            templateCache.clear();
            fileCache.clear(); // modifiers from trimpath get stored within servlets
        }
        String rpath = sconf.getServletContext().getRealPath(path);
        CacheEntry entry = templateCache.get(rpath);
        long now = System.currentTimeMillis();
        if (entry != null) {
            if (entry.lastcheck + RELOAD_WAIT > now) {
                return entry.object;
            } else if (new File(rpath).lastModified() == entry.filedate) {
                entry.lastcheck = now;
                return entry.object;
            }
        }
        ScriptableObject tp = (ScriptableObject) get("TrimPath", this);
        Function parseTemplate = (Function) tp.get("parseTemplate", tp);
        Context cx = Context.enter();
        try {
            Object template = parseTemplate.call(cx, this, this, new Object[] {readFile(path), rpath});
            entry = new CacheEntry(template, new File(rpath).lastModified(), now);
            templateCache.put(rpath, entry);
        } finally {
            Context.exit();
        }
        return entry.object;
    }

    private static class CacheEntry {
        public Object object;
        public long filedate;
        public long lastcheck;

        CacheEntry(Object object, long date, long lastcheck) {
            this.object = object;
            this.filedate = date;
            this.lastcheck = lastcheck;
        }
    }
}
