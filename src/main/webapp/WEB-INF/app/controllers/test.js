codec = com.joelhockey.codec

test = javax.servlet.http.HttpServlet({
    doGet: function(req, res) {
        this.a = [x * 2 for each (x in [0, 1, 2])]
        this.b64 = codec.B64.b2s(codec.Hex.s2b(req.getParameter('hex')))
    }
})
