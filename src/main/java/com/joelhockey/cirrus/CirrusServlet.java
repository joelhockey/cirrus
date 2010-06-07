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
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;

/**
 * Main servlet for cirrus.  Copied some ideas from jython's PyServlet.
 * Manages ThreadLocal {@link CirrusScope} and dispatches to /WEB-INF/app/cirrus.js
 *
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final Log log = LogFactory.getLog(CirrusServlet.class);
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

    /**
     * Forward requests to WEB-INF/app/cirrus.js.
     * Puts variables, path, method and params (NativeObject), publicFiles (NativeObject) into global scope.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            CirrusScope scope = localScope.get();
            if (scope.load("/WEB-INF/app/cirrus.js")) {
                NativeObject publicFiles = new NativeObject();
                for (File dir : new File(getServletContext().getRealPath("/WEB-INF/public")).listFiles()) {
                    if (dir.isDirectory()) {
                        publicFiles.put(dir.getName(), scope, true);
                    }
                }
                scope.put("publicFiles", scope, publicFiles);
            }

            // parse path to find which js servlet to dispatch to
            String path = (String) req.getAttribute("javax.servlet.include.servlet_path");
            if (path == null) {
                path = ((HttpServletRequest)req).getServletPath();
                if (path == null || path.length() == 0) {
                    // Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
                    path = ((HttpServletRequest)req).getPathInfo();
                }
            }
            scope.put("path", scope, path);
            scope.put("method", scope, ((HttpServletRequest) req).getMethod());

            NativeObject params = new NativeObject();
            for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
                String key = en.nextElement();
                params.put(key, params, req.getParameter(key));
            }
            scope.put("params", scope, params);
            scope.put("req", scope, new NativeJavaObject(scope, req, HttpServletRequest.class));
            scope.put("res", scope, new NativeJavaObject(scope, res, HttpServletResponse.class));

            Function cirrus = (Function) scope.get("cirrus", scope);
            Context cx = Context.enter();
            try {
            	cx.setOptimizationLevel(9);
                cirrus.call(cx, scope, scope, new Object[] {req, res});
            } finally {
                Context.exit();
                scope.delete("req");
                scope.delete("res");
            }
        } catch (Exception e) {
            log.error("error loading cirrus", e);
            throw new ServletException("Could not load cirrus", e);
        }
    }
}