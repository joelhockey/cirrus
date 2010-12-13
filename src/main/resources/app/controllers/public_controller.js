// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

// public controller serves static content in /public/
cirrus.controllers["public"] = {
    getLastModified: function () {
        return fileLastModified("/public" + path);
    },
    GET: {
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
};
