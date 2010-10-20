// Original code from http://weblog.raganwald.com/2007/07/javascript-on-jvm-in-fifteen-minutes.html
// Updates Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

/**
 * Convert between Rhino and Java types.
 *
 * @author http://weblog.raganwald.com/2007/07/javascript-on-jvm-in-fifteen-minutes.html
 * @author Joel Hockey
 */
public class RhinoJava extends WrapFactory {

    /**
     * Convert Java (Map, Collection) to Rhino (Object, Array).
     * @param obj java object.
     * @param scope optional scope - required for NativeJavaArray
     * @return rhino object
     */
    public static Object java2rhino(Scriptable scope, Object obj) {
        if (obj == null || obj == Undefined.instance || obj instanceof String
                || obj instanceof Number || obj instanceof Boolean) {
            return obj;
        } else if (obj instanceof Map) {
            return java2rhinoMap(scope, (Map) obj);
        } else if (obj instanceof Collection) {
            return java2rhinoCollection(scope, (Collection) obj);
        } else if (obj instanceof Object[]) {
            return java2rhinoCollection(scope, Arrays.asList(obj));
        } else if (obj.getClass().isArray()) {
            return new NativeJavaArray(scope, obj);
        }
        return obj;
    }

    public static NativeObject java2rhinoMap(Scriptable scope, Map map) {
        NativeObject no = new NativeObject();
        ScriptRuntime.setObjectProtoAndParent(no, scope);
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            no.defineProperty(entry.getKey().toString(), java2rhino(scope, entry.getValue()), ScriptableObject.EMPTY);
        }
        return no;
    }

    public static NativeArray java2rhinoCollection(Scriptable scope, Collection col) {
        NativeArray na = new NativeArray(col.size());
        ScriptRuntime.setObjectProtoAndParent(na, scope);
        int i = 0;
        for (Iterator it = col.iterator(); it.hasNext(); ) {
            na.put(i++, na, java2rhino(scope, it.next()));
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
        if (obj == Undefined.instance || obj == null) {
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
        Object[] ids = sObj.getIds();
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i].toString();
            result.put(id, rhino2java(sObj.get(id, null)));
        }
        return result;
    }

    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
            Object javaObject, Class<?> staticType) {
        if (javaObject instanceof Map) {
            return java2rhinoMap(scope, (Map) javaObject);
        } else if (javaObject instanceof Collection) {
            return java2rhinoCollection(scope, (Collection) javaObject);
        } else {
            return new NativeJavaObject(scope, javaObject, staticType);
        }
    }
}
