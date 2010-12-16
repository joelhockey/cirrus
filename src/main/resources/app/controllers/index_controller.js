// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

// default index controller redirects to '/login'
cirrus.controllers.index = {
    GET: {
        $: function() {
            cirrus.log(this)
            this.response.sendRedirect("/login");
        }
    }
};
