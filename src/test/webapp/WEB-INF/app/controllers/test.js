controllers.test = {
    GET: {
        hello: function() {
            this.a = [x * 2 for each (x in [0,1,2])];
            jst();
        }
    }
};
