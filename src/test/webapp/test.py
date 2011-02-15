from com.joelhockey.codec import B64, Hex
import java
import javax

class test(javax.servlet.http.HttpServlet):
    def doGet(self, req, res):
        a = [x * 2 for x in range(3)]
        b64 = B64.b2s(Hex.s2b(req.getParameter("hex")))
        res.getWriter().print('Hello, World, array is: %s, b64: %s' % (a, b64))
