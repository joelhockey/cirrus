// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

/**
 * Main cirrus entry point to service HTTP requests.
 * cirrus props:
 * - servletConfig javax.servlet.ServletConfig
 * - servletContext javax.servlet.ServletContext
 * - controllers Object
 * env props:
 * - db com.joelhockey.cirrus.DB
 * - request javax.servlet.http.HttpServletRequest
 * - response javax.servlet.http.HttpServletResponse
 * - timer com.joelhockey.cirrus.Timer
 * Create env props:
 * - flash
 * - method
 * - path
 * - params
 * - headers
 * - body
 * @param env environment
 */
cirrus.service = function(env) {
    env.flash = {};
    env.method = env.request.getMethod();
    env.path = env.request.getRequestURI();
    env.params = {};
    for (var en = env.request.getParameterNames(); en.hasMoreElements();) {
        var paramName = en.nextElement();
        params[paramName] = request.getParameter(paramName));
    }
    env.body = [];
    env.headers = {};
    var result = cirrus.forward(env);
    
    // result is either String, or {status: Number, headers: Object, body: Array}
    // merge result into env
    var status;
    if (typeof result === "string") {
        env.body.push(result);
    } else {
        status = result.status || env.status;
        for (var headerName in result.headers) {
            env.headers[headerName] = result[headerName];
        }
        Array.prototype.push.apply(env.body, result.body); 
    }
    
    // put env.status, env.headers, env.body into servlet
    if (!response.isStatusSet() && status) {
        response.setStatus(status);
    }
    for (var headerName in env.headers) {
        response.addHeader(headerName, env.headers[headerName]);
    }
    var writer = response.getWriter();
    for each (var bodyPart in env.body) {
        writer.write(bodyPart);
    }
}

/** 
 * Method and path are optional to override env.method and env.path
 * Adds controller and action props to env.
 * @param env environment
 * @param method optional method - e.g. GET, POST
 * @param path optional path - e.g. /user/list
 */
cirrus.forward = function(env, method, path) {
    method = method || env.method;
    path = path || env.path;
    
    var pathdirs = path.split("/");
    // use 'index' as default controller and action
    env.controller = pathdirs[1] || "index";
    env.action = pathdirs[2] || "index";
    
    // use public controller if path is in public dir
    if (this.publicPaths[env.controller]) {
        env.controller = "public";
    }

    var ctlr;
    try {
        try {
            this.load("/app/controllers/" + env.controller + "_controller.js");
            ctlr = cirrus.controllers[env.controller];
            if (!ctlr) {
                throw null;
            }
        } catch (e) {
            this.logwarn("warning, no controller defined for path: " + path);
            throw 404;
        }

        // call before
        if (ctlr.before && ctlr.before.call(env) === false) {
            return; // return early, request fully serviced
        }

        // check 'If-Modified-Since' vs 'Last-Modified' and return 304 if possible
        if (method === "GET" && ctlr.getLastModified) {
            var pageLastMod = ctlr.getLastModified.call(env)
            if (pageLastMod >= 0) {
                if (pageLastMod - env.request.getDateHeader("If-Modified-Since") < 1000) {
                    env.response.setStatus(304); // Not Modified
                    return; // early exit
                } else {
                    if (!env.response.containsHeader("Last-Modified") && pageLastMod >= 0) {
                        env.response.setDateHeader("Last-Modified", pageLastMod)
                    }
                }
            }
        }
    
        // find method handler or 405
        var methodHandler = ctlr[method] || ctlr.$;
        cirrus.log("ctlr", ctlr)
        if (!methodHandler) {
            // return 405 Method Not Allowed
            this.logwarn("warning, no handler in ctlr for method="
                    + method + ", path=" + path);
            var allowed = [m for each (m in "OPTIONS,GET,HEAD,POST,PUT,DELETE,TRACE".split(",")) if (ctlr[m])];
            env.response.addHeader("Allow", allowed.join(", "));
            throw 405;
        }
        var actionHandler = methodHandler[env.action] || methodHandler.$;
        var args = pathdirs.slice(3);
        if (!(actionHandler instanceof Function) || actionHandler.arity !== args.length) {
            this.logwarn("warning, no action handler for method=" + method
                    + ", action=" + env.action + ", path=" + path
                    + " got arity: " + (actionHandler && actionHandler.arity));
            throw 404;
        }
        return actionHandler.apply(env, args);

    // error - set status and use error templates
    } catch (e) {
        var status = 500;
        if (typeof e === "number") {
            status = e;
        } else {
            this.logerror("internal server error", e);
        }
        env.response.setStatus(status);
        if (status >= 400) { // only show error page for 4xx, 5xx
            env.jst("errors", String(status));
        }
    } finally {
        ctlr && ctlr.after && ctlr.after.call(env);
    }
};

// cirrus.publicPaths contains all dirs and files in public root
// if first path part or a req matches one of these, then we use public ctlr
(function () {
    cirrus.publicPaths = {};
    for (var path in cirrus.getResourcePaths("/public/")) {
        var part = path.split("/")[2];
        cirrus.log("public path: " + part)
        cirrus.publicPaths[part] = part;
    }
})();


/**
 * Migrate database to specified version
 * @param env environment containing db, timer.
 */
cirrus.migrate = function(env, version) {
    var dbversion;
    try {
        // get current version from 'db_version' table
        dbversion = env.db.selectInt("select max(version) from db_version");
    } catch (e) {
        // error reading from 'db_version' table, try init script
        this.logwarn("Error getting db version, will try and load init script: ", e.toString());
        var sql = this.readFile("/db/000_init.sql");
        env.db.execute(sql);
        env.db.insert("insert into db_version (version, filename, script) values (0, '000_init.sql', ?)", [sql]);
        env.timer.mark("db init");
        dbversion = env.db.selectInt("select max(version) from db_version");
    }
    
    // check if up to date
    var msg = "db at version: " + dbversion + ", app requires version: " + version;
    if (dbversion === version) {
        this.log("db version ok.  " + msg);
        return;
    } else if (dbversion > version) { // very strange
        throw new java.sql.SQLException(msg);
    }
    
    // move from dbversion to version
    this.log("doing db migration.  " + msg);
    
    // look in dir '/db/migrate' to find required files
    var files = this.getResourcePaths("/db/migrate/") || [];
    if (!files || files.length === 0) {
        throw new java.sql.SQLException("No files found in /db/migrate/");
    }
    this.log("files in '/db/migrate/':", files)
    var fileMap = {};
    for (var file in files) {
        // check for filename format <nnn>_<desc>.sql
        var match;
        if (match = /^\/db\/migrate\/(\d{3})_.*\.sql$/.exec(file)) {
            var filenum = parseInt(match[1]);
            if (filenum > dbversion && filenum <= version) {
                // check for duplicates
                if (fileMap[filenum]) {
                    throw new java.sql.SQLException("Found duplicate file for migration: " + fileMap[filenum] + ", " + files[i]);
                }
                fileMap[filenum] = file;
            }
        }
    }
    
    // ensure all files exist
    for (var i = dbversion + 1; i <= version; i++) {
        if (!fileMap[i]) {
            throw new java.sql.SQLException("Migrating from: " + dbversion + " to: " + version + ", missing file: "
                + i + ", got files: " + JSON.stringify(fileMap));
        }
    }
    
    env.timer.mark("check version");
    // run scripts
    for (var i = dbversion + 1; i <= version; i++) {
        this.log("db migration running script: " + fileMap[i]);
        var sql = this.readFile(fileMap[i]);
        env.db.insert("insert into db_version (version, filename, script) values (?, ?, ?)", [i, fileMap[i], sql]);
        env.db.execute(sql);
        env.timer.mark(fileMap[i]);
    }
};

/** Env is environment holding request, response, etc */
cirrus.Env = function() {};

/**
 * Render specified JST template.  Optional parameters are
 * ctlr, action and context.
 * @param arguments is either [], [action], [context], [ctlr, action],
 * [action, context], [ctlr, action, context]
 * If not specified, ctlr uses 'this.controller', action uses 'this.action'
 * and context uses 'this'.
 */
cirrus.Env.prototype.jst = function() {
    this.timer.mark("action");

    // shift args right twice, or until typeof args[1] is string
    // then we have [ctrl, action, context]
    var args = Array.prototype.slice.call(arguments);
    if (typeof args[1] !== "string") args.unshift(null);
    if (typeof args[1] !== "string") args.unshift(null);
    var controller = args[0] || this.controller;
    var action = args[1] || this.action;
    var context = args[2] || this;
    
    try {
        var template = cirrus.jst(controller + "." + action);
        this.response.setContentType("text/html");
        template.render(this.response.getWriter(), context);
    } finally {
        this.timer.mark("view");
    }
};
