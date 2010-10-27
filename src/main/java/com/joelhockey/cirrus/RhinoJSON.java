// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.InputStream;

import org.mozilla.javascript.Scriptable;

import com.joelhockey.codec.JSON;

/**
 * JSON for use in js scripts.
 * @author Joel Hockey
 */
public class RhinoJSON {
    private Scriptable scope;
    public RhinoJSON(Scriptable scope) {
        this.scope = scope;
    }

    public static String stringify(Object obj) {
        return JSON.stringify(RhinoJava.rhino2java(obj));
    }

    public Object parse(String json) {
        return RhinoJava.java2rhino(scope, JSON.parse(json));
    }

    public Object parse(InputStream ins) {
        return RhinoJava.java2rhino(scope, JSON.parse(ins));
    }
}
