load("/app/jst.js");

JSTTest = {
    writef: function(filename, s) {
        var fw = new java.io.FileWriter(filename);
        fw.write(s);
        fw.close();
    },
    testJst: function() {
        var tests = readFile("/jst.tests.txt").split(/\r?\n=====\r?\n/);
        for (var i = 0; i < tests.length; i++) {
            var name = "Test" + (i + 1);
            print("doing " + name);
            
            var parts = tests[i].split(/\r?\n-----\r?\n/);
            var src = parts[0];
            var expectedParsed = parts[1].replace(/\r/g, "");
            var cxstr = parts[2];
            var expectedRendered = parts[3].replace(/\r/g, "");
            
            this.writef("target/test-classes/jst.parsed.expected.txt", expectedParsed);
            this.writef("target/test-classes/jst.rendered.expected.txt", expectedRendered);
            
            // validate parsed template
            var actualParsed = JST.parse(src, name);
            this.writef("target/test-classes/jst.parsed.actual.txt", actualParsed);
//            assertEquals(name + ":parsed", expectedParsed, actualParsed);
            // eval
            eval(expectedParsed);
            
            // create cx
            var cx = eval("(" + cxstr + ")");
            
            // render
            var out = {
                buf: [],
                write: function(s) { this.buf.push(s); }
            }
            JST.templates[name].render(out, cx);
            var actualRendered = out.buf.join("");
            this.writef("target/test-classes/jst.rendered.actual.txt", actualRendered);
            assertEquals(name + ":rendered", expectedRendered, actualRendered);
        }
    }
}