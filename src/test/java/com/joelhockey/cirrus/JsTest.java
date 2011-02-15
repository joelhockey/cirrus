// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import com.joelhockey.jairusunit.JairusUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Execute js tests using JairusUnit
 * @author Joel Hockey
 */
public class JsTest extends TestCase {
    static {
        System.out.println("jstest");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JairusUnit.jairusunitTestSuite("src/test/javascript/unit/cirrustest.js"));
        return suite;
    }
}
