// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

// anonymous function to keep global scope clean
(function() {

    // read required version from servlet init-param
    var param = servletConfig.getInitParameter("dbversion");
    var version = parseInt(param);
    if (isNaN(version)) {
        throw new java.sql.SQLException(
                "Invalid 'dbversion' servlet init-param: " + param);
    }

    var dbversion;
    try {
        // get current version from 'db_version' table
        dbversion = DB.selectInt("select max(version) from db_version");
    } catch (e) {
        // error reading from 'db_version' table, try init script
        logwarn("Error getting db version, will try and load init script: ", e.toString());
        var sql = readFile("/db/000_init.sql");
        DB.execute(sql);
        DB.insert("insert into db_version (version, filename, script) values (0, '000_init.sql', ?)", [sql]);
        timer.mark("init");
        dbversion = DB.selectInt("select max(version) from db_version");
    }
    
    // check if up to date
    var msg = "db at version: " + dbversion + ", app requires version: " + version;
    if (dbversion === version) {
        log("db version ok.  " + msg);
        return;
    } else if (dbversion > version) { // very strange
        throw new java.sql.SQLException(msg);
    }
    
    // move from dbversion to version
    log("doing db migration.  " + msg);

    // look in dir /db to find required files
    var files = getResourcePaths("/db/") || [];
    if (!files || files.length === 0) {
        throw new java.sql.SQLException("No files found in /db/");
    }
    log("files in '/db/':", files)
    var fileMap = {};
    for (var file in files) {
        // check for filename format <nnn>_<desc>.sql
        var match;
        if (match = /^\/db\/(\d{3})_.*\.sql$/.exec(file)) {
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

    timer.mark("check version");
    // run scripts
    for (var i = dbversion + 1; i <= version; i++) {
        log("db migration running script: " + fileMap[i]);
        var sql = readFile(fileMap[i]);
        DB.insert("insert into db_version (version, filename, script) values (?, ?, ?)", [i, fileMap[i], sql]);
        DB.execute(sql);
        timer.mark(fileMap[i]);
    }
})()
