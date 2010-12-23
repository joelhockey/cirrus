UserTest = {
    setUp: function() {
        load("/setup.js");
        load("/app/models/user.js");
        cirrus.db.open();
        load("/fixtures.js");
    },
    
    tearDown: function() {
        cirrus.db.close();
    },
    
    testCreate: function() {
        assertEquals(0, cirrus.db.selectInt("select count(*) from user where username=?", ["newuser"]));
        cirrus.models.User.create("newuser", "newuser");
        assertEquals(1, cirrus.db.selectInt("select count(*) from user where username=?", ["newuser"]));
    },

    testGetUser: function() {
        fixtures.call(this, "user");
        var found = cirrus.db.selectAll("select * from user where username=?", ["alice"]);
        assertEquals(1, found.length);
    }
}
