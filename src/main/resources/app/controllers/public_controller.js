// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

// public controller serves static content in /public/
cirrus.controllers["public"] = {
    getLastModified: function () {
        return cirrus.fileLastModified("/public" + this.path);
    },
    GET: {
        $: function () {
            try {
                // set Content-Type
                var contentType = cirrus.servletContext.getMimeType(this.path);
                this.response.setContentType(contentType);
                cirrus.log("using Content-Type: " + contentType + ", for file: " + this.path);
                cirrus.readFile("/public" + this.path, this.response.getOutputStream());
            } catch (e) {
                cirrus.logwarn("error sending static file: " + this.path, e);
                throw 404;
            }
        }
    }
};
