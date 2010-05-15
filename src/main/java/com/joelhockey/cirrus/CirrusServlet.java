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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

/**
 * Servlet based on jython's PyServlet.  Creates separate instance of
 * {@link CirrusScope} for each thread and forwards all requests to
 * WEB-INF/app/cirrus.js.
 * 
 * @author Joel Hockey
 */
public class CirrusServlet extends HttpServlet {
    private CirrusScope scope;
    private Function cirrus;

    /**
     * Create new {@link CirrusScope} for each thread and initialise.
     */
    @Override
    public void init() throws ServletException {
        Context cx = Context.enter();
        try {
            scope = new CirrusScope(cx, getServletConfig());
        } finally {
            cx.exit();
        }
    }

    /**
     * Forward requests to WEB-INF/app/cirrus.js.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            if (scope.load("/WEB-INF/app/cirrus.js")) {
                cirrus = (Function) scope.get("cirrus", scope);
            }
            Context cx = Context.enter();
            try {
                cirrus.call(cx, scope, scope, new Object[] {getServletConfig(), req, res});
            } finally {
                Context.exit();
            }
        } catch (Exception e) {
            throw new ServletException("Could not load cirrus", e);
        }
    }
}
