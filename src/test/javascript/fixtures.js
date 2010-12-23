var fixtures = fixtures || function() {
    var args = Array.prototype.slice.call(arguments);
    for each (var table in args) {
        // read fixtures file
        var json = cirrus.readFile("/fixtures/" + table + ".json");
        // delete from table
        cirrus.db.del("delete from " + table);
        
        // load new data
        cirrus.db.insertJson(table, json);
        
        // put object into 'this'
        this[table] = JSON.parse(json);
    }
}