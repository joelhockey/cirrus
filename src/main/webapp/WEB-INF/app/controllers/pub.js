pub = new javax.servlet.http.HttpServlet({
    
//    agetLastModified: function(req) { print('hello') },
//print('getlastmod')
//        try {
//            var result = new java.io.File(getServletConfig().getRealPath(getPublicPath(req))).lastModified()
//print('lastmod: ' + result)
//            return result
//        } catch (e) {
//            return -1
//        }
//    },
   
    doGet: function(req, res) {
print('pub: doGet')
//        var path = this.getPublicPath(req)

        var path = req.getAttribute('com.joelhockey.cirrus.path')
        var parts = path.split('/')
        if (parts.length > 0 && parts[0] == '') { parts = parts.splice(1) } // skip first empty value
        if (parts.length == 0) { parts = ['index'] } // index is default view
        if (parts[parts.length - 1].indexOf('.') == -1) { parts[parts.length - 1] += '.html' } // html is default suffix
        path = '/WEB-INF/public/' + parts.join('/')
        
        readFile(path, res.getOutputStream())
    },
    
    doPost: function(req, res) {},
    
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
    },

 //   xyz: function(x) { return x }    
})
