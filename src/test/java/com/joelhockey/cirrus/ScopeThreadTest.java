package com.joelhockey.cirrus;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;

import org.mozilla.javascript.Context;

public class ScopeThreadTest {
    public static void main(String[] args) {
        int ITS = 10000;
        Context cx = Context.enter();
        ServletConfig sconf = new MockServletConfig();

        long start, end, memstart, memend;
        memstart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        List<CirrusScope> list = new ArrayList<CirrusScope>();

        start = System.currentTimeMillis();
        for (int i = 0; i < ITS; i++) {
            list.add(new CirrusScope(sconf));
        }
        end = System.currentTimeMillis();
        memend = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("time: " + (end-start) + ", mem: " + (memend-memstart));

        start = System.currentTimeMillis();
        for (int i = 0; i < ITS; i++) {
            list.add(new CirrusScope(sconf));
        }
        end = System.currentTimeMillis();
        memend = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("time: " + (end-start) + ", mem: " + (memend-memstart));


        Context.exit();
    }
}
