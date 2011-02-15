package com.joelhockey.cirrus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.joelhockey.codec.B64;
import com.joelhockey.codec.Hex;

public class JavaServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        List<Integer> a = new ArrayList<Integer>();
        for (int i : new int[]{0, 1, 2}) {
            a.add(i * 2);
        }
        String b64 = B64.b2s(Hex.s2b(req.getParameter("hex")));
        res.getWriter().print(String.format("Hello, World, array is: %s, b64: %s", a, b64));
    }
}
