// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence
//
// JST is a parser / generator.  It parses templates and generates 
// JavaScript that is then eval'ed and saved.  

var JST = {
    templates : {},
    parse : function (body, name) {

        // parser variables
        var line = 1;       // used by parser for error messages
        var linepos = 1;    // used by parser for error messages
        var toks = [];      // parser puts results in here
        var groups;         // used by parser to store regexp matches

        // parser puts tokens in 'toks' array.
        // Each token is an object containing information used by generator 
        // Format of token:
        //  - type: newline|value|opentag|closetag|text
        //  - tok: exact matched text, e.g. '{for (var item in items)}'
        //  - value: value of token, e.g. 'for', 'function foo'
        //  - words: value.split(), used only in opentag and closetag,
        //     e.g. ['for','(var','item','in','items)'], ['function', 'foo']
        
        // parse
        while (body.length > 0) {
            // newline
            if (groups = /^[ \t]*(\r?\n|\r)/.exec(body)) { 
                toks.push({type: "newline", tok: groups[0], value: groups[0]});
                
            // value, opentag, closetag
            } else if (groups = /^(\$|\s*){(\/?)([^\r\n{}]+)}/.exec(body)) {
                var type = groups[1] === "$" ? "value" : 
                    groups[2] == "/" ? "closetag" : "opentag";
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
        //   JST.templates[name] = Object.create(JST.templates[proto]);
        var src = ['JST.templates["' + name + '"] = {}; '];
        
        // escapes
        var swaps = {
                '"': '\\"', 
                "\r": "\\r\\\n",
                "\n": "\\n\\\n", 
                "\r\n": "\\r\\n\\\n"
        };
        
        // add specified string to src.
        // first put text parts into single 'out.write' statement
        var addsrc = function(s) {
            // first push text onto src
            if (textparts.length) {
                src.push('out.write("' + textparts.join('')
                        .replace(/\r?\n|\r|"/g, function(str) { 
                            return swaps[str];
                        }) + '"); ');
                textparts = [];
            }
            src.push(s);
        };
        
        // errors during generator
        var error = function(desc) {
            throw new Error(desc + ", line: " + line + "." + linepos + 
                    ", tagstack: [" + tagstack.join(" > ") + "]");
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
                if (tok.words.length !== 2) {
                    error("invalid prototype tag format: " + tok.tok);
                }
                src[0] = 'JST.templates["' + name 
                    + '"] = Object.create(JST.templates["'
                    + tok.words[1] + '"]); ';
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
                // or if line contains only open/close tags
                var j = i;
                while (j > 0 && /(open|close)tag/.test(toks[--j].type));
                var tags = j < i-1 && (j === 0 || toks[j].type === "newline");
                if (inEval || tags) {
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
            } else if (inText) { // still in text
                textlines.push(tok);
            } else if (inEval) { // still in eval
                addsrc(tok);

            // value substitution
            } else if (tok.type === "value") {
                addsrc('h(' + tok.value + ', out); '); // html-escape

            // open tag
            } else if (tok.type === "opentag") {
                if (tok.words[0] === "function") {
                    if (tok.words.length !== 2) {
                        error("invalid tag format: " + tok.tok);
                    }
                    if (tok.words[1] === "render" && i !== 0) {
                        error("function 'render' not allowed");
                    }
                    fcount++;
                    addsrc('if (!JST.templates["' + 
                            name + '"].hasOwnProperty("' + 
                            tok.words[1] + '")) { JST.templates["' + 
                            name + '"].' + tok.words[1] + 
                            ' = function (out, cx) { with (cx) { ');
                    tagstack.push(tok.value); // push 'function <fname>'
                } else if (tok.words[0] === "if") {
                    addsrc(tok.value + " {");
                    tagstack.push("if");
                } else if (tok.words[0].match(/^els?e?(if)?$/)) {
                    if (!toptag || !toptag.match(/^(if|els?e?(if)?)$/)) {
                        error("unexpected else(if)? tag");
                    }
                    var ifword = tok.words[0] === "else" ? "" : " if ";
                    addsrc("} else " + ifword 
                            + tok.words.slice(1).join(" ") + " {");
                } else if (tok.words[0] === "for") {
                    addsrc("var forcounter = 0; " + 
                            tok.value + " { forcounter++; ");
                    tagstack.push("for");
                } else if (tok.words[0] === "forelse") {
                    if (toptag !== "for") { error("unexpected forelse tag"); }
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
                } else {
                    error("unrecognised tag " + tok.tok);
                }

            // close tag
            } else if (tok.type === "closetag") {
                if (toptag !== tok.value) {
                    error("unexepcted closetag " + tok.tok);
                }
                tagstack.pop();
                if (tok.words[0] === "function") {
                    fcount--;
                    // all nested functions must invoke themselves
                    addsrc(fcount === 0 ? "}}}; "
                            : "}}}; this." + tok.words[1] + "(out, cx); ");
                } else {
                    addsrc("} ");
                }

            // text
            } else if (tok.type === "text") {
                textparts.push(tok.value);
            } else {
            	error("invalid token type: " + tok.type)
            }
            linepos += tok.value.length
        }
        
        return src.join("");
    }
}

// JST relies on global method 'h' for html-escape
var h = h || function(s, out) {
    if (!s) return ""; 
    var result = s.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
    if (out) {
        out.write(result);
    } else {
        return result;
    }
}

// JST relies on Object.create
if (typeof Object.create !== "function") {
    Object.create = function(o) {
        function F() {}
        F.prototype = o;
        return new F();
    }
}
