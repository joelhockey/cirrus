var JST = {
    templates : {},
    parse : function(body, name) {
        var src = ['JST.templates["' + name + '"] = function() {}; '];
        var tagstack = [];
        var toptag = function() { return tagstack.length > 0 ? tagstack[tagstack.length - 1] : null; };
        var line = 1;
        var linepos = 1;
        var inEval = false;
        var inText = false;
        var tokens = [];
        var groups;
        // split body into tokens of either newline, value, opentag, closetag, text
        while (body.length > 0) {
        	if ((groups = /^[ \t]*\r?\n/.exec(body)) != null) { // newline
        		tokens.push({type: "newline", token: groups[0], value: groups[0]});
        	} else if ((groups = /^\$?{\/?([^\r\n{}]+)}/.exec(body)) != null) { // value, opentag, closetag
        		var type = body[0] == "$" ? "value" : body[1] == "/" ? "closetag" : "opentag";
        		var value = groups[1].replace(/^\s+|\s+$/g, ""); // trim space
        		tokens.push({type: type, token: groups[0], value: value, words: value.split(/\s+/)});
        	} else if ((groups = /^[^\r\n${]+/.exec(body)) != null || (groups = /^[^\r\n]+/) != null) {
        		tokens.push({type: "text", token: groups[0], value: groups[0]});
        	} else {
        		continue;
        	}
            body = body.substring(groups[0].length);
        }
        
        var textparts = [];
        var text = function() {
            if (textparts.length > 0) {
                src.push('out.write("' + textparts.join('').replace(/\r?\n/g, '\\n\\\n').replace('"', '\\"') + '"); ');
                textparts = [];
            }
        }
        var error = function(desc) {
        	throw new Error(desc + ", line: " + line + ", linepos: " + linepos + ", tagstack: " + tagstack);
        }
        var fcount = 0;

        // skip blank lines at start
        while (tokens.length > 0 && tokens[0].type == "newline") {
        	textparts.push(tokens.shift().token)
        }
        
        // if first non-newline token not 'prototype' then wrap with 'function render'
        if (tokens.length > 0 && (tokens[0].type != "opentag" || tokens[0].words[0] != "prototype")) {
        	tokens.unshift({type: "opentag", token: "{function render}", value: "function render", words: ["function", "render"]});
        	tokens.push({type: "closetag", token: "{/function render}", value: "function render", words: ["function", "render"]});
        }
        
        for each (token in tokens) {
//print('token: ' + token.type + ", " + (token.type == "newline" ? "" : token.value))

            // newline
            if (token.type == "newline") {
//print('newline')
                line++;
                linepos = 1 - token.value.length; // pos increased at bottom of for-loop
                if (inEval) {
                	src.push(token.value);
                } else {
                	textparts.push(token.value);
                }

            // are we at end of {text?}...{/text?} or {eval}...{/eval}
            } else if ((inText || inEval) && token.type == "closetag" && token.value == toptag()) {
//print('finishing text/eval')
                text();
                inText = inEval = false; // we are now finished 'text' or 'eval' section
            } else if (inText) { // still in text
//print('still in text')
                textlines.push(token);
            } else if (inEval) { // still in eval
//print('still in eval')
                src.push(token);

            // value substitution
            } else if (token.type == "value") {
//print('value ' + token.type + ', ' + token.token)
                text(); // dump existing text
                src.push("out.write(" + token.value + ' || ""); ')

            // open tag
            } else if (token.type == "opentag") {
//print('opentag: ' + token.value)
                if (token.words[0] == "prototype") {
//print('opentag prototype')
                	if (token.words.length != 2) { error("invalid tag format: " + token.token); }
                    text();
                    src.push('JST.templates["' + name + '"].prototype = new JST.templates["' + token.words[1] + '"](); ');
                } else if (token.words[0] == "function") {
//print('opentag function')
                	if (token.words.length != 2) { error("invalid tag format: " + token.token); }
                	fcount++;
                    text();
                    src.push('if (typeof JST.templates["' + name + '"].prototype.' + token.words[1] + ' == "undefined") JST.templates["' + name + '"].prototype.' + token.words[1] + ' = function(out, context) { ');
                    tagstack.push(token.value); // push 'function <fname>'
                } else if (token.words[0] == "if") {
//print('opentag if')
                    text();
                    src.push("if (" + token.words.slice(1).join(" ") + ") {");
                    tagstack.push("if");
                } else if (token.words[0].match(/^else?if$/)) {
//print('opentag elseif')
                	if (!toptag().match(/^(else?)?if$/)) { error("unexpected /else?if/ tag"); }
                    text();
                    src.push("} else if (" + words.slice(1).join(" ") + ") {");
                } else if (token.words[0] == "for") {
//print('opentag for')
                    text();
                    src.push("var forcounter = 0; for (" + token.words.slice(1).join(" ") + ") { forcounter++; ");
                    tagstack.push("for");
                } else if (token.words[0] == "forelse") {
//print('opentag forelse')
                	if (toptag() != "for") { error("unexpected forelse tag"); }
                    text();
                    src.push("} if (forcounter == 0) { ");
                } else if (token.words[0] == "eval") {
//print('opentag eval')
                    text();
                    if (token.words.length == 1) {
                         inEval = true;
                         tagstack.push("eval");
                    } else {
                        src.push(token.words.slice(1).join(" "));
                    }
                } else if (token.words[0].match(/^text/)) {
//print('opentag text')
                    inText = true;
                    tagstack.push(token.value);
                } else {
//print('opentag ?')
                	error("unrecognised tag " + token.token);
                }

            // close tag
            } else if (token.type == "closetag") {
//print('closetag')
            	if (tagstack.length == 0 || toptag() != token.value) { error("unexepcted closetag " + token.token); }
            	tagstack.pop();
	            text();
	            if (token.words[0] == "function") {
	            	fcount--;
	            	src.push(fcount > 0 ? "}; this." + token.words[1] + "(out, context); " : "}; ");
	            } else {
		            src.push("} ");
	            }

            // text
            } else {
//print('pushing text [' + token.value + ']')
                textparts.push(token.value);
            }
            linepos += token.value.length
        }
        text();
        return src.join("");
    }
}