package com.joelhockey.cirrus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PerfTest implements Runnable {
    private int its;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private HttpServlet servlet;
    public long start, end;
    public boolean done = false;
    public Exception exception;

    public PerfTest(int its, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) {
        this.its = its;
        this.servlet = servlet;
        this.req = req;
        this.res =res;
    }

    public void run() {
        try {
            start = System.currentTimeMillis();
            for (int i = 0; i < its; i++) {
                res.reset();
                servlet.service(req, res);
            }
            end = System.currentTimeMillis();
        } catch (ServletException se) {
            se.printStackTrace();
            se.getRootCause().printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            done = true;
        }
    }

    private static void test(int its, int threads, String test, HttpServlet servlet, String path) throws Exception {
        MockServletConfig config = new MockServletConfig();
        servlet.init(config);

        Runtime.getRuntime().gc();
        long startmem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024;

        // kick off one for init
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.path = path;
        MockHttpServletResponse res = new MockHttpServletResponse();
        new PerfTest(1, servlet, req, res).run();
        System.out.println(test + " out: " + new String(res.baos.toByteArray()));

        // create threads
        PerfTest[] testThreads = new PerfTest[threads];
        for (int i = 0; i < testThreads.length; i++) {
            req = new MockHttpServletRequest();
            req.path = path;
            res = new MockHttpServletResponse();
            testThreads[i] = new PerfTest(its, servlet, req, res);
        }

        for (int i = 0; i < testThreads.length; i++) {
            new Thread(testThreads[i]).start();
        }

        long time = 0;
        boolean running = true;
        while (running) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            running = false;
            time = 0;
            for (int i = 0; i < testThreads.length; i++) {
                if (!testThreads[i].done) {
                    running = true;
                }
                time += testThreads[i].end - testThreads[i].start;
            }
        }
        long endmem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024;
        System.out.println(String.format("ITS: %d, threads: %d, test: %-6s time: %5d, per thread: %5d, mem: %5d, startmem: %5d, endmem: %5d",
                its, threads, test, time, (time/threads), (endmem-startmem), startmem, endmem));
    }

    public static void main(String[] args) throws Exception {
        int ITS = 1;
        HttpServlet javaServlet = new JavaServlet();
//        test(ITS, 1, "java", javaServlet, "");
//        test(ITS, 2, "java", javaServlet, "");

        HttpServlet jsServlet = new CirrusServlet();
        test(ITS, 1, "js", jsServlet, "/test/hello");
//        test(ITS, 2, "js", jsServlet, "/test/hello");

//        HttpServlet jythonServlet = new JythonServlet();
//        test(ITS, 1, "jython", jythonServlet, "/src/dynweb/test.py");
//        test(ITS, 2, "jython", jythonServlet, "/src/dynweb/test.py");
    }
}
