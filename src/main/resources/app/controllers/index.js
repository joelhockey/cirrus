// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

// default index controller redirects to 'index.html'
cirrus.controllers.index = {
    GET: {
        $: function() {
            response.sendRedirect("/login");
        }
    }
};
