// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import javax.servlet.ServletConfig;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;

/**
 * Rhino scope for CirrusServlet.
 * @author Joel Hockey
 */
public class CirrusScope extends ImporterTopLevel {
    private static final long serialVersionUID = 0xDC7C4EC5275394BL;

    private Cirrus cirrus;

    /**
     * Create CirrusScope instance.
     * @param servletConfig servlet config used to access files within web context
     */
    public CirrusScope(ServletConfig servletConfig) {
        Context cx = Context.enter();
        initStandardObjects(cx, false);
        cirrus = new Cirrus(this, servletConfig);
        put("cirrus", this, cirrus);
        put("JSON", this, new RhinoJSON());

        // add some of the cirrus functions into global
        String[] names = {
            "dir",
            "h",
            "load",
            "log",
            "print",
            "readFile",
        };
        defineFunctionProperties(names, Cirrus.class, ScriptableObject.DONTENUM);
        Context.exit();
    }

    public Cirrus getCirrus() {
        return cirrus;
    }
}
