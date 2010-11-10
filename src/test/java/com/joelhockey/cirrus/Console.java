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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Main;

public class Console {

    public static void main(String[] args) throws Exception {
        console(System.out, System.in);
    }

    public static void console(PrintStream ps, InputStream ins) {
        List<String> exit = Arrays.asList("q,quit,exit".split(","));
        Context cx = Context.enter();
        CirrusScope scope = new CirrusScope(new MockServletConfig());

        ps.println(cx.getImplementationVersion());

        // Use the interpreter for interactive input
        cx.setOptimizationLevel(-1);

        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        int lineno = 1;
        boolean hitEOF = false;
        while (!hitEOF) {
            ps.print("js> ");
            ps.flush();
            String source = "";

            // Collect lines of source to compile.
            while (true) {
                String newline;
                try {
                    newline = in.readLine();
                } catch (IOException ioe) {
                    ps.println(ioe.toString());
                    break;
                }
                if (newline == null) {
                    hitEOF = true;
                    break;
                }
                if (exit.contains(newline.trim())) {
                    System.exit(0);
                }
                source = source + newline + "\n";
                lineno++;
                if (cx.stringIsCompilableUnit(source))
                    break;
                ps.print("  > ");
            }
            Script script = Main.loadScriptFromSource(
                    cx, source, "<stdin>", lineno, null);
            if (script != null) {
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
        ps.println();
    }
}
