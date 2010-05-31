JST.templates["test.hello"] = function() {}; JST.templates["test.hello"].prototype = new JST.templates.layout(); out.write("\n\" +
"); if (typeof JST.templates["test.hello"].prototype.title == "undefined") JST.templates["test.hello"].prototype.title = function(out, context) { out.write("hello page"); }; out.write("\n\
"); if (typeof JST.templates["test.hello"].prototype.body == "undefined") JST.templates["test.hello"].prototype.body = function(out, context) {
  out.write("Hello a is "); out.write(a.toString() || ""); out.write("\n\
  "); var forcounter = 0; for (p in params) { forcounter++; out.write("\n\
    "); out.write(p); out.write(" : "); out.write(params[p] || ""); out.write("\n\
  "); } if (forcounter == 0) { out.write("\n\
  	no params\n\
  "); } out.write("\n\
"); }
