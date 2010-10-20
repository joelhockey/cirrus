// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence.

controllers.login = {
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
                var sha256 = com.joelhockey.jless.security.Digest.newSha256();
                sha256.update(com.joelhockey.codec.Hex.s2b(user.salt));
                var hash = sha256.digest(new java.lang.String(params.password).getBytes());
                if (hash !== user.hashedPassword) {
                    throw [hash, user.hashedPassword, user];
                }
                req.getSession().put("user", user);
                log("user logged in: " + user.username)
                jst("user", "list");
            } catch (e) {
log(e)
                flash.errors = "invalid username / password";
                jst("login");
            }
        },
    }
};
