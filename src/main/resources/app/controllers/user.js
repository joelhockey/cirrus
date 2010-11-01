
cirrus.controllers.user = {
    before : function () {
        if (!request.session.getAttribute("user")) {
            throw 407;
        }
    },
    GET : {
        detail : function(id) {
            var user = DB.selectAll("select username from user where id=?", id)[0];
            if (user) {
                jst({user: user});
            } else {
                flash.error = "No user found";
                this.$(); // forward to list
            }
        },
        $ : function () {
            var users = DB.selectAll("select id, username, created_at, version from user");
            jst("list", {users: users});
        },
    },
    
    POST : {
        add : function() {
            
        },
    }
};
