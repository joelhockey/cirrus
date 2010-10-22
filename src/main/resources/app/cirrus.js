// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

/**
 * Dispatch request.
 * Existing global vars:
 * - request HttpServletRequest
 * - response HttpServletResponse
 * Create global vars (they aren't really that evil):
 * - flash
 * - method
 * - path
 * - params
 * - pathdirs
 * - controller
 * - action
 */
var cirrus = function() {
    flash = {};
    method = String(request.getMethod());
    params = {};
    for (var en = request.getParameterNames(); en.hasMoreElements();) {
        var key = en.nextElement();
        params[String(key)] = String(request.getParameter(key));
    }

    path = String(request.getRequestURI());
    pathdirs = path.split("/");
    controller = pathdirs[1];
    action = pathdirs[2] || "index";
    
    // use public controller if no matches or file is in public
    if (!controller || cirrus.publicPaths[controller]) {
        controller = "public";
    } else {
        load("/app/controllers/" + controller + ".js");
    }

    try {
        var ctlr = controllers[controller];

        if (!ctlr) {
            logwarn("warning, no controller defined for path: " + path);
            throw 404; // not found
        }

        // call before
        if (ctlr.before && ctlr.before() === false) {
            return; // return early, request fully serviced
        }

        // check 'If-Modified-Since' vs 'Last-Modified' and return 304 if possible
        if (method === "GET" && ctlr.getLastModified) {
            var pageLastMod = ctlr.getLastModified()
            if (pageLastMod >= 0) {
                if (pageLastMod - request.getDateHeader("If-Modified-Since") < 1000) {
                    response.setStatus(304);
                    return; // early exit
                } else {
                    if (!response.containsHeader("Last-Modified") && pageLastMod >= 0) {
                        response.setDateHeader("Last-Modified", pageLastMod)
                    }
                }
            }        
        }
    
        // find method handler or 405
        var methodHandler = ctlr[method] || ctlr.$;
        if (methodHandler) {
            var actionHandler = methodHandler[action] || methodHandler.$;
            var args = pathdirs.slice(3);
            if (actionHandler instanceof Function && actionHandler.arity === args.length) {
                actionHandler.apply(ctlr, args);
            } else {
                logwarn("warning, no action handler for path: " + path + " got arity: " + actionHandler.arity);
                throw 404;
            }
        } else {
            // return 405 Method Not Allowed
            logwarn("warning, no method handler for path: " + path);
            response.addHeader("Allow", [m for each (m in "OPTIONS,GET,HEAD,POST,PUT,DELETE,TRACE".split(",")) if (ctlr[m])].join(", "));
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
        response.setStatus(status);
        if (status >= 400) { // only show error page for 4xx, 5xx
            jst("errors", String(status));
        }
    } finally {
        ctlr && ctlr.after && ctlr.after();
    }
}

// set publicPaths - this lets us know when we have static files to serve
cirrus.publicPaths = {};
getResourcePaths("/public/").forEach(function(path) {
    var part = path.split("/")[2];
    log("public path: " + part)
    cirrus.publicPaths[part] = part;
});

// controllers
var controllers = controllers || {};
// public controller serves static content in /public/
controllers["public"] = {
    getLastModified: function () {
        return fileLastModified("/public" + path);
    },
    GET: {
        index: function() {
            response.sendRedirect("/login");
        },
        $: function () {
            try {
                // set Content-Type
                var contentType = sconf.getServletContext().getMimeType(path);
                response.setContentType(contentType);
                log("using Content-Type: " + contentType + ", for file: " + path);
                readFile("/public" + path, response.getOutputStream());
            } catch (e) {
                logwarn("error sending static file: " + path, e);
                throw 404;
            }
        }
    }
}
