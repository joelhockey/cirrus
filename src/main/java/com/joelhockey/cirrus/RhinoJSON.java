// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.InputStream;

import com.joelhockey.codec.JSON;

/**
 * JSON for use in js scripts.
 * @author Joel Hockey
 */
public class RhinoJSON {
    public static String stringify(Object obj) {
        return JSON.stringify(RhinoJava.rhino2java(obj));
    }

    public static Object parse(String json) {
        return JSON.parse(json);
    }

    public static Object parseStream(InputStream ins) {
        return JSON.parse(ins);
    }
}
