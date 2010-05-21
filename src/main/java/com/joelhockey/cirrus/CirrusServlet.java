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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

/**
 * Servlet based on jython's PyServlet. Creates separate instance of {@link CirrusScope} for each thread and forwards
 * all requests to WEB-INF/app/cirrus.js.
 *
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final Log log = LogFactory.getLog(CirrusServlet.class);
    private static final String PUBLIC_FILES_VAR = "publicFiles";
    private static final String PUBLIC_DIR = "/WEB-INF/public";
    private ThreadLocal localScope = new ThreadLocal();
    private Set<String> publicFiles = new HashSet<String>();
    private Map<String, HttpServlet> servletCache = new HashMap<String, HttpServlet>();

    /** Forward requests to WEB-INF/app/cirrus.js. */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String controller = null;
        Context cx = Context.enter();
        try {
            // scope is thread local
            Object[] o = (Object[]) localScope.get();
            if (o == null) {
                o = new Object[] { new CirrusScope(cx, getServletConfig()) };
                localScope.set(o);
            }
            CirrusScope scope = (CirrusScope) o[0];

            // parse path to find which js servlet to dispatch to
            String path = (String) req.getAttribute("javax.servlet.include.servlet_path");
            if (path == null) {
                path = ((HttpServletRequest)req).getServletPath();
                if (path == null || path.length() == 0) {
                    // Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
                    path = ((HttpServletRequest)req).getPathInfo();
                }
            }

            String action = "index"; // default action
            String[] pathdirs = path.split("/");
            if (pathdirs.length > 0 && pathdirs[0].length() == 0) { // skip first empty value
                pathdirs = Arrays.copyOfRange(pathdirs, 1, pathdirs.length);
            }
            if (pathdirs.length > 0 && pathdirs[0].length() != 0) {
                controller = pathdirs[0];
            }
            if (pathdirs.length > 1 && pathdirs[1].length() != 0) {
                action = pathdirs[1];
            }
            if (publicFiles.contains(controller) || controller == null) { // use pub controller for any public files
                controller = "pub";
            }

            // put variables in global scope to be used by controllers, models, and views
            scope.put("req", scope, req);
            scope.put("res", scope, res);
            scope.put("path", scope, path);
            scope.put("pathdirs", scope, Context.javaToJS(pathdirs, scope));
            scope.put("controller", scope, controller);
            scope.put("action", scope, action);

            // add params as native object
            NativeObject params = new NativeObject();
            for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
                String key = en.nextElement();
                params.defineProperty(key, req.getParameter(key), 0);
            }
            scope.put("params", scope, params);

            // get controller servlet from cache
            HttpServlet servlet = servletCache.get(controller);
            if (scope.load("/WEB-INF/app/controllers/" + controller + ".js")) {
                // if refresh, put into local map
                Object obj = scope.get(controller, scope);
                if (obj == Scriptable.NOT_FOUND) {
                    throw new Exception("Controller not found: " + controller);
                }
                servlet = (HttpServlet) cx.jsToJava(obj, HttpServlet.class);
                servletCache.put(controller, servlet);
            }

            try {
                // execute controller
                servlet.service(req, res);
            } finally {
                // remove objects from scope
                scope.delete("params");
                scope.delete("req");
                scope.delete("res");
            }
        } catch (Exception e) {
            log.error("Error running cirrus", e);
            throw new ServletException("Error running cirrus controller: " + controller, e);
        } finally {
            Context.exit();
        }
    }

    public void destroy() {
        localScope.set(null);
    }
}