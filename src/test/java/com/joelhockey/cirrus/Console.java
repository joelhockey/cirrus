// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence
// Modified from org.mozilla.javascript.tools.shell.Main

package com.joelhockey.cirrus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Main;
import org.mozilla.javascript.tools.shell.ShellLine;

/**
 * Cirrus console.
 * @author Joel Hockey
 */
public class Console {
    static {
        Logger.getRootLogger().addAppender(
                new ConsoleAppender(new PatternLayout("%m%n"), "System.out"));
    }

    public static void main(String[] args) throws Exception {
        Context cx = Context.enter();
        // create global scope and load 'setup.js'
        CirrusScope scope = new CirrusScope(new MockServletConfig());
        scope.getCirrus().load("/setup.js");

        InputStream jlineIns = ShellLine.getStream(scope);
        console(cx, scope, System.out, jlineIns != null ? jlineIns : System.in);
        Context.exit();
    }

    public static void console(Context cx, ScriptableObject scope,
            PrintStream ps, InputStream ins) {

        List<String> exitCmds = Arrays.asList("q,quit,exit".split(","));

        ps.println(cx.getImplementationVersion());

        // Use the interpreter for interactive input
        cx.setOptimizationLevel(-1);

        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        int lineno = 1;
        while (true) {
            ps.print("js> ");
            ps.flush();
            String source = "";

            // Collect lines of source to compile.
            while (true) {
                try {
                    String line = in.readLine();
                    if (line == null || exitCmds.contains(line.trim())) {
                        ps.println();
                        return;
                    }
                    source = source + line + "\n";
                    lineno++;
                    if (cx.stringIsCompilableUnit(source)) {
                        break;
                    }
                    ps.print("  > ");
                } catch (IOException ioe) {
                    ps.println(ioe.toString());
                    break;
                }
            }
            Script script = Main.loadScriptFromSource(
                    cx, source, "<stdin>", lineno, null);
            if (script == null) { // error compiling
                continue;
            }
            Object result = Main.evaluateScript(script, cx, scope);

            // Avoid printing out undefined or function definitions.
            if (result != Context.getUndefinedValue()
                    && !(result instanceof Function
                        && source.trim().startsWith("function"))) {
                try {
                    ps.println(Context.toString(result));
                } catch (RhinoException rex) {
                    ToolErrorReporter.reportException(
                            cx.getErrorReporter(), rex);
                }
            }
        }
    }
}
