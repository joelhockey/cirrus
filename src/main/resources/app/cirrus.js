// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

var cirrus = cirrus || {controllers: {}};

/**
 * Dispatch request.
 * Existing global vars:
 * - servletConfig javax.servlet.ServletConfig
 * - servletContext javax.servlet.ServletContext
 * - request javax.servlet.http.HttpServletRequest
 * - response javax.servlet.http.HttpServletResponse
 * Create global vars:
 * - flash
 * - method
 * - path
 * - params
 * - controller
 * - action
 */
cirrus.service = function() {
    flash = {};
    method = String(request.getMethod());
    params = {};
    for (var en = request.getParameterNames(); en.hasMoreElements();) {
        var key = en.nextElement();
        params[String(key)] = String(request.getParameter(key));
    }

    path = String(request.getRequestURI());
    var pathdirs = path.split("/");
    controller = pathdirs[1];
    action = pathdirs[2] || "index";
    
    // use public controller if no matches or file is in public
    if (!controller || cirrus.publicPaths[controller]) {
        controller = "public";
    } else {
        load("/app/controllers/" + controller + ".js");
    }

    try {
        var ctlr = this.controllers[controller];

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
                    response.setStatus(304); // Not Modified
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
            jst("errors", String(status), this);
        }
    } finally {
        ctlr && ctlr.after && ctlr.after();
    }
};

// publicPaths contains all dirs and files in public root
// if first part of path matches one of these, then we use public controller
cirrus.publicPaths = {};
getResourcePaths("/public/").forEach(function(path) {
    var part = path.split("/")[2];
    log("public path: " + part)
    cirrus.publicPaths[part] = part;
});

// public controller serves static content in /public/
cirrus.controllers["public"] = {
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
                var contentType = servletContext.getMimeType(path);
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

// add some global helpers
if (typeof Object.create !== "function") {
    Object.create = function(o) {
        function F() {}
        F.prototype = o;
        return new F();
    }
}
