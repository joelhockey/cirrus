// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

cirrus.controllers.login = {
    GET : {
        $ : function () {
            this.jst("login");
        },
    },
    
    POST : {
        $ : function() {
            var users = cirrus.db.selectAll("select username, salt, hashed_password from user where username=?", [this.params.username]);
            try {
                var user = users[0];
                var hash = com.joelhockey.jless.security.Digest.newSha256Digest().
                    updateHex(user.salt).updateStr(this.params.password).digestHex();
                if (hash != user.hashedPassword) {
                    throw [hash, user.hashedPassword, user];
                }
                this.request.session.setAttribute("user", user);
                cirrus.log("user logged in: " + user.username)
                this.response.sendRedirect("/user/list");
            } catch (e) {
                this.flash.error = "invalid username / password";
                this.jst("login");
            }
        },
    }
};
