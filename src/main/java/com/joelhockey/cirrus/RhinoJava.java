/**
 * Original code from:
 * http://weblog.raganwald.com/2007/07/javascript-on-jvm-in-fifteen-minutes.html
 *
 * Updates from Joel Hockey:
 * The MIT Licence
 *
 * Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.joelhockey.cirrus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import com.joelhockey.codec.JSON;

/**
 * Convert between Java String, List, Map and Rhino NativeString, NativeArray, NativeObject
 *
 * @author http://weblog.raganwald.com/2007/07/javascript-on-jvm-in-fifteen-minutes.html
 * @author Joel Hockey
 */
public class RhinoJava {

    /**
     * Convert Java (Map, List) to Rhino (Object, Array).
     * @param obj java object.
     * @param scope optional scope - required for NativeJavaArray
     * @return rhino object
     */
    public static Object java2rhino(Scriptable scope, Object obj) {
        if (obj instanceof String) {
            return obj;
        } else if (obj instanceof Map) {
            return java2rhinoMap(scope, (Map) obj);
        } else if (obj instanceof List) {
            return java2rhinoArray(scope, (List) obj);
        } else if (obj instanceof byte[] || obj instanceof char[] ||  obj instanceof short[] || obj instanceof int[] || obj instanceof long[]) {
            return new NativeJavaArray(scope, obj);
        } else if (obj instanceof Object[]) {
            return java2rhinoArray(scope, Arrays.asList(obj));
        }
        return obj;
    }

    public static NativeObject java2rhinoMap(Scriptable scope, Map map) {
        NativeObject no = new NativeObject();
        // set prototype, add json toString
        ScriptRuntime.setObjectProtoAndParent(no, scope);
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            no.defineProperty(entry.getKey().toString(), java2rhino(scope, entry.getValue()), ScriptableObject.EMPTY);
        }
        return no;
    }

    public static NativeArray java2rhinoArray(Scriptable scope, List list) {
        NativeArray na = new NativeArray(list.size());
        // set prototype, add json toString
        ScriptRuntime.setObjectProtoAndParent(na, scope);
        for (int i = 0; i < list.size(); i++) {
            na.put(i, na, java2rhino(scope, list.get(i)));
        }
        return na;
    }

    /**
     * Convert Rhino (Object, Array) to Java (Map, List), or unwrap
     * NativeJavaObjects.
     * @param obj rhino object
     * @return java object
     */
    public static Object rhino2java(final Object obj) {
        if (obj == Undefined.instance) {
            return null;
        } else if (obj instanceof NativeArray) {
            return rhino2javaNativeArray((NativeArray) obj);
        } else if (obj instanceof ScriptableObject) {
            return rhino2javaScriptableObject((ScriptableObject) obj);
        } else if (obj instanceof NativeJavaObject) {
            return ((NativeJavaObject) obj).unwrap();
        } else {
            return obj;
        }
    }

    public static List rhino2javaNativeArray(final NativeArray na) {
        return new ArrayList() {{
            for (int i = 0; i < na.getLength(); ++i) {
                add(rhino2java(na.get(i, null)));
            }
        }};
    }

    public static Map rhino2javaScriptableObject (final ScriptableObject sObj) {
        Map result = new LinkedHashMap();
        for (Object idObj : sObj.getIds()) {
            String id = idObj.toString();
            result.put(id, rhino2java(sObj.get(id, null)));
        }
        return result;
    }
}
