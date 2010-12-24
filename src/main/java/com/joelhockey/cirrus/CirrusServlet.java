// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.debugger.Main;

/**
 * Main servlet for cirrus. Dispatches to JavaScript
 * 'cirrus.service(request, response)'
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final long serialVersionUID = 0x26FAB6AD9ECB6BDCL;
    private static final Log log = LogFactory.getLog(CirrusServlet.class);

    private static boolean STATIC_INIT = false;
    public static ScriptableObject GLOBAL_SCOPE;
    private static Cirrus CIRRUS;

    static {

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

        // check if running in debug mode
        if (System.getProperty("debugjs") != null) {
            // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            Main main = new Main("Cirrus Debug");
            main.attachTo(ContextFactory.getGlobal());
            main.pack();
            main.setSize(960, 720);
            main.setVisible(true);
        }

        Context cx = Context.enter();
        cx.setWrapFactory(Cirrus.WRAP_FACTORY); // use cirrus WrapFactory

        try {
            GLOBAL_SCOPE = new ImporterTopLevel(cx);
            CIRRUS = new Cirrus(GLOBAL_SCOPE, getServletConfig());

            // test cnxn
            DB db = CIRRUS.getDB();
            db.open();
            db.close();

            // get dbversion from servlet init-param
            String dbversion = getServletConfig().getInitParameter("dbversion");
            log.info("servlet init-param dbversion=" + dbversion);

            // cirrus.migrate(dbversion)
            Function migrate = (Function) CIRRUS.get("migrate", CIRRUS);
            migrate.call(cx, GLOBAL_SCOPE, CIRRUS, new Object[] {dbversion});
        } catch (Exception e) {
            log.error("Error initialising cirrus", e);
            throw new ServletException("Error initialising cirrus", e);
        } finally {
            Context.exit();
        }

        STATIC_INIT = true;
    }

    /**
     * Calls JavaScript 'cirrus.service(request, response)'
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {


        try {
            Context cx = Context.enter();
            cx.setWrapFactory(Cirrus.WRAP_FACTORY); // use cirrus WrapFactory

            CIRRUS.load("/app/cirrus.js"); // might have changed

            try {
                Function service = (Function) CIRRUS.get("service", CIRRUS);
                service.call(cx, CIRRUS, CIRRUS, new Object[] { req, res });
            } finally {
                Context.exit();
            }
        } catch (Exception e) {
            log.error("Error running cirrus", e);
            throw new ServletException("Error running cirrus", e);
        }
    }
}