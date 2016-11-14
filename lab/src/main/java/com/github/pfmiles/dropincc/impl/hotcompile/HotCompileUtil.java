/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.hotcompile;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.impl.util.Util;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.StringWriter;
import java.util.*;

/**
 * Compiles code at runtime, using JDK1.6 compiler API.
 * 
 * @author pf-miles
 * 
 */
public class HotCompileUtil {


    public static CompilationResult compile(String qualifiedName, String sourceCode) {
        return _compile(qualifiedName, sourceCode);
    }
    /**
     * Compile java source code dynamically, with a full-qualified class name.
     * 
     * @param qualifiedName
     * @param sourceCode
     * @return The resulting java class object and its corresponding class
     *         loader.
     */
    public static CompilationResult _compile(String qualifiedName, String sourceCode) {
        JavaStringSource source = new JavaStringSource(qualifiedName, sourceCode);
        List<JavaStringSource> ss = Collections.singletonList(source);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        Map<String, JavaStringSource> srcs = new HashMap<>();
        srcs.put(source.getClsName(), source);
        Map<String, JavaMemCls> clses = new HashMap<>();
        JavaFileManager fileManager = null;
        try {
            fileManager = new MemClsFileManager(compiler.getStandardFileManager(null, null, null), clses, srcs);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StringWriter out = new StringWriter();
            List<String> options = Arrays.asList("-classpath", HotCompileConstants.CLASSPATH);
            CompilationTask task = compiler.getTask(out, fileManager, diagnostics, options, null, ss);
            boolean sucess = task.call();
            if (!sucess) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    out.append("Error on line ").append(String.valueOf(diagnostic.getLineNumber())).append(" in ").append(String.valueOf(diagnostic)).append('\n');
                }
                return new CompilationResult(out.toString());
            }
        } finally {
            try {
                fileManager.close();
            } catch (Exception e) {
                throw new DropinccException(e);
            }
        }
        // every parser class should be loaded by a new specific class loader
        HotCompileClassLoader loader = new HotCompileClassLoader(Util.getParentClsLoader(), clses);
        Class<?> cls = null;
        try {
            cls = loader.loadClass(qualifiedName);
        } catch (ClassNotFoundException e) {
            throw new DropinccException(e);
        }
        return new CompilationResult(cls, loader);
    }
}
