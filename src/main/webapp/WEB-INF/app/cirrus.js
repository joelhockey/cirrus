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
var publicFiles = null // gets set by CirrusServlet as HashSet

var cirrus = function(sconf, req, res) {
    var path = req.getAttribute("javax.servlet.include.servlet_path");
    if (path == null) {
        path = req.getServletPath()
        if (path == null || path.length() == 0) {
        	// Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
        	path = req.getPathInfo()
        }
    }
    var controller = 'pub' // default controller (check later for public files)
    var action = 'index' // default action
    var parts = path.split('/')
    if (parts.length > 0 && parts[0] == '') { parts = parts.splice(1) } // skip first empty value
    if (parts.length > 0 && parts[0] != '') { controller = parts[0] }
    if (parts.length > 1 && parts[1] != '') { view = parts[1] }
    if (publicFiles.contains(controller)) { // publicFiles is HashSet
        controller = 'pub'
    }

//    print('controller: ' + controller + ', action: ' + action + ', path: ' + path)
    if (load('/WEB-INF/app/controllers/' + controller + '.js')) {
    	this[controller].init(sconf)
    }
    var servlet = this[controller]
    req.setAttribute('com.joelhockey.cirrus.path', path)
    req.setAttribute('com.joelhockey.cirrus.path_parts', parts)
    req.setAttribute('com.joelhockey.cirrus.controller', controller)
    req.setAttribute('com.joelhockey.cirrus.action', action)
    servlet.service(req, res)
    
    if (req.getAttribute('com.joelhockey.cirrus.view') != null) {
        servlet.req = req
        servlet.res = res
        var tmpl = template('/WEB-INF/app/views/' + controller + '/' + view + '.jst')
        tmpl.process(servlet, null, res.getWriter())
        delete servlet.req
        delete servlet.res
    }
}
