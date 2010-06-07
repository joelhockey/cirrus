PublicTest = {
//    setUp: function() {
//    	this.cirrus = new com.joelhockey.cirrus.CirrusServlet();
//    	var sconf = new com.joelhockey.cirrus.MockServletConfig();
//    	this.cirrus.init(sconf);
//    },
    
//	testRoot: function() {
//    	var req = new com.joelhockey.cirrus.MockHttpServletRequest("/");
//    	var res = new com.joelhockey.cirrus.MockHttpServletResponse();
//    	this.cirrus.service(req, res);
//    	assertMatches(/welcome to cirrus/, res.getResult());
//	}
        
    testAdd: function() {
        assertEquals(1, 1);
        assertMatches(/abc/, "xabcx");
    }
}
