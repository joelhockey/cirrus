/**
Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

JST is a parser / generator / compiler.  It parses templates and generates 
JavaScript that can be compiled (eval'ed using JavaScript runtime).

JST ensures that the generated JavaScript code will have the same line numbers
as the input template so that error messages from 'eval' will have
line numbers that match the input template.

Each compiled template produces a JavaScript object that is stored
in JST.templates and contains method 'render(out, cx)'
 * 'out' must be an object that implements 'write(str)'
 * 'cx' is an object that is used as the context to provide 
   variables used in the template.
JST provides a helper method 'JST.render(name, cx)' that returns
the rendered template as a string.

The most basic usage is to call:
 * JST.compile(name, template);
 * var output = JST.render(name, cx);
 
    js> load("jst.js");
    js> var template = "{for each (var name in names)}\nHello ${name}\n{/for}";
    js> JST.compile("hello", template);
    js> print(JST.render("hello", { names: ["John", "Paul", "George", "Ringo"] }));
    Hello John
    Hello Paul
    Hello George
    Hello Ringo
    
    js>
    
The code below shows parsing and compiling done separately, and
templates writing to a stream.

    js> var generated = JST.parse("hello", template);
    js> print(generated);
    JST.templates["hello"] = {}; if (!JST.templates["hello"].hasOwnProperty("render")) { JST.templates["hello"].render = function (out, cx) { with (cx) { var forcounter = 0; for each (var name in names) { forcounter++;
    out.write("Hello "); h(name, out); out.write("\n\
    "); } }}};
    js> eval(generated);
    js> var stream = { buf: [], write: function(s) { this.buf.push(s); } }
    js> JST.templates.hello.render(stream, { names: ["John", "Paul", "George", "Ringo"] });
    js> print(stream.buf.join(""));
    Hello John
    Hello Paul
    Hello George
    Hello Ringo
    
    js>
*/

var JST = {
    templates : {},
    compile: function(name, body) { eval(this.parse(name, body)); }, 
    render: function(name, cx) {
        var out = { buf: [], write: function(s) { this.buf.push(s); }};
        JST.templates[name].render(out, cx);
        return out.buf.join("");
    },
    get: function(templateName, protoName) {
        if (protoName) {
            var F = function() {}
            F.prototype = this.get(protoName);
            JST.templates[templateName] = new F();
        } else {
            JST.templates[templateName] = JST.templates[templateName] || {};
        }
        return JST.templates[templateName];
    },
    parse : function (name, body) {

        // parser variables
        var line = 1;       // used by parser for error messages
        var linepos = 1;    // used by parser for error messages
        var toks = [];      // parser puts results in here
        var groups;         // used by parser to store regexp matches

        // parser puts tokens in 'toks' array.
        // Each token is an object containing information used by generator 
        // Format of token:
        //  - type: newline|value|opentag|closetag|text|comment
        //  - tok: exact matched text, e.g. '{for (var item in items)}'
        //  - value: value of token, e.g. 'for', 'function foo'
        //  - words: value.split(), used only in opentag and closetag,
        //     e.g. ['for','(var','item','in','items)'], ['function', 'foo']

        // parse
        while (body.length > 0) {
            // newline
            if (groups = /^[ \t]*(\r?\n|\r)/.exec(body)) { 
                toks.push({type: "newline", tok: groups[0], value: groups[0]});

            // comment
            } else if (groups = /^\s*{\*(.*?)\*}/.exec(body)) {
                toks.push({type: "comment", tok: groups[0], value: groups[1]});
            // value, opentag, closetag
            } else if (groups = /^(\$|\s*){(\/?)([^\r\n{}]+)}/.exec(body)) {
                var type = "opentag";
                if (groups[1] === "$") { type = "value";
                } else if (groups[2] === "/") { type = "closetag"; }
                var value = groups[3].replace(/^\s+|\s+$/g, ""); // trim space
                toks.push({type: type, tok: groups[0], 
                    value: value, words: value.split(/\s+/)});

            // text
            } else if ((groups = /^[^\r\n${]+/.exec(body)) ||
                    (groups = /^[^\r\n]+/.exec(body))) {
                toks.push({type: "text", tok: groups[0], value: groups[0]});
            } else {
                // impossible
                throw new Error("Could not parse: " + body);
            }
            body = body.substring(groups[0].length);
        }

        // generator variables
        var tagstack = [];  // used by generator to match open/close tags
        var inEval = false; // gen handles tokens differently in eval-mode
        var inText = false; // similar to 'inEval'
        var textparts = []; // text parts are grouped for better render perf
        var fcount = 0;     // number of functions on tagstack
        // src is output of generator
        // First line of 'src' creates template object within 'JST.templates'
        // If template declares prototype, src[0] is replaced with: 
        //   JST.get(name, proto);
        var src = ['JST.get("' + name + '"); '];
        
        // add specified string to src.
        // first put text parts into single 'out.write' statement
        var addsrc = function(s) {
            // first push text onto src
            if (textparts.length) {
                src.push('out.write("' + textparts.join('')
                        .replace(/\r?\n|\r|"/g, function(str) {
                            switch(str) {
                            case "\r\n": return "\\r\\n\\\n";
                            case '"': return '\\"';
                            case "\n": return "\\n\\\n";
                            case "\r": return "\\r\\\n";
                            }
                        }) + '"); ');
                textparts = [];
            }
            src.push(s);
        };
        
        // errors during generator
        var error = function(b, desc) {
            if (b) {
                throw new Error(desc + ", line: " + line + "."  + linepos 
                        + ", tagstack: [" + tagstack.join(" > ") + "]");
            }
        };

        // Generate JS template that can be eval'ed
        // Take care to match line numbers of generated JS with
        // line numbers of source.  We then rely on JS interpreter to
        // provide good error messages
        
        // skip blank lines at start
        while (toks.length > 0 && toks[0].type === "newline") {
            toks.shift();
            src.push("\n");
        }
        
        // either set up 'prototype' or wrap toks with 'function render'
        if (toks.length > 0 && (toks[0].type === "opentag" 
                    && toks[0].words[0] === "prototype")) {
                var tok = toks.shift();
                error(tok.words.length !== 2, 
                        "invalid prototype tag format: " + tok.tok);
                // create empty prototype if it doesn't yet exist
                src[0] = 'JST.get("' + name + '", "' + tok.words[1] + '"); '; 
        } else {
            // wrap with render (front and back)
            toks.unshift({type: "opentag", tok: "{function render}", 
                value: "function render", words: ["function", "render"]});
            toks.push({type: "closetag", tok: "{/function render}", 
                value: "function render", words: ["function", "render"]});
        }

        for (var i = 0; i < toks.length; i++) {
            var tok = toks[i];
            var toptag = tagstack[tagstack.length - 1];
            // newline
            if (tok.type === "newline") {
                // update line, pos is increased at bottom of for-loop
                line++;
                linepos = 1 - tok.value.length;
                // check if newline is formatting only, or needs to be rendered
                // formatting only if we are in {eval}...{/eval},
                // or if line contains only open/close tags, or if fcount === 0
                var j = i;
                while (j > 0 && /^(opentag|closetag|comment)$/.test(toks[--j].type));
                var tags = j < i-1 && (j === 0 || toks[j].type === "newline");
                if (inEval || tags || fcount === 0) {
                    addsrc("\n"); // formatting only
                } else {
                    textparts.push(tok.value); // needs to be rendered
                }

            // are we at end of {text?}...{/text?} or {eval}...{/eval}
            } else if ((inText || inEval) && tok.type === "closetag"
                    && tok.value === toptag) {
                addsrc("; ");
                // we are now finished 'text' or 'eval' section
                inText = inEval = false; 
                tagstack.pop();
            } else if (inText) { // still in text
                textparts.push(tok.tok);
            } else if (inEval) { // still in eval
                addsrc(tok.value);

            // comment
            } else if (tok.type === "comment") {
                // do nothing - ignore comment
                
            // value substitution
            } else if (tok.type === "value") {
                addsrc('h(' + tok.value + ', out); '); // html-escape

            // open tag
            } else if (tok.type === "opentag") {
                if (tok.words[0] === "function") {
                    error(tok.words.length !== 2, 
                            "invalid function tag format: " + tok.tok);
                    error(tok.words[1] === "render" && i !== 0, 
                            "function 'render' not allowed");
                    // inner functions are wrapped within
                    // 'if (!JST.templates[name].hasOwnProperty(name)) { ...'
                    if (fcount > 0) {
                        addsrc('if (!JST.templates["' + name 
                            + '"].hasOwnProperty("' + tok.words[1] + '")) { '); 
                    }
                    addsrc('JST.templates["' + name + '"].' + tok.words[1]
                        + ' = function (out, cx) { with (cx) { ');
                    fcount++;
                    tagstack.push(tok.value); // push 'function <fname>'
                } else if (tok.words[0] === "render") {
                    error(tok.words.length !== 2, 
                            "invalid render tag format: " + tok.tok);
                    addsrc('JST.templates["' + tok.words[1] + '"].render(out, cx); ');
                } else if (tok.words[0] === "if") {
                    addsrc(tok.value + " {");
                    tagstack.push("if");
                } else if (tok.words[0].match(/^els?e?(if)?$/)) {
                    error(!toptag || !toptag.match(/^(if|els?e?(if)?)$/),
                            "unexpected else(if)? tag");
                    var ifword = tok.words[0] === "else" ? "" : " if ";
                    addsrc("} else " + ifword 
                            + tok.words.slice(1).join(" ") + " {");
                } else if (tok.words[0] === "for") {
                    addsrc("var forcounter = 0; " + 
                            tok.value + " { forcounter++; ");
                    tagstack.push("for");
                } else if (tok.words[0] === "forelse") {
                    error(toptag !== "for", "unexpected forelse tag"); 
                    addsrc("} if (forcounter === 0) { ");
                } else if (tok.words[0] === "eval") {
                    if (tok.words.length === 1) {
                         inEval = true;
                         tagstack.push("eval");
                    } else { // inline eval
                        addsrc(tok.words.slice(1).join(" ") + "; ");
                    }
                } else if (tok.words[0].match(/^text/)) {
                    inText = true;
                    tagstack.push(tok.value);
                } else { // treat as text
                    tok.type = "text";
                    textparts.push(tok.tok);
                }

            // close tag
            } else if (tok.type === "closetag") {
                error(toptag !== tok.value, "unexepcted closetag " + tok.tok);
                addsrc("} ");
                tagstack.pop();
                if (tok.words[0] === "function") {
                    addsrc("};");
                    fcount--;
                    // inner functions are wrapped within
                    // 'if (!JST.templates[name].hasOwnProperty(name)) { ...'
                    // and must invoke self
                    if (fcount > 0) {
                        addsrc(" }; this." + tok.words[1] + "(out, cx); ");
                    }
                }

            // text
            } else if (tok.type === "text") {
                // ignore text outside functions
                if (fcount > 0) {
                    textparts.push(tok.value);
                }
            
            } else {
                // should not happen
                error(true, "unrecognised token type: " + tok.type);
            }
            linepos += tok.value.length
        }
        
        return src.join("");
    }
}

// JST relies on global method 'h' for html-escape
var h = h || function(s, out) {
    if (!s) return ""; 
    var result = s.toString().replace(/[&<>"']/g, function(str) {
       switch (str) {
       case "&": return "&amp;";
       case "<": return "&lt;";
       case ">": return "&gt;";
       case '"': return "&quot;";
       case "'": return "&#39;";
       default: return str;
       } 
    });
    if (out) {
        out.write(result);
    } else {
        return result;
    }
}
