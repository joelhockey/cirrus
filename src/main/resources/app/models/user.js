cirrus.models.User = function (username, salt, hashedPassword) {
    username: null,
    salt: null,
    hashedPassword: null,
    
    create: function (username, password) {
        try {
            var saltbuf = Buf.random(32);
            var salt = Hex.b2s(saltbuf);
            var sha256 = java.security.MessageDigest.getInstance("SHA-256");
            sha256.update(salt);
            sha256.update(Buf.c2b(password));
            var hashedPassword = Hex.b2s(sha256.digest());
            DB.insert("insert into user (username, salt, hashedPassword) values (?, ?, ?)", [username, salt, hashedPassword]);
            return new User(username, salt, hashedPassword);
        } finally {
            dbconn.close();
        }
    },
    
    getUser: function (username, password) {
        try {
            var user = this.db.selectAll("select username, salt, hashed_password from user where username=?", [this.params.username])[0];
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
            
        }
    }
};