# cirrus
Cirrus is a Javascript server-side web framework using Java servlets and Rhino.
It is inspired in part by jython's PyServlet, and adds a little more
functionality including javascript templates (similar to trimpath
but with django-style template inheritance) and some routing,
filters, and other features similar to rails or sinatra. 

## Cirrus philosophy
The philosophy of cirrus is that JavaScript (whether you like it yet or not) is
the language that runs in browsers.  If you are doing web development, you will need
to know JavaScript.  So if you have to learn JavaScript, then why not use it
wherever possible, including on the server.

## Cirrus directory layout
A cirrus application is deployed as a java webapp inside a .war file.
The directory layout for a cirrus application 'example' is:

    example
    example/WEB-INF/
    example/WEB-INF/web.xml -- will map all URLs to CirrusServlet
    example/WEB-INF/app/cirrus.js -- main logic to dispath requests
    example/WEB-INF/app/jst.js -- cirrus templates parser
    example/WEB-INF/app/controllers -- controllers in this dir
    example/WEB-INF/app/controllers/pub.js -- handles static content
    example/WEB-INF/app/controlelrs/example.js -- example controller
    example/WEB-INF/app/views/ -- views in subdirs
    example/WEB-INF/app/views/example -- views for example controller
    example/WEB-INF/app/views/example/layout.jst -- layout
    example/WEB-INF/app/views/example/hello.jst -- template for 'hello' page
    example/WEB-INF/public -- static content (htmls, css, js) in here

    
## Cirrus controllers and views
Cirrus has controllers and views.
Cirrus maps the first 2 parts of urls to controller and action.  For example,
the url /example/hello would map to the 'hello' action of controller 'example'.

Controllers are javascript objects (using object literal notation) that contain
functions for each action.  Controllers can also have functions for the
(lower case) http methods 'get', 'post', 'put', 'delete', etc,
and can also have functions 'before', 'after', and 'getLastModified'.

controllers can render views by calling the global 'jst' function, passing in
a context object (can be 'this') containing variables to be used in the template.

Cirrus will look up the view based on the controller and action.  /example/hello
will use the view app/views/example/hello.jst.

### Cirrus Controller example

    var example = {
        hello : function(req, res) {
            this.a = [x * 2 for each (x in [0,1,2])];
            jst(this);
        },
        
        get : function(req, res) {
            res.getWriter().write('unrecognised action in path: ' + path)
        }
    }


## Global variables
Cirrus runs each request in a separate rhino scope to allow multiple threads to
execute simultaneously.  The following global objects are available to each
controller and view:

* path - url path
* method - http method (upper case)
* params - object with parameters
* req - HttpServletRequest object
* res - HttpServletResponse object
* controller - controller parsed from path ('pub' by default)
* action - action parsed from path ('index' by default)

## Cirrus Templates
Cirrus templates are inspired from ideas in
[trimpath](http://code.google.com/p/trimpath/wiki/JavaScriptTemplates)
and [django templates](http://docs.djangoproject.com/en/dev/ref/templates/builtins/).

Cirrus templates copy the django inheritance model fairly closely and use a similar
syntax to both django and trimpath.  Cirrus tries to make things JavaScripty.  For
example rather than templates 'extending' from each other, the keyword 'prototype'
is used, and rather than using 'blocks', Cirrus Templates use functions.

Cirrus parses templates once and compiles them into equivalent javascript objects
(which Rhino compiles into bytecode (and the JVM JIT compiler will optimize if needed)).
The Cirrus inheritance model uses JavaScript functions with prototype inheritance
to allow child templates to override sections defined in parent template files.

### Cirrus Template Examples

app/views/example/layout.jst
    <html>
      <head>
        <title>{function title}default title{/function title}</title>
      </head>
      <body>
      This is start of layout
      {function menu}default layout menu{/function menu}
      {function body}page must do body{/function body}
      </body>
    </html>
    
app/views/example/hello.jst
    {prototype example.layout}
    {function title}hello page{/function title}
    {function body}
      body of hello page
      Hello a is ${cx.a.toString()}
      {for (var p in params)}
        ${p} : ${params[p]}
      {forelse}
        no params
      {/for}
    {/function body}


