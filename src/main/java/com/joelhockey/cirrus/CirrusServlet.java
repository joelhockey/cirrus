// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.IOException;
import java.sql.Connection;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
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
import org.mozilla.javascript.tools.debugger.Main;

/**
 * Main servlet for cirrus. Manages ThreadLocal {@link CirrusScope}
 * and dispatches to /app/cirrus.js
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final long serialVersionUID = 0x26FAB6AD9ECB6BDCL;
    private static final Log log = LogFactory.getLog(CirrusServlet.class);

    private static boolean STATIC_INIT = false;
    private static boolean DEBUG_JS = false;
    private static DataSource DATA_SOURCE;
    private static ServletConfig SERVLET_CONFIG;
    static final RhinoJava WRAP_FACTORY = new RhinoJava();
    static ThreadLocal<CirrusScope> THREAD_SCOPES = new ThreadLocal<CirrusScope>() {
        @Override
        protected CirrusScope initialValue() {
            return new CirrusScope(SERVLET_CONFIG);
        }
    };
    static ThreadLocal<Main> DEBUGGERS = new ThreadLocal<Main>() {
        @Override
        protected Main initialValue() {
            // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            Main main = new Main("Cirrus Debug " + Thread.currentThread().getName());
            main.setScope(THREAD_SCOPES.get());
            main.attachTo(ContextFactory.getGlobal());
            main.pack();
            main.setSize(960, 720);
            main.setVisible(true);
            return main;
        }
    };

    /** Context Factory to set wrap factory and opt level. */
    static class CirrusContextFactory extends ContextFactory {
        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setWrapFactory(WRAP_FACTORY);
            cx.setOptimizationLevel(DEBUG_JS ? -1 : 9);
            return cx;
        }
    }

    static {
        ContextFactory.initGlobal(new CirrusContextFactory());
    }

    @Override
    public void init() throws ServletException {
        staticInit();
    }

    /**
     * Perform init actions once per classloader.
     * Ensure DB is at correct version, if not run migrations.
     */
    private synchronized void staticInit() throws ServletException {
        if (STATIC_INIT) return;
        SERVLET_CONFIG = getServletConfig();

        // check if running in debug mode
        if (System.getProperty("debugjs") != null) {
            DEBUG_JS = true;
            DEBUGGERS.get();
        }

        // get datasource using 'dbname' servlet init-param
        try {
            InitialContext ic = new InitialContext();
            String dbname = getServletConfig().getInitParameter("dbname");
            log.info("Looking up datasource in jndi using 'dbname' servlet init-param: " + dbname);
            DATA_SOURCE = (DataSource) ic.lookup(dbname);
            // test
            Connection dbconn = DATA_SOURCE.getConnection();
            dbconn.close();
        } catch (Exception e) {
            log.error("Error getting dbconn", e);
            throw new ServletException("Error getting dbconn", e);
        }

        DB db = null;
        CirrusScope scope = THREAD_SCOPES.get();
        scope.getTimer().start();
        try {
            db = new DB(DATA_SOURCE);
            scope.put("DB", scope, db);
            scope.load("/db/migrate.js");
            scope.delete("DB");
        } catch (Exception e) {
            log.error("Error migrating db", e);
            throw new ServletException("Error migrating db", e);
        } finally {
            if (db != null) {
                db.close();
            }
            scope.getTimer().end("DB Migration");
        }
        STATIC_INIT = true;
    }

    /**
     * Forward requests to WEB-INF/app/cirrus.js.
     * Puts 'DB', {@link HttpServletRequest} as 'request', and
     * {@link HttpServletResponse} as 'response' into JS scope.
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            if (DEBUG_JS) {
                // launches Rhino Swing debugger attached to this thread / scope
                DEBUGGERS.get();
            }

            CirrusScope scope = THREAD_SCOPES.get();
            scope.getTimer().start();
            scope.load("/app/cirrus.js");
            // put request and response in global scope so they are accessible to controllers, models and views
            scope.put("request", scope, new NativeJavaObject(scope, req, HttpServletRequest.class));
            scope.put("response", scope, new NativeJavaObject(scope, res, HttpServletResponse.class));

            // set up DB
            DB db = new DB(DATA_SOURCE);
            scope.put("DB", scope, db);

            Context cx = Context.enter();
            try {
                // 'cirrus.service()'
                NativeObject cirrus = (NativeObject) scope.get("cirrus", scope);
                Function service = (Function) cirrus.get("service", cirrus);
                service.call(cx, scope, cirrus, new Object[0]);
            } finally {
                Context.exit();
                // close DB
                db.close();

                // don't keep reference to Servlet objects
                scope.delete("request");
                scope.delete("response");
                scope.getTimer().end(req.getMethod() + " " + req.getRequestURI());
            }
        } catch (Exception e) {
            log.error("Error running cirrus", e);
            throw new ServletException("Error running cirrus", e);
        }
    }
}