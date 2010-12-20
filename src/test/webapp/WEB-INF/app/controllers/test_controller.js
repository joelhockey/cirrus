cirrus.controllers.test = {
    GET: {
        action1: function() {
            this.body = "GET /test/action1";
        },
        action2: function() {
            this.body = "GET /test/action2";
        }
    },
    POST: {
        action1: function() {
            this.body = "POST /test/action1";
        }
    }
    
};
