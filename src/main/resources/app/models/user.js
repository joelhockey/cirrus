cirrus.models.User = function(username, salt, hashedPassword) {
    this.username = username;
    this.salt = salt;
    this.hashedPassword = hashedPassword;
};

cirrus.models.User.create = function(username, password) {
    var saltbuf = com.joelhockey.codec.Buf.random(32);
    var salt = com.joelhockey.codec.Hex.b2s(saltbuf);
    var hashedPassword = com.joelhockey.jless.security.Digest.newSha256Digest().
        updateHex(salt).updateStr(password).digestHex();
    var user = new cirrus.models.User(username, salt, hashedPassword);
    cirrus.db.insert("user", user);
    return user;
};

cirrus.models.User.getUser = function(username, password) {
    var user = cirrus.db.selectAll("select username, salt, hashed_password from user where username=?", [this.params.username])[0];
    var hash = com.joelhockey.jless.security.Digest.newSha256Digest().
        updateHex(user.salt).updateStr(password).digestHex();
    if (hash != user.hashedPassword) {
        throw [hash, user.hashedPassword, user];
    }
};
