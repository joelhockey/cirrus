var test = {
    hello : function(req, res) {
        this.a = [x * 2 for each (x in [0,1,2])];
        this.b64 = b64_b2s(hex_s2b(params.hex));
        jst(this);
    },
    doGet : function(req, res) { this.hello(req, res); }
}
