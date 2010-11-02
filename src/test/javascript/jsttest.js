load("/app/jst.js");

JSTTest = {
    testComment: function() {
        var tests = readFile("/jst.tests.txt").split(/\r?\n=====\r?\n/);
        for (var i = 0; i < tests.length; i++) {
            var parts = tests[i].split(/\r?\n-----\r?\n/);
            var src = parts[0];
            var expected = parts[1].replace(/\r/g, "");
            var name = "Test" + (i + 1);
            var actual = JST.parse(src, name);
            assertEquals(name, expected, actual);
        }
    }
}