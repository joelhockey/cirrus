
cirrus.controllers.user = {
    before : function () {
        
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
	        jst("list", {users: DB.selectAll("select id, username, created_at, version")});
		},
	},
	
	POST : {
		add : function() {
		    
		},
	}
};
