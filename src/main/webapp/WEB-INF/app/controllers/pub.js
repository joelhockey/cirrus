pub = new javax.servlet.http.HttpServlet({
    getLastModified: function(req) { return lastModified(this.getPublicPath(req)) },
  
    doGet: function(req, res) {
        var path = this.getPublicPath(req)
        res.setDateHeader('Last-Modified', new java.io.File(this.getServletContext().getRealPath(path)).lastModified())
        
        readFile(path, res.getOutputStream())
    },
    
    getPublicPath: function(req) {
        var path = req.getAttribute('com.joelhockey.cirrus.public_path')
        if (path) { return path }
       
        path = req.getAttribute('com.joelhockey.cirrus.path')
        var parts = path.split('/')
        if (parts.length > 0 && parts[0] == '') { parts = parts.splice(1) } // skip first empty value
        if (parts.length == 0) { parts = ['index'] } // index is default view
        if (parts[parts.length - 1].indexOf('.') == -1) { parts[parts.length - 1] += '.html' } // html is default suffix
        path = '/WEB-INF/public/' + parts.join('/')
        req.setAttribute('com.joelhockey.cirrus.public_path', path)
        return path
    }
})
