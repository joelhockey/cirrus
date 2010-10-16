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

var JSON = new com.joelhockey.cirrus.RhinoJSON(global());
var CONTROLLERS = CONTROLLERS || {};
var LIB = LIB || {};
var MODELS = MODELS || {};
var MIME_TYPES = {
        css: "text/css",
        html: "text/html",
        ico: "application/x-icon",
        js: "application/x-javascript",
        jpeg: "image/jpeg",
        wsdl: "text/xml",
        xml: "text/xml",
        xsd: "text/xml",
};

// variables already injected into global namespace by CirrusServlet:
// * method - String
// * path - String
// * params - NativeObject with params
// * PUBLIC_ROOT - NativeObject with files, dirs in root of public dir.
// This method adds strings: pathdirs, controller, action
var cirrus = function() {
    // put variables 'flash', 'pathdirs', 'controller', 'action' in global scope
    flash = {};
    pathdirs = path.substring(1).split("/");
    controller = pathdirs[0] || "public";
    var ctlr = CONTROLLERS[controller];
    action = pathdirs[1] || "index";
    if (PUBLIC_ROOT[controller]) {
        // use pub controller for any public files
        log("using public controller for " + path);
        controller = "public";
        action = null;
    }

    logf("method.controller.action: %s.%s.%s, path: %s", method, controller, action, path);
    var ctlr = CONTROLLERS[controller];
    try {
        if (load("/WEB-INF/app/controllers/" + controller + ".js")) {
            ctlr = CONTROLLERS[controller];
        }
    } catch (e) {
        logerror("error loading controller: " + controller + ", path: " + path, e);
    }

    try {
        if (!ctlr) {
            logwarn("warning, no controller defined for path: " + path);
            throw 404;
        }

        // call before
        if (ctlr.before && ctlr.before() === false) {
            return;
        }
    
        // check 'If-Modified-Since' vs 'Last-Modified' and return 304 if possible
        if (method === "GET" && ctlr.getLastModified) {
            var pageLastMod = ctlr.getLastModified()
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
    
        // find method handler or 405
        var methodHandler = ctlr[method] || ctlr.$;
        if (methodHandler) {
            var actionHandler = methodHandler[action] || methodHandler.$;
            var args = pathdirs.slice(2);
            if (actionHandler instanceof Function && actionHandler.arity === args.length) {
                actionHandler.apply(ctlr, pathdirs.slice(2));
            } else {
                logwarn("warning, no action handler for path: " + path + " got arity: " + actionHandler.arity);
                throw 404;
            }
        } else {
            // return 405 Method Not Allowed
            logwarn("warning, no method handler for path: " + path);
            res.addHeader("Allow", [m for each (m in "OPTIONS,GET,HEAD,POST,PUT,DELETE,TRACE".split(",")) if (ctlr[m])].join(", "));
            throw 405;
        }
        
    // error - set status and use error templates
    } catch (e) {
        var status = 500;
        if (typeof e === "number") {
            status = e;
        } else {
            logerror("internal server error", e);
        }
        res.setStatus(status);
        if (status >= 400) { // only show error page for 4xx, 5xx
            jst("errors", String(status));
        }
    } finally {
        ctlr && ctlr.after && ctlr.after();
    }
}
