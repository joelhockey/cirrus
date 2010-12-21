// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.IOException;
import java.sql.Connection;

import javax.naming.InitialContext;
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
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
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
    private static DataSource DATA_SOURCE;
    static CirrusScope GLOBAL_SCOPE;

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
        int dbversion;
        try {
            // get datasource using 'dbname' servlet init-param
            InitialContext ic = new InitialContext();
            String dbname = getServletConfig().getInitParameter("dbname");
            String dbversionStr = getServletConfig().getInitParameter("dbversion");
            log.info("servlet init-params dbname=" + dbname + ", dbversion="
                    + dbversionStr + ", looking up jndi for datasource");
            dbversion = Integer.parseInt(dbversionStr);
            DATA_SOURCE = (DataSource) ic.lookup(dbname);
            // test cnxn
            Connection dbconn = DATA_SOURCE.getConnection();
            dbconn.close();

            GLOBAL_SCOPE = new CirrusScope(getServletConfig());

            // check if running in debug mode
            if (System.getProperty("debugjs") != null) {
                // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
                Main main = new Main("Cirrus Debug");
                main.setScope(GLOBAL_SCOPE);
                main.attachTo(ContextFactory.getGlobal());
                main.pack();
                main.setSize(960, 720);
                main.setVisible(true);
            }
        } catch (Exception e) {
            log.error("Error getting dbconn", e);
            throw new ServletException("Error getting dbconn", e);
        }

        // put 'dataSource' in cirrus
        Cirrus cirrus = GLOBAL_SCOPE.getCirrus();
        cirrus.put("dataSource", cirrus, DATA_SOURCE);

        Context cx = Context.enter();
        cx.setWrapFactory(Cirrus.WRAP_FACTORY); // use cirrus WrapFactory

        try {
            // cirrus.migrate(env, dbversion)
            Function migrate = (Function) cirrus.get("migrate", cirrus);
            migrate.call(cx, GLOBAL_SCOPE, cirrus, new Object[] {dbversion});
        } catch (Exception e) {
            log.error("Error migrating db", e);
            throw new ServletException("Error migrating db", e);
        } finally {
            Context.exit();
        }
        STATIC_INIT = true;
    }

    /**
     * Creates 'env' JavaScript object, populates with request, response,
     * etc, and calls JavaScript 'cirrus.forward(env)'
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            Context cx = Context.enter();
            cx.setWrapFactory(Cirrus.WRAP_FACTORY); // use cirrus WrapFactory

            Cirrus cirrus = GLOBAL_SCOPE.getCirrus();
            cirrus.load("/app/cirrus.js");

            try {
                // 'cirrus.forward(env)'
                Function service = (Function) cirrus.get("service", cirrus);
                service.call(cx, cirrus, cirrus, new Object[] { req, res });
            } finally {
                Context.exit();
            }
        } catch (Exception e) {
            log.error("Error running cirrus", e);
            throw new ServletException("Error running cirrus", e);
        }
    }
}