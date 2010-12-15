// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// publicPaths contains all dirs and files in public root
// if first part of path matches one of these, then we use public controller
cirrus.publicPaths = {};
for (var path in cirrus.getResourcePaths("/public/")) {
    var part = path.split("/")[2];
    cirrus.log("public path: " + part)
    cirrus.publicPaths[part] = part;
}

/**
 * Dispatch request.
 * cirrus props:
 * - servletConfig javax.servlet.ServletConfig
 * - servletContext javax.servlet.ServletContext
 * - timer com.joelhockey.cirrus.Timer
 * - controllers Object
 * env props:
 * - db com.joelhockey.cirrus.DB
 * - request javax.servlet.http.HttpServletRequest
 * - response javax.servlet.http.HttpServletResponse
 * Create env props:
 * - flash
 * - method
 * - path
 * - params
 * - controller
 * - action
 */
cirrus.service = function(env) {
    env.flash = {};
    env.method = String(env.request.getMethod());
    env.params = {};
    for (var en = env.request.getParameterNames(); en.hasMoreElements();) {
        var key = en.nextElement();
        env.params[String(key)] = String(env.request.getParameter(key));
    }

    env.path = String(env.request.getRequestURI());
    
    var pathdirs = path.split("/");
    // use 'index' as default controller and action
    env.controller = pathdirs[1] || "index";
    env.action = pathdirs[2] || "index";
    
    // use public controller if path is in public dir
    if (cirrus.publicPaths[env.controller]) {
        env.controller = "public";
    }

    var ctlr;
    try {
        try {
            cirrus.load("/app/controllers/" + env.controller + "_controller.js");
            ctlr = cirrus.controllers[env.controller];
            if (!ctlr) {
                throw null;
            }
        } catch (e) {
            cirrus.logwarn("warning, no controller defined for path: " + env.path);
            throw 404;
        }

        // call before
        if (ctlr.before && ctlr.before.call(env) === false) {
            return; // return early, request fully serviced
        }

        // check 'If-Modified-Since' vs 'Last-Modified' and return 304 if possible
        if (this.method === "GET" && ctlr.getLastModified) {
            var pageLastMod = ctlr.getLastModified()
            if (pageLastMod >= 0) {
                if (pageLastMod - this.request.getDateHeader("If-Modified-Since") < 1000) {
                    this.response.setStatus(304); // Not Modified
                    return; // early exit
                } else {
                    if (!this.response.containsHeader("Last-Modified") && pageLastMod >= 0) {
                        this.response.setDateHeader("Last-Modified", pageLastMod)
                    }
                }
            }
        }
    
        // find method handler or 405
        var methodHandler = ctlr[this.method] || ctlr.$;
        if (!methodHandler) {
            // return 405 Method Not Allowed
            this.logwarn("warning, no method handler for path: " + path);
            this.response.addHeader("Allow", [m for each (m in "OPTIONS,GET,HEAD,POST,PUT,DELETE,TRACE".split(",")) if (ctlr[m])].join(", "));
            throw 405;
        }
        var actionHandler = methodHandler[this.action] || methodHandler.$;
        var args = pathdirs.slice(3);
        if (!(actionHandler instanceof Function) || actionHandler.arity !== args.length) {
            this.logwarn("warning, no action handler for path: " + path + " got arity: " + actionHandler.arity);
            throw 404;
        }
        actionHandler.apply(ctlr, args);

    // error - set status and use error templates
    } catch (e) {
        var status = 500;
        if (typeof e === "number") {
            status = e;
        } else {
            this.logerror("internal server error", e);
        }
        this.response.setStatus(status);
        if (status >= 400) { // only show error page for 4xx, 5xx
            jst("errors", String(status), this);
        }
    } finally {
        ctlr && ctlr.after && ctlr.after();
    }
};
