
cirrus.controllers.user = {
    before : function () {
        if (!this.request.session.getAttribute("user")) {
            throw 407;
        }
    },
    GET : {
        detail : function(id) {
            var user = cirrus.db.selectAll("select username from user where id=?", id)[0];
            if (user) {
                this.jst({user: user});
            } else {
                this.flash.error = "No user found";
                cirrus.forward(this, "GET /user/list");
            }
        },
        $ : function () {
            var users = cirrus.db.selectAll("select id, username, created_at, version from user");
            this.jst("list", {users: users});
        },
    },
    
    POST : {
        add : function() {
            
        },
    }
};
