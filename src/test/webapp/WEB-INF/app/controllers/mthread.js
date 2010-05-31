var mthread = {
    doGet: function(req, res) {
	    java.lang.Thread.sleep(1000)
	    print('after 1 s, req.getParameter(p) ' + req.getParameter('p') + ', params.p ' + params.p)
	    java.lang.Thread.sleep(1000)
	    print('after 2 s, req.getParameter(p) ' + req.getParameter('p') + ', params.p ' + params.p)
	    java.lang.Thread.sleep(1000)
	    print('after 3 s, req.getParameter(p) ' + req.getParameter('p') + ', params.p ' + params.p)
    },
}
