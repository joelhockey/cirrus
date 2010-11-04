load("/app/jst.js");

JSTTest = {
    writef: function(filename, s) {
        var fw = new java.io.FileWriter(filename);
        fw.write(s);
        fw.close();
    },
    testJst: function() {
        // each test definition separated by line of '====='
        var tests = readFile("/jst.tests.txt").split(/\r?\n=====\r?\n/);
        for (var i = 0; i < tests.length; i++) {
            var name = "Test" + (i + 1);
            
            // within test, template, expected parsed, context,
            // and expected rendered separated by line of '-----'
            var parts = tests[i].split(/\r?\n-----\r?\n/);
            var src = parts[0];
            var expectedParsed = parts[1].replace(/\r/g, "");
            var cxstr = parts[2];
            var expectedRendered = parts[3];
            
            this.writef("target/test-classes/jst.parsed.expected.txt", expectedParsed);
            this.writef("target/test-classes/jst.rendered.expected.txt", expectedRendered);
            
            // validate parsed template
            var actualParsed = JST.parse(src, name);
            this.writef("target/test-classes/jst.parsed.actual.txt", actualParsed);
            assertEquals(name + ":parsed", expectedParsed, actualParsed);
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
    },
    
    testErrors: function() {
        // each test definition separated by line of '====='
        var tests = readFile("/jst.errors.txt").split(/\r?\n=====\r?\n/);
        for (var i = 0; i < tests.length; i++) {
            var name = "Test" + (i + 1);
            // within test, template and expected error separated by line of '-----'
            var parts = tests[i].split(/\r?\n-----\r?\n/);
            var src = parts[0];
            var expectedError = parts[1];
            var gotError = true;
            try {
                JST.parse(src, name);
            } catch (e) {
                assertMatches(name, new RegExp(expectedError), e.message);
                continue; // goto next test
            }
            // if no exception caught, then fail test
            fail("error " + name + " did not give expected error: " + expectedError);
        }
   }
}