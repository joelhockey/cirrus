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

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

/**
 * Servlet based on jython's PyServlet. Manages ThreadLocal {@link CirrusScope} and dispatches reqs to js controller.
 *
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private static final long serialVersionUID = 0xD68B9CABE8D4D445L;
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
    private Set<String> publicFiles = new HashSet<String>();

    /** dispatch requests to appropriate controller. */
    @Override
   public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
       // parse path to find which js servlet to dispatch to
       String path = (String) req.getAttribute("javax.servlet.include.servlet_path");
       if (path == null) {
           path = ((HttpServletRequest)req).getServletPath();
           if (path == null || path.length() == 0) {
               // Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
               path = ((HttpServletRequest)req).getPathInfo();
           }
       }

       String controller = "pub"; // default controller
       String action = "index"; // default action
       String view = null;
       String[] pathdirs = path.split("/");
       if (pathdirs.length > 0 && pathdirs[0].length() == 0) { // skip first empty value
           pathdirs = Arrays.copyOfRange(pathdirs, 1, pathdirs.length);
       }
       if (pathdirs.length > 0 && pathdirs[0].length() != 0) {
           controller = pathdirs[0];
       }
       if (pathdirs.length > 1 && pathdirs[1].length() != 0) {
           action = pathdirs[1];
           view = action;
       }
       if (publicFiles.contains(controller) || controller == null) {
           // use pub controller for any public files
           controller = "pub";
           view = null;
       }

       // scope is thread local
       CirrusScope scope = localScope.get();
       // get controller servlet from cache
       String jsfile = "/WEB-INF/app/controllers/" + controller + ".js";
       scope.load(jsfile); // reload if file modified
       Object jsServlet = scope.get(controller, scope);
       if (jsServlet == Scriptable.NOT_FOUND) {
           throw new ServletException("Controller not found in " + jsfile);
       }
       HttpServlet servlet = (HttpServlet)((Wrapper)jsServlet).unwrap();

       // put variables in global scope to be used by controllers, models, and views
       // add params as native object
       NativeObject params = new NativeObject();
       for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
           String key = en.nextElement();
           params.put(key, params, req.getParameter(key));
       }
       Context cx = Context.enter();
       try {
           scope.put("req", scope, req);
           scope.put("res", scope, res);
           scope.put("path", scope, path);
           scope.put("pathdirs", scope, new NativeJavaArray(scope, pathdirs));
           scope.put("controller", scope, controller);
           scope.put("action", scope, action);
           scope.put("view", scope, view);
           scope.put("params", scope, params);
           // execute controller
           servlet.service(req, res);
           Object jsView = scope.get("view", scope);
           if (jsView != ScriptableObject.NOT_FOUND &&  jsView != null) {
               view = (String) jsView;
               jsfile = "/WEB-INF/app/views/" + controller + "/" + view + ".jst";
               NativeObject template = (NativeObject) scope.template(jsfile);
               Function process = (Function) template.get("process", template);
               process.call(cx, scope, template, new Object[] {jsServlet, null, new NativeJavaObject(scope, res.getWriter(), null)});
           }
       } finally {
           Context.exit();
           // remove objects from scope
           scope.delete("params");
           scope.delete("req");
           scope.delete("res");
       }
   }

    public void destroy() {
        localScope.remove();
    }
}