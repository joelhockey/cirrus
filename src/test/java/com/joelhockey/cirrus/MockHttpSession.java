// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class MockHttpSession implements HttpSession {
    public Map<String, Object> session = new HashMap<String, Object>();

    public long getCreationTime() { throw new UnsupportedOperationException(); }
    public String getId() { return "" + session.hashCode(); }
    public long getLastAccessedTime() { throw new UnsupportedOperationException(); }
    public ServletContext getServletContext() { throw new UnsupportedOperationException(); }
    public void setMaxInactiveInterval(int interval) { throw new UnsupportedOperationException(); }
    public int getMaxInactiveInterval() { throw new UnsupportedOperationException(); }
    public HttpSessionContext getSessionContext() { throw new UnsupportedOperationException(); }
    public Object getAttribute(String name) {
        return session.get(name);
    }
    public Object getValue(String name) {
        return session.get(name);
    }
    public Enumeration getAttributeNames() {
        return new Vector(session.keySet()).elements();
    }
    public String[] getValueNames() {
        return session.keySet().toArray(new String[0]);
    }
    public void setAttribute(String name, Object value) {
        session.put(name, value);
    }
    public void putValue(String name, Object value) {
        session.put(name, value);
    }
    public void removeAttribute(String name) {
        session.remove(name);
    }
    public void removeValue(String name) {
        session.remove(name);
    }
    public void invalidate() {
        session.clear();
    }
    public boolean isNew() { throw new UnsupportedOperationException(); }

}
