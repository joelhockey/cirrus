// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

/**
 * Migrate database to specified version
 * @param version version to migrate to
 */
cirrus.migrate = function(version) {
    // create timer, and DB object
    var timer = new com.joelhockey.cirrus.Timer();
    timer.start();
    cirrus.db.open();
    try {
        var dbversion;
        try {
            // get current version from 'db_version' table
            dbversion = cirrus.db.selectInt("select max(version) from db_version");
        } catch (e) {
            // error reading from 'db_version' table, try init script
            this.logwarn("Error getting dbversion, will try and load init script: ", e.toString());
            var sql = this.readFile("/db/000_init.sql");
            cirrus.db.execute(sql);
            cirrus.db.insert("insert into db_version (version, filename, script) values (0, '000_init.sql', ?)", [sql]);
            timer.mark("db init");
            dbversion = cirrus.db.selectInt("select max(version) from db_version");
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
        this.log("db migration: " + msg);
        
        // look in dir '/db/migrate' to find required files
        var files = this.getResourcePaths("/db/migrate/") || [];
        if (!files || files.length === 0) {
            throw new java.sql.SQLException("No files found in /db/migrate/");
        }
        this.log("files in '/db/migrate/'", files)
        var fileMap = {};
        var maxVersion = 0;
        var match;
        for (var file in files) {
            // check for filename format <nnn>_<desc>.sql
            if (match = /^\/db\/migrate\/(\d{3})_.*\.sql$/.exec(file)) {
                var filenum = parseInt(match[1]);
                maxVersion = Math.max(maxVersion, filenum);
                if (filenum > dbversion) {
                    // check for duplicates
                    if (fileMap[filenum]) {
                        throw new java.sql.SQLException(
                                "Found duplicate file for migration: "
                                + fileMap[filenum] + ", " + file);
                    }
                    fileMap[filenum] = file;
                }
            }
        }
        
        // if version not provided, set to max version found
        if (version === undefined) {
            this.logwarn("db migrate target version not provided, "
                    + "using max value found: " + maxVersion);
            version = maxVersion
        }
        
        // ensure all files exist
        for (var i = dbversion + 1; i <= version; i++) {
            if (!fileMap[i]) {
                throw new java.sql.SQLException("Migrating from: " + dbversion 
                    + " to: " + version + ", missing file: "
                    + i + ", got files: " + JSON.stringify(fileMap));
            }
        }
        
        timer.mark("check version");
        // run scripts
        for (var i = dbversion + 1; i <= version; i++) {
            this.log("db migration running script: " + fileMap[i]);
            var sql = this.readFile(fileMap[i]);
            cirrus.db.insert("insert into db_version (version, filename, script) values (?, ?, ?)", [i, fileMap[i], sql]);
            cirrus.db.execute(sql);
            timer.mark(fileMap[i]);
        }
    } finally {
        cirrus.db.close();
        timer.end("DB migration");
    }
};

/**
 * Main cirrus entry point to service HTTP requests.
 * @param request javax.servlet.http.HttpServletRequest
 * @param response com.joelhockey.cirrus.CirrusHttpServletResponse
 */
cirrus.service = function(request, response) {
    var env = new this.Env();
    // create timer, and DB object
    env.timer = new com.joelhockey.cirrus.Timer();
    env.timer.start();
    cirrus.db.open(); // close in finally
    try {
        env.request = request;
        env.response = response;
        env.flash = {};
        env.method = String(env.request.getMethod());
        env.path = String(env.request.getRequestURI());
        env.params = {};
        for (var en = env.request.getParameterNames(); en.hasMoreElements();) {
            var paramName = en.nextElement();
            env.params[paramName] = request.getParameter(paramName);
        }
        
        env.status = null; // optionally used for response
        env.body = []; // optionally used for response
        env.headers = {}; // optionally used for response
        var result = cirrus.forward(env);

        // processing may cause env.status, env.headers, env.body to be set
        // result may be either String (for response body) or
        //   {status: Number, headers: Object, body: Array}
        // if result has values, then merge them into env
        // and put env values (if any) into servlet response
        if (typeof result === "string") {
            env.body.push(result);
        } else if (result) {
            if (result.status) {
                env.status = result.status;
            }
            // merge result into env
            for (var headerName in result.headers) {
                env.headers[headerName] = result.headers[headerName];
            }
            if (typeof result.body === "string") {
                env.body.push(result.body)
            } else {
                Array.prototype.push.apply(env.body, result.body);
            }
        }

        // put env.status, env.headers, env.body into servlet response
        if (env.status) {
            response.setStatus(env.status);
        }
        for (var headerName in env.headers) {
            response.addHeader(headerName, env.headers[headerName]);
        }
        if (env.body.length > 0) {
            var writer = response.getWriter();
            for each (var part in env.body) {
                writer.write(part);
            }
        }
    } finally {
        cirrus.db.close();
        env.timer.end(env.method + " " + env.path
                + " " + response.getStatus());
    }
}

/** 
 * Method and path are optional to override env.method and env.path
 * Adds controller and action props to env.
 * @param env environment
 * @param requestLine optional HTTP Request-Line - e.g. 'GET /user/list'
 */
cirrus.forward = function(env, requestLine) {
    var method, path;
    // read method and path from requestLine if provided
    if (requestLine) {
        var methodPath = requestLine.split(" ");
        method = methodPath[0];
        path = methodPath[1];
    }
    
    // use values from env if not provided in requestLine
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
            this.logwarn("warning, could not load ctlr=" + env.controller
                    + ", path=" + path + ", error=" + e.message);
            throw 404;
        }

        // call before if exists - will throw exception to stop processing
        ctlr.before && ctlr.before.call(env);

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
        if (!methodHandler) {
            // return 405 Method Not Allowed
            this.logwarn("warning, no handler in ctlr=" + env.controller 
                    + " for method=" + method + ", path=" + path);
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
        this.response.setContentType("text/html; charset=UTF-8");
        template.render(this.response.getWriter(), context);
    } finally {
        this.timer.mark("view");
    }
};
