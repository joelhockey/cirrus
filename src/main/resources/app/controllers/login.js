// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

cirrus.controllers.login = {
    GET : {
        $ : function () {
            jst("login");
        },
    },
    
    POST : {
        $ : function() {
            var users = DB.selectAll("select username, salt, hashed_password from user where username=?", [params.username]);
            try {
                var user = users[0];
                var hash = com.joelhockey.jless.security.Digest.newSha256Digest().
                    updateHex(user.salt).updateStr(params.password).digestHex();
                if (hash != user.hashedPassword) {
                    throw [hash, user.hashedPassword, user];
                }
                request.session.setAttribute("user", user);
                log("user logged in: " + user.username)
                response.sendRedirect("/user/list");
            } catch (e) {
                flash.error = "invalid username / password";
                jst("login");
            }
        },
    }
};
