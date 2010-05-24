var cirrus

testCases(
    test,
    function setUp() {
    	cirrus = new com.joelhockey.cirrus.CirrusServlet();
    	var sconf = new com.joelhockey.cirrus.MockServletConfig();
    	cirrus.init(sconf);
    },
    
	function testRoot() {
    	var req = new com.joelhockey.cirrus.MockHttpServletRequest("/");
    	var res = new com.joelhockey.cirrus.MockHttpServletResponse();
    	cirrus.service(req, res);
    	assert.that(String(res.baos), matches(/welcome to cirrus/));
	}
)
