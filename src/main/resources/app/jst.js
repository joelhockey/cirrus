// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

var JST = {
    templates : {},
    parse : function (body, name) {
        var src = ['JST.templates["' + name + '"] = function() {}; '];
        var tagstack = [];
        var line = 1;
        var linepos = 1;
        var inEval = false;
        var inText = false;
        var textparts = [];
        var fcount = 0;
        var toks = [];
        var groups;
        // split body into toks of either newline, value, opentag, closetag, text
        while (body.length > 0) {
            if ((groups = /^\/\/[^\r\n]*/.exec(body)) != null) { // comment - ignore
            } else if ((groups = /^[ \t]*\r?\n/.exec(body)) != null) { // newline
                toks.push({type: "newline", tok: groups[0], value: groups[0]});
            } else if ((groups = /^\$?{\/?([^\r\n{}]+)}/.exec(body)) != null) { // value, opentag, closetag
                var type = body[0] == "$" ? "value" : body[1] == "/" ? "closetag" : "opentag";
                var value = groups[1].replace(/^\s+|\s+$/g, ""); // trim space
                toks.push({type: type, tok: groups[0], value: value, words: value.split(/\s+/)});
            } else if ((groups = /^[^\r\n${]+/.exec(body)) != null || (groups = /^[^\r\n]+/.exec(body)) != null) { // text
                toks.push({type: "text", tok: groups[0], value: groups[0]});
            } else {
                continue;
            }
            body = body.substring(groups[0].length);
        }
        
        var text = function() {
            if (textparts.length > 0) {
                src.push('out.write("' + textparts.join('').replace(/\r?\n/g, '\\n\\\n').replace(/"/g, '\\"') + '"); ');
                textparts = [];
            }
        };
        var error = function(desc) {
            throw new Error(desc + ", line: " + line + "." + linepos + ", tagstack: [" + tagstack.join(" > ") + "]");
        };

        // skip blank lines at start
        while (toks.length > 0 && toks[0].type === "newline") {
            src.push(toks.shift().tok)
        }
        
        // if first non-newline tok not 'prototype' then wrap with 'function render'
        if (toks.length > 0 && (toks[0].type !== "opentag" || toks[0].words[0] !== "prototype")) {
            toks.unshift({type: "opentag", tok: "{function render}", value: "function render", words: ["function", "render"]});
            toks.push({type: "closetag", tok: "{/function render}", value: "function render", words: ["function", "render"]});
        }
        
        for each (tok in toks) {
            //print('tok: ' + tok.type + ", " + (tok.type == "newline" ? "" : tok.value))
            var toptag = tagstack[tagstack.length - 1];
            // newline
            if (tok.type === "newline") {
                line++;
                linepos = 1 - tok.value.length; // pos increased at bottom of for-loop
                if (inEval || fcount === 0) {
                    src.push(tok.value);
                } else {
                    textparts.push(tok.value);
                }

            // are we at end of {text?}...{/text?} or {eval}...{/eval}
            } else if ((inText || inEval) && tok.type === "closetag" && tok.value === toptag) {
                text();
                inText = inEval = false; // we are now finished 'text' or 'eval' section
            } else if (inText) { // still in text
                textlines.push(tok);
            } else if (inEval) { // still in eval
                src.push(tok);

            // value substitution
            } else if (tok.type === "value") {
                text(); // dump existing text
                src.push('h(' + tok.value + ', out); ') // html-escape

            // open tag
            } else if (tok.type === "opentag") {
                if (tok.words[0] === "prototype") {
                    if (tok.words.length !== 2) { error("invalid tag format: " + tok.tok); }
                    text();
                    src.push('JST.templates["' + name + '"].prototype = new JST.templates["' + tok.words[1] + '"](); ');
                } else if (tok.words[0] === "function") {
                    if (tok.words.length !== 2) { error("invalid tag format: " + tok.tok); }
                    fcount++;
                    text();
                    src.push('if (typeof JST.templates["' + name + '"].prototype.' + tok.words[1] + ' === "undefined") { JST.templates["' + name + '"].prototype.' + tok.words[1] + ' = function (out, cx) { with (cx) { ');
                    tagstack.push(tok.value); // push 'function <fname>'
                } else if (tok.words[0] === "if") {
                    text();
                    src.push(tok.value + " {");
                    tagstack.push("if");
                } else if (tok.words[0].match(/^els?e?if$/)) {
                    if (!toptag || !toptag.match(/^(else?)?if$/)) { error("unexpected /else?if/ tag"); }
                    text();
                    src.push("} else if " + words.slice(1).join(" ") + " {");
                } else if (tok.words[0] === "for") {
                    text();
                    src.push("var forcounter = 0; " + tok.value + " { forcounter++; ");
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
                        src.push(tok.words.slice(1).join(" "));
                    }
                } else if (tok.words[0].match(/^text/)) {
                    inText = true;
                    tagstack.push(tok.value);
                } else {
                    error("unrecognised tag " + tok.tok);
                }

            // close tag
            } else if (tok.type === "closetag") {
                if (toptag !== tok.value) { error("unexepcted closetag " + tok.tok); }
                tagstack.pop();
                text();
                if (tok.words[0] === "function") {
                    fcount--;
                    src.push(fcount > 0 ? "}}}; this." + tok.words[1] + "(out, cx); " : "}}}; ");
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