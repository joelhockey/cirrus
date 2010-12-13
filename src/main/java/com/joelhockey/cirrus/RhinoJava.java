// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

/**
 * Convert between Rhino and Java types.
 * @see http://weblog.raganwald.com/2007/07/javascript-on-jvm-in-fifteen-minutes.html
 * @see http://groups.google.com/group/mozilla.dev.tech.js-engine.rhino/browse_thread/thread/d27869954c44c355
 * @author Joel Hockey
 */
public class RhinoJava extends WrapFactory {

    /**
     * Convert Rhino (Object, Array) to Java (Map, List), or unwrap
     * NativeJavaObjects.  JavaScript function treated as null.
     * @param obj rhino object
     * @return java object
     */
    public static Object rhino2java(Object obj) {
        if (obj == Undefined.instance || obj == null || obj instanceof BaseFunction) {
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

    /**
     * Convert Rhino NativeArray to java List.
     * @param na native array
     * @return java list
     */
    public static List rhino2javaNativeArray(NativeArray na) {
        List result = new ArrayList();
        for (int i = 0; i < na.getLength(); ++i) {
            result.add(rhino2java(na.get(i, null)));
        }
        return result;
    }

    /**
     * Convert Rhino ScriptableObject to java Map.  Javascript function
     * properties are ignored.
     * @param sObj scriptable object
     * @return java map
     */
    public static Map rhino2javaScriptableObject(ScriptableObject sObj) {
        Map result = new LinkedHashMap();
        Object[] ids = sObj.getIds();
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i].toString();
            Object value = sObj.get(id, null);
            // don't include functions
            if (!(value instanceof BaseFunction)) {
                result.put(id, rhino2java(value));
            }
        }
        return result;
    }

    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
            Object javaObject, Class<?> staticType) {
        if (javaObject instanceof Map) {
            return new RhinoMap(scope, (Map) javaObject);
        } else if (javaObject instanceof List) {
            return new RhinoList(scope, (List) javaObject);
        } else if (javaObject instanceof Collection) {
            return new RhinoCollection(scope, (Collection) javaObject);
        } else if (javaObject instanceof Date) {
            return cx.newObject(scope, "Date", new Object[] {((Date) javaObject).getTime()});
        } else {
            return new NativeJavaObject(scope, javaObject, staticType);
        }
    }

    /**
     * Abstract class that wraps {@link Collection} and implements
     * most of {@link Scriptable}.
     */
    public static abstract class RhinoAbstractCollection implements Scriptable {
        protected Scriptable scope;
        protected Collection collection;
        protected Scriptable prototype;
        public RhinoAbstractCollection(Scriptable scope, Collection collection) {
            this.scope = scope;
            this.collection = collection;
            this.prototype = ScriptableObject.getClassPrototype(scope, "Object");
        }
        public Object get(String name, Scriptable start) {
            if ("length".equals(name) ) {
                return collection.size();
            }
            return collection.contains(name) ? name : Scriptable.NOT_FOUND;
        }
        public Object get(int index, Scriptable start) {
            return collection.contains(index) ? index : Scriptable.NOT_FOUND;
        }
        public boolean has(String name, Scriptable start) {
            return collection.contains(name);
        }
        public boolean has(int index, Scriptable start) {
            return collection.contains(index);
        }
        public void put(String name, Scriptable start, Object value) {
            collection.add(name);
        }
        public void put(int index, Scriptable start, Object value) {
            collection.add(index);
        }
        public void delete(String name) {
            collection.remove(name);
        }
        public void delete(int index) {
            collection.remove(index);
        }
        public Scriptable getPrototype() {
            return prototype;
        }
        public void setPrototype(Scriptable prototype) {}
        public Scriptable getParentScope() {
            return scope;
        }
        public void setParentScope(Scriptable parent) {}
        public Object[] getIds() {
            return collection.toArray();
        }
        public Object getDefaultValue(Class<?> hint) {
            if (hint == null || hint == String.class) {
                return collection.toString();
            } else if (hint == Number.class) {
                return collection.size();
            } else if (hint == Boolean.class) {
                return collection.size() > 0;
            } else {
                throw Context.reportRuntimeError(
                        ScriptRuntime.getMessage0("msg.default.value")
                        + " Unsupported hint class: " + hint);
            }
        }
        public boolean hasInstance(Scriptable instance) {
            return false;
        }
    }

    /**
     * Wraps {@link Collection} to also implement {@link Scriptable}.
     */
    public static class RhinoCollection extends RhinoAbstractCollection implements Collection {
        public RhinoCollection(Scriptable scope, Collection collection) {
            super(scope, collection);
        }
        public String getClassName() {
            return "RhinoCollection";
        }

        // java.util.Collection
        public int size() { return collection.size(); }
        public boolean isEmpty() { return collection.isEmpty(); }
        public boolean contains(Object o) { return collection.contains(o); }
        public Iterator iterator() { return collection.iterator(); }
        public Object[] toArray() { return collection.toArray(); }
        public Object[] toArray(Object[] a) { return collection.toArray(a); }
        public boolean add(Object e) { return collection.add(e); }
        public boolean remove(Object o) { return collection.remove(o); }
        public boolean containsAll(Collection c) { return collection.containsAll(c); }
        public boolean addAll(Collection c) { return collection.addAll(c); }
        public boolean removeAll(Collection c) { return collection.removeAll(c); }
        public boolean retainAll(Collection c) { return collection.retainAll(c); }
        public void clear() { collection.clear(); }
    }

    /**
     * Wraps {@link Map} to also implement {@link Scriptable}.
     */
    public static class RhinoMap extends RhinoAbstractCollection implements Map {
        private Map map;
        public RhinoMap(Scriptable scope, Map map) {
            super(scope, map.keySet());
            this.map = map;
        }
        public String getClassName() {
            return "RhinoMap";
        }
        public Object get(String name, Scriptable start) {
            Object value = map.get(name);
            return value == null ? Scriptable.NOT_FOUND : Context.javaToJS(value, start);
        }
        public Object get(int index, Scriptable start) {
            Object value = map.get(index);
            return value == null ? Scriptable.NOT_FOUND : Context.javaToJS(value, start);
        }
        public void put(String name, Scriptable start, Object value) {
            map.put(name, value);
        }
        public void put(int index, Scriptable start, Object value) {
            map.put(index, value);
        }
        public void delete(String name) {
            map.remove(name);
        }
        public void delete(int index) {
            map.remove(index);
        }
        public void setParentScope(Scriptable parent) {}

        // java.util.Map
        public int size() { return map.size(); }
        public boolean isEmpty() { return map.isEmpty(); }
        public void clear() { map.clear(); }
        public boolean containsKey(Object key) { return map.containsKey(key); }
        public boolean containsValue(Object value) { return map.containsValue(value); }
        public Object get(Object key) { return map.get(key); }
        public Object put(Object key, Object value) { return map.put(key, value); }
        public void putAll(Map m) { map.putAll(m); }
        public Set keySet() { return map.keySet(); }
        public Collection values() { return map.values(); }
        public Set entrySet() { return map.entrySet(); }
        public Object remove(Object key) { return map.remove(key); }
    }

    /**
     * Wraps {@link List} to also implement {@link Scriptable}.
     */
    public static class RhinoList extends RhinoCollection implements List {
        private List list;
        public RhinoList(Scriptable scope, List list) {
            super(scope, list);
            this.list = list;
            this.prototype = ScriptableObject.getClassPrototype(scope, "Array");
        }
        public String getClassName() {
            return "RhinoList";
        }
        public Object get(String name, Scriptable start) {
            return "length".equals(name) ? list.size() : Scriptable.NOT_FOUND;
        }
        public Object get(int index, Scriptable start) {
            if (index < 0 || index >= list.size()) {
                return ScriptableObject.NOT_FOUND;
            }
            return Context.javaToJS(list.get(index), start);
        }
        public boolean has(String name, Scriptable start) {
            return false;
        }
        public boolean has(int index, Scriptable start) {
            return index >= 0 && index < list.size();
        }
        public void put(String name, Scriptable start, Object value) {}
        public void put(int index, Scriptable start, Object value) {
            list.add(index, value);
        }
        public void delete(String name) {}
        public void delete(int index) {
            list.remove(index);
        }
        public Object[] getIds() {
            Object[] ids = new Object[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ids[i] = i;
            }
            return ids;
        }

        // java.util.List
        public boolean addAll(int index, Collection c) { return list.addAll(index, c); }
        public Object get(int index) { return list.get(index); }
        public Object set(int index, Object element) { return list.set(index, element); }
        public void add(int index, Object element) { list.add(index, element); }
        public Object remove(int index) { return list.remove(index); }
        public int indexOf(Object o) { return list.indexOf(o); }
        public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
        public ListIterator listIterator() { return list.listIterator(); }
        public ListIterator listIterator(int index) { return list.listIterator(index); }
        public List subList(int fromIndex, int toIndex) { return list.subList(fromIndex, toIndex); }
    }
}