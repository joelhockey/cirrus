// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence
//
// JST is a parser / generator.  It parses templates and generates 
// JavaScript that is then eval'ed and saved.  

var JST = {
    templates : {},
    parse : function (body, name) {

        // First line of 'src' creates template object within JST.templates
        // If template declares prototype, it is replaced with: 
        //   JST.templates[name] = Object.create(JST.templates[proto]);
        var src = ['JST.templates["' + name + '"] = {}; '];
        var tagstack = [];
        var line = 1;
        var linepos = 1;
        var inEval = false;
        var inText = false;
        var textparts = [];
        var fcount = 0;
        var toks = [];
        var groups;

        // parser puts tokens in 'toks' array.
        // Each token is an object containing information used by generator 
        // Format of token:
        //  - type: newline|value|opentag|closetag|text
        //  - tok: exact matched text, e.g. '{for (var item in items)}'
        //  - value: value of token, e.g. 'for'
        //  - words: value.split(), used only in opentag and closetag,
        //     e.g. ['for','(var','item','in','items)'] 
        while (body.length > 0) {
            // newline
            if ((groups = /^[ \t]*(\r?\n|\r)/.exec(body)) != null) { 
                toks.push({type: "newline", tok: groups[0], value: groups[0]});
                
            // value, opentag, closetag
            } else if ((groups = /^\$?{\/?([^\r\n{}]+)}/.exec(body)) != null) {
                var type = body[0] == "$" ? "value" : 
                    body[1] == "/" ? "closetag" : "opentag";
                var value = groups[1].replace(/^\s+|\s+$/g, ""); // trim space
                toks.push({type: type, tok: groups[0], 
                    value: value, words: value.split(/\s+/)});
                
            // text
            } else if ((groups = /^[^\r\n${]+/.exec(body)) != null ||
                    (groups = /^[^\r\n]+/.exec(body)) != null) {
                toks.push({type: "text", tok: groups[0], value: groups[0]});
            
            // parse error should never happen
            } else {
                throw new Error("Could not parse: " + body);
            }
            body = body.substring(groups[0].length);
        }
        
        var text = function() {
            if (textparts.length > 0) {
                src.push('out.write("' + textparts.join('')
                        .replace(/(\r?\n|\r)/g, '\\n\\\n')
                        .replace(/"/g, '\\"') + '"); ');
                textparts = [];
            }
        };
        var error = function(desc) {
            throw new Error(desc + ", line: " + line + "." + linepos + 
                    ", tagstack: [" + tagstack.join(" > ") + "]");
        };

        // skip blank lines at start
        while (toks.length > 0 && toks[0].type === "newline") {
            src.push(toks.shift().tok)
        }
        
        // if first non-newline tok not 'prototype'
        // then wrap with 'function render'
        if (toks.length > 0 && (toks[0].type !== "opentag" || 
                toks[0].words[0] !== "prototype")) {
            toks.unshift({type: "opentag", tok: "{function render}", 
                value: "function render", words: ["function", "render"]});
            toks.push({type: "closetag", tok: "{/function render}", 
                value: "function render", words: ["function", "render"]});
        }
        
        for each (tok in toks) {
            var toptag = tagstack[tagstack.length - 1];
            // newline
            if (tok.type === "newline") {
                line++;
                // pos increased at bottom of for-loop
                linepos = 1 - tok.value.length; 
                if (inEval || fcount === 0) {
                    src.push(tok.value);
                } else {
                    textparts.push(tok.value);
                }

            // are we at end of {text?}...{/text?} or {eval}...{/eval}
            } else if ((inText || inEval) && tok.type === "closetag" &&
                    tok.value === toptag) {
                text();
                src.push("; ");
                // we are now finished 'text' or 'eval' section
                inText = inEval = false; 
            } else if (inText) { // still in text
                textlines.push(tok);
            } else if (inEval) { // still in eval
                src.push(tok);

            // value substitution
            } else if (tok.type === "value") {
                text(); // dump existing text
                src.push('h(' + tok.value + ', out); '); // html-escape

            // open tag
            } else if (tok.type === "opentag") {
                if (tok.words[0] === "prototype") {
                    if (src.length !== 1) { 
                        error("prototype only allowed as first tag");
                    }
                    if (tok.words.length !== 2) {
                        error("invalid tag format: " + tok.tok);
                    }
                    text();
                    src[0] = 'JST.templates["' + 
                        name + '"] = Object.create(JST.templates["' + 
                        tok.words[1] + '"]); ';
                } else if (tok.words[0] === "function") {
                    if (tok.words.length !== 2) {
                        error("invalid tag format: " + tok.tok);
                    }
                    fcount++;
                    text();
                    src.push('if (!JST.templates["' + 
                            name + '"].hasOwnProperty("' + 
                            tok.words[1] + '")) { JST.templates["' + 
                            name + '"].' + tok.words[1] + 
                            ' = function (out, cx) { with (cx) { ');
                    tagstack.push(tok.value); // push 'function <fname>'
                } else if (tok.words[0] === "if") {
                    text();
                    src.push(tok.value + " {");
                    tagstack.push("if");
                } else if (tok.words[0].match(/^els?e?if$/)) {
                    if (!toptag || !toptag.match(/^(else?)?if$/)) {
                        error("unexpected /else?if/ tag");
                    }
                    text();
                    src.push("} else if " + words.slice(1).join(" ") + " {");
                } else if (tok.words[0] === "for") {
                    text();
                    src.push("var forcounter = 0; " + 
                            tok.value + " { forcounter++; ");
                    tagstack.push("for");
                } else if (tok.words[0] === "forelse") {
                    if (toptag !== "for") { error("unexpected forelse tag"); }
                    text();
                    src.push("} if (forcounter === 0) { ");
                } else if (tok.words[0] === "eval") {
                    text();
                    if (tok.words.length === 1) {
                         inEval = true;
                         tagstack.push("eval");
                    } else { // inline eval
                        src.push(tok.words.slice(1).join(" ") + "; ");
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
                text();
                if (tok.words[0] === "function") {
                    fcount--;
                    src.push(fcount > 0 ? "}}}; this." + 
                            tok.words[1] + "(out, cx); " : "}}}; ");
                } else {
                    src.push("} ");
                }

            // text
            } else if (tok.type === "text") {
                textparts.push(tok.value);
            } else {
            	error("invalid token type: " + tok.type)
            }
            linepos += tok.value.length
        }
        text();
        return src.join("");
    }
}