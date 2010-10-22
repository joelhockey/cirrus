MODELS.User = function (username, salt, hashedPassword) {
    username: null,
    salt: null,
    hashedPassword: null,
    
    create: function (username, password) {
        var dbconn = DATASOURCE.getConnection();
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
        var dbconn = DATASOURCE.getConnection();
        try {
        } finally {
            dbconn.close();
        }
    }
};