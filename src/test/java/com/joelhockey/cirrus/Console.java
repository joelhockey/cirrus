// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence
// Modified from org.mozilla.javascript.tools.shell.Main

package com.joelhockey.cirrus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;
import org.mozilla.javascript.tools.shell.ShellLine;

/**
 * Cirrus console.
 * @author Joel Hockey
 */
public class Console {

    public static void main(String[] args) throws Exception {
        Context cx = Context.enter();
        // create global scope and load 'setup.js'

        Global global = Main.getGlobal();
        URL setup = Console.class.getResource("/setup.js");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(setup.openStream()));
        cx.evaluateReader(global, reader, setup.toString(), 1, null);

        Logger.getRootLogger().addAppender(new ConsoleAppender(
                new PatternLayout("%m%n"), "System.out"));

        InputStream jlineIns = ShellLine.getStream(global);
        console(cx, global, System.out, jlineIns != null ? jlineIns : System.in);
        Context.exit();
    }

    public static void console(Context cx, ScriptableObject scope,
            PrintStream ps, InputStream ins) {

        ps.println(cx.getImplementationVersion());

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
                    if (line == null) {
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

            // compile
            Script script = Main.loadScriptFromSource(
                    cx, source, "<stdin>", lineno, null);
            if (script == null) { // error compiling
                continue;
            }

            // Main.evaluateScript
            Object result= Context.getUndefinedValue();
            try {
                result = script.exec(cx, scope);
            } catch (RhinoException rex) {
                // exit if one of the 'exitCmds'
                if (Arrays.asList("q","quit","exit").contains(source.trim())) {
                    return;
                }
                ToolErrorReporter.reportException(
                    cx.getErrorReporter(), rex);
            } catch (VirtualMachineError ex) {
                // Treat StackOverflow and OutOfMemory as runtime errors
                ex.printStackTrace();
                String msg = ToolErrorReporter.getMessage(
                    "msg.uncaughtJSException", ex.toString());
                Context.reportError(msg);
            }

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
