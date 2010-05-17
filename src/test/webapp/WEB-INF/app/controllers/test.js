codec = com.joelhockey.codec
load('src/dynweb/lib.js')

test = javax.servlet.http.HttpServlet({
    getLastModified: function(req) { print('getLastMod called'); return -1 },
    doGet: function(req, res) {
        this.a = [x * 2 for each (x in [0, 1, 2])]
        this.b64 = codec.B64.b2s(codec.Hex.s2b(req.getParameter('hex')))
    },
    
    doPost: function(req, res) {
    },
    
    helper: function(req) { return req },
    abc: function(x) { return x }
})
