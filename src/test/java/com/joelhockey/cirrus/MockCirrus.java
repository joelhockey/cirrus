// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Used for testing.
 * @author Joel Hockey
 */
public class MockCirrus extends Cirrus {
    private static final long serialVersionUID = 0x55FF726E6F32A9A6L;

    public MockCirrus(Scriptable global) {
        super(global, new MockServletConfig());
        // set wrap factory on current context
        Context cx = Context.enter();
        cx.setWrapFactory(CirrusServlet.WRAP_FACTORY);
        Context.exit();
    }
}
