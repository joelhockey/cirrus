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

var cirrus = function(sconf, req, res) {
    var spath = req.getAttribute("javax.servlet.include.servlet_path")
    if (spath == null) {
        spath = req.getServletPath()
        if (spath == null || spath.length() == 0) {
        	// Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
        	spath = req.getPathInfo()
        }
    }
    var parts = spath.split('/')
    var controller = parts[1]
    if (load("/WEB-INF/app/controllers/" + controller + ".js")) {
    	this[controller].init(sconf)
    }
    var servlet = this[controller]
    servlet.service(req, res)
    
    if (parts.length > 2) {
        servlet.req = req
        servlet.res = res
    	var tmpl = template("/WEB-INF/app/views/" + parts[2] + ".jst")
    	tmpl.process(servlet, null, res.getWriter())
    	delete servlet.req
    	delete servlet.res
    }
}
