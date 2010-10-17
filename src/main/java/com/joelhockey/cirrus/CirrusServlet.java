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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Enumeration;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.tools.debugger.Main;

/**
 * Main servlet for cirrus.  Copied some ideas from jython's PyServlet.
 * Manages ThreadLocal {@link CirrusScope} and dispatches to /WEB-INF/app/cirrus.js
 *
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final long serialVersionUID = 0x26FAB6AD9ECB6BDCL;
    private static final Log log = LogFactory.getLog(CirrusServlet.class);

    private boolean debugjs = false;
    private ThreadLocal<CirrusScope> localScope = new ThreadLocal<CirrusScope>() {
        @Override
        protected CirrusScope initialValue() {
            Context cx = Context.enter();
            try {
                return new CirrusScope(cx, getServletConfig());
            } finally {
                Context.exit();
            }
        }
    };
    private ThreadLocal<Main> debugger = new ThreadLocal<Main>() {
        @Override
        protected Main initialValue() {
//          try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            Main main = new Main("Cirrus Debug " + Thread.currentThread().getName());
            main.setScope(localScope.get());
            main.attachTo(ContextFactory.getGlobal());
            main.pack();
            main.setSize(800, 600);
            main.setVisible(true);
            return main;
        }
    };

    private DataSource dataSource;


    /** Ensure DB is at correct version, if not run migrations.  */
    @Override
    public void init() throws ServletException {
        // check if debugjs
        debugjs = System.getProperty("debugjs") != null || Boolean.valueOf(getServletConfig().getInitParameter("debugjs"));
        if (debugjs) {
            debugger.get();
        }

        // get datasource
        try {
            InitialContext ic = new InitialContext();
            String dbname = getServletConfig().getInitParameter("dbname");
            log.info("Looking up jndi db: " + dbname);
            dataSource = (DataSource) ic.lookup(dbname);
            // test
            Connection dbconn = dataSource.getConnection();
            dbconn.close();
        } catch (Exception e) {
            log.error("Error getting dbconn", e);
            throw new ServletException("Error getting dbconn", e);
        }

        DB db = null;
        try {
            CirrusScope scope = localScope.get();
            db = new DB(scope, dataSource);
            scope.put("DB", scope, db);
            scope.load("/WEB-INF/db/migrate.js");
        } catch (Exception e) {
            log.error("Error migrating db", e);
            throw new ServletException("Error migrating db", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Forward requests to WEB-INF/app/cirrus.js.
     * Puts variables, path, method and params (NativeObject), publicFiles (NativeObject) into global scope.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            CirrusScope scope = localScope.get();
            if (debugjs) {
                debugger.get();
            }

            if (scope.load("/WEB-INF/app/cirrus.js")) {
                NativeObject publicRoot = new NativeObject();
                for (File f : new File(getServletContext().getRealPath("/WEB-INF/public")).listFiles()) {
                    log.info("public: " + f.getName() + (f.isDirectory() ? " : dir" : " : file"));
                    publicRoot.put(f.getName(), publicRoot, true);
                }
                scope.put("PUBLIC_ROOT", scope, publicRoot);
            }
            String path = ((HttpServletRequest)req).getRequestURI();
            scope.put("path", scope, path);
            scope.put("method", scope, ((HttpServletRequest) req).getMethod());
            // put params in native JS object
            NativeObject params = new NativeObject();
            ScriptRuntime.setObjectProtoAndParent(params, scope);
            for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
                String key = en.nextElement();
                params.put(key, params, req.getParameter(key));
            }
            scope.put("params", scope, params);
            scope.put("req", scope, new NativeJavaObject(scope, req, HttpServletRequest.class));
            scope.put("res", scope, new NativeJavaObject(scope, res, HttpServletResponse.class));

            // set up DB
            DB db = new DB(scope, dataSource);
            scope.put("DB", scope, db);

            Context cx = Context.enter();

            try {
                Function cirrus = (Function) scope.get("cirrus", scope);
                //cx.setOptimizationLevel(9);
                cirrus.call(cx, scope, scope, new Object[0]);
            } finally {
                Context.exit();
                // close DB
                db.close();
                scope.delete("req");
                scope.delete("res");
            }
        } catch (Exception e) {
            log.error("error loading cirrus", e);
            throw new ServletException("Could not load cirrus", e);
        }
    }
}