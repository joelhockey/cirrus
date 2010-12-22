var fixtures = fixtures || function() {
    var db = new com.joelhockey.cirrus.DB(cirrus.dataSource);
    
    var args = Array.prototype.slice.call(arguments);
    for each (var table in args) {
        // read fixtures file
        var json = cirrus.readFile("/fixtures/" + table + ".json");
        // delete from table
        db.del("delete from " + table);
        
        // load new data
        db.insertJson(table, json);
        
        // put object into 'this'
        this[table] = JSON.parse(json);
    }
}