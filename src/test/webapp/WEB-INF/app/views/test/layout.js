JST.templates["test.layout"] = function() {}; JST.templates["test.layout"].prototype.render = function(out, context) { out.write("<html>\n\
  <head>\n\
    <title>"); if (typeof JST.templates["test.layout"].prototype.title == "undefined") JST.templates["test.layout"].prototype.title = function(out, context) { out.write("default title"); }; this.title(out, context); out.write("</title>\n\
  </head>\n\
  <body>\n\
  This is start of layout\n\
  "); if (typeof JST.templates["test.layout"].prototype.menu == "undefined") JST.templates["test.layout"].prototype.menu = function(out, context) { out.write("default layout menu"); }; this.menu(out, context); out.write("\n\
  "); if (typeof JST.templates["test.layout"].prototype.body == "undefined") JST.templates["test.layout"].prototype.body = function(out, context) { out.write("page must do body"); }; this.body(out, context); out.write("\n\
  </body>\n\
</html>"); }