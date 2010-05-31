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

var ControllerPrototype = {
	getLastModified : function(req) { return -1; },
    before : function(req, res) { return true; },
    after : function(req, res) {},
    options : function(req, res) {
		res.addHeader("Allow", [m.toUpperCase() for each (m in "options,get,head,post,put,delete,trace".split(",")) if (this[m])].join(", "));
    },
    trace : function(req, res) {
    	var body = "TRACE " + req.getRequestURI() + " " + req.getProtocol() + "\r\n" +
		    [key + ": " + params[key] for each (key in params)].join("\r\n")
    	res.setContentType("message/http");
        resp.setContentLength(body.length);
    	res.getWriter().write(body);
    }
}

function cirrus(req, res) {
	// variables injected by CirrusServlet:
	// params - NativeObject with params
	// publicFiles - NativeObject with public directories
	// path - String
	// method - String

	// put variables 'action', 'pathdirs', 'controller', in global scope
	// to allow access from template
    action = null;
    pathdirs = path.split("/");
    controller = pathdirs[0];
    if (!controller) {
    	pathdirs.shift();
    	controller = pathdirs[0];
    }
    action = pathdirs[1]
    if (!action) {
    	action = "index";
    }
    if (!controller || publicFiles[controller]) {
        // use pub controller for any public files
        controller = "pub";
    }

//    print("controller: " + controller + ", action: " + action + ", path: " + path)
    var ctlr = this[controller]
    if (load("/WEB-INF/app/controllers/" + controller + ".js")) {
    	ctlr = this[controller];
    	// copy ControllerPrototype
    	for (var f in ControllerPrototype) {
    		if (typeof ctlr[f] == "undefined") {
    			ctlr[f] = ControllerPrototype[f];
    		}
    	}
    }

    if (!ctlr) {
        log.warn("no controller defined for path: " + path);
    	res.setStatus(404);
    	return;
    }

    if (!ctlr.before(req, res)) {
    	// before stopped processing
    	return;
    }
    
    // check 'If-Modified-Since' vs 'Last-Modified' and return 304 if possible
    if (method == "GET") {
    	var pageLastMod = ctlr.getLastModified(req)
    	if (pageLastMod >= 0) {
            if (pageLastMod - req.getDateHeader("If-Modified-Since") < 1000) {
                res.setStatus(304);
                return; // early exit
            } else {
            	if (!res.containsHeader("Last-Modified") && pageLastMod >= 0) {
            		res.setDateHeader("Last-Modified", pageLastMod)
            	}
            }
        }    	
    }

    // check controller for function matching 'action'
    var f = ctlr[action];
    if (f instanceof Function) {
    	f.call(ctlr, req, res);
    	
    // else fall back on function from HTTP method
    } else {
    	f = ctlr[method.toLowerCase()]
    	if (f instanceof Function) {
    		f.call(ctlr, req, res);
    	} else {
    		// return 405 Method Not Allowed
    		res.setStatus(405);
    		res.addHeader("Allow", [m.toUpperCase() for each (m in "options,get,head,post,put,delete,trace".split(",")) if (ctlr[m])].join(", "));
    	}
    }
    
    ctlr.after(req, res);
}
