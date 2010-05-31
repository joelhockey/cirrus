package com.joelhockey.cirrus;


public class MultiThreadTest implements Runnable {
	private String p;
	private CirrusServlet cirrus;
	public MultiThreadTest(CirrusServlet cirrus, String p) {
		this.cirrus = cirrus;
		this.p = p;
	}

	public void run() {
		MockHttpServletRequest req = new MockHttpServletRequest("/mthread");
		MockHttpServletResponse res = new MockHttpServletResponse();
		req.params.put("p", p);
		try {
			cirrus.service(req, res);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		MockServletConfig sconf = new MockServletConfig();
		CirrusServlet cirrus = new CirrusServlet();
		cirrus.init(sconf);
		
		new Thread(new MultiThreadTest(cirrus, "p1")).start();
		Thread.sleep(1500);
		new Thread(new MultiThreadTest(cirrus, "p2")).start();
		Thread.sleep(1500);
		new Thread(new MultiThreadTest(cirrus, "p3")).start();
	}
}
