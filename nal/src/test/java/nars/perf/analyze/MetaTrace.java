//package nars.analyze;
//
//
///*******************************************************************************
// * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * Contributors:
// *    Marc R. Hoffmann - initial API and implementation
// *
// *******************************************************************************/
//
//import nars.nal.nal1.NAL1Test;
//import org.jacoco.core.analysis.Analyzer;
//import org.jacoco.core.analysis.CoverageBuilder;
//import org.jacoco.core.analysis.IClassCoverage;
//import org.jacoco.core.analysis.ICounter;
//import org.jacoco.core.data.ExecutionDataStore;
//import org.jacoco.core.data.SessionInfoStore;
//import org.jacoco.core.instr.Instrumenter;
//import org.jacoco.core.runtime.IRuntime;
//import org.jacoco.core.runtime.LoggerRuntime;
//import org.jacoco.core.runtime.RuntimeData;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.junit.runner.JUnitCore;
//import org.junit.runner.Result;
//import org.junit.runner.notification.Failure;
//
//import java.io.*;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Predicate;
//
///**
// * TODO generates NAL input from instrumented class's stack trace and derivatives
// * of that data (ex: bug discovery by implicating code blocks against the set of
// * unit tests which do and don't involve them in their solution, solution being
// * a critical path of the possibility space)
// * Example usage of the JaCoCo core API. In this tutorial a single target class
// * will be instrumented and executed. Finally the coverage information will be
// * dumped.
// */
//public final class MetaTrace {
//
//    private final Class clazz;
//
////    /**
////     * The test target we want to see code coverage for.
////     */
////    public static class TestTarget implements Runnable {
////
////        public void run() {
////            isPrime(7);
////        }
////
////        private boolean isPrime(final int n) {
////            for (int i = 2; i * i <= n; i++) {
////                if ((n ^ i) == 0) {
////                    return false;
////                }
////            }
////            return true;
////        }
////
////    }
//    private final PrintStream out;
//
//    /**
//     * Creates a new example instance printing to the given stream.
//     *
//     * @param out
//     *            stream for outputs
//     */
//    public MetaTrace(final Class c, final PrintStream out) {
//        this.clazz = c;
//        this.out = out;
//    }
//
//    public static int analyzeAll(@NotNull Analyzer a, @Nullable final File file, @NotNull Predicate<File> include) throws IOException {
//        int count = 0;
//        if (file == null) {
//            throw new NullPointerException();
//        } else if (file.isDirectory()) {
//            for (final File f : file.listFiles()) {
//                if (include.test(f))
//                    count += analyzeAll(a, f, include);
//            }
//        } else {
//            if (include.test(file)) {
//                try (InputStream in = new FileInputStream(file)) {
//                    count += a.analyzeAll(in, file.getPath());
//                }
//            }
//        }
//        return count;
//    }
//
//    /**
//     * Entry point to run this examples as a Java application.
//     *
//     * @param args
//     *            list of program arguments
//     * @throws Exception
//     *             in case of errors
//     */
//    public static void main(final String[] args) throws Exception {
//        new MetaTrace(NAL1Test.class, System.out).execute();
//    }
//
//    /**
//     * Run this example.
//     *
//     * @throws Exception
//     *             in case of errors
//     */
//    public void execute() throws Exception {
//
//        final String targetName = clazz.getName();
//
//        // For instrumentation and runtime we need a IRuntime instance
//        // to collect execution data:
//        final IRuntime runtime = new LoggerRuntime();
//
//        // The Instrumenter creates a modified version of our test target class
//        // that contains additional probes for execution data recording:
//        final Instrumenter instr = new Instrumenter(runtime);
//        final byte[] instrumented = instr.instrument(
//                getTargetClass(targetName), targetName);
//
//
//
//        // Now we're ready to run our instrumented class and need to startup the
//        // runtime first:
//        final RuntimeData data = new RuntimeData();
//        runtime.startup(data);
//
//        // In this tutorial we use a special class loader to directly load the
//        // instrumented class definition from a byte[] instances.
//        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
//        memoryClassLoader.addDefinition(targetName, instrumented);
//
//        final Class<?> targetClass = memoryClassLoader.loadClass(targetName);
//
//
//
//        // Here we execute our test target class through its Runnable interface:
//        if (Runnable.class.isAssignableFrom(targetClass)) {
//            final Object targetInstance = targetClass.newInstance();
//            ((Runnable)targetInstance).run();
//        } else {
//            //try JUnit
//            Result result = JUnitCore.runClasses(targetClass);
//            for (Failure failure : result.getFailures()) {
//                System.out.println(failure.toString());
//            }
//            boolean success = result.wasSuccessful();
//
//            //System.out.println(result.wasSuccessful());
//
//            //throw new RuntimeException("unknown execution strategy: " + targetInstance);
//        }
//
//        // At the end of test execution we collect execution data and shutdown
//        // the runtime:
//        final ExecutionDataStore executionData = new ExecutionDataStore();
//        final SessionInfoStore sessionInfos = new SessionInfoStore();
//        data.collect(executionData, sessionInfos, false);
//        runtime.shutdown();
//
//        // Together with the original class definition we can calculate coverage
//        // information:
//        final CoverageBuilder coverageBuilder = new CoverageBuilder();
//
//
//        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
//
//        analyzeAll(analyzer,
//                //new File(MetaTrace.class.getClassLoader().getResource("..").toURI())
//                new File("/home/me/opennars/nars_logic/builder/classes/main")
//        ,
//
//                (p)->{
//                    /*return p.isDirectory() ||
//                            p.toString();*/
//                    System.out.println("analyze: " + p);
//                    return true;
//                });
//
//
//
//        analyzer.analyzeClass(getTargetClass(targetName), targetName);
//
//        // Let's dump some metrics and line coverage information:
//        for (final IClassCoverage cc : coverageBuilder.getClasses()) {
//            out.printf("Coverage of class %s%n", cc.getName());
//
//
//
//            printCounter("instructions", cc.getInstructionCounter());
//            printCounter("branches", cc.getBranchCounter());
//            printCounter("lines", cc.getLineCounter());
//            printCounter("methods", cc.getMethodCounter());
//            printCounter("complexity", cc.getComplexityCounter());
//
//            for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
//                out.printf("Line %s: %s%n", i, getColor(cc
//                        .getLine(i).getStatus()));
//            }
//        }
//    }
//
//    private InputStream getTargetClass(@NotNull final String name) {
//        final String resource = '/' + name.replace('.', '/') + ".class";
//        return getClass().getResourceAsStream(resource);
//    }
//
//    private void printCounter(final String unit, @NotNull final ICounter counter) {
//        final Integer missed = counter.getMissedCount();
//        final Integer total = counter.getTotalCount();
//        out.printf("%s of %s %s missed%n", missed, total, unit);
//    }
//
//    private static String getColor(final int status) {
//        switch (status) {
//            case ICounter.NOT_COVERED:
//                return "red";
//            case ICounter.PARTLY_COVERED:
//                return "yellow";
//            case ICounter.FULLY_COVERED:
//                return "green";
//        }
//        return "";
//    }
//
//    /**
//     * A class loader that loads classes from in-memory data.
//     */
//    public static class MemoryClassLoader extends ClassLoader {
//
//        private final Map<String, byte[]> definitions = new HashMap<>();
//
//        /**
//         * Add a in-memory representation of a class.
//         *
//         * @param name
//         *            name of the class
//         * @param bytes
//         *            class definition
//         */
//        public void addDefinition(final String name, final byte[] bytes) {
//            definitions.put(name, bytes);
//        }
//
//        @Override
//        protected Class<?> loadClass(final String name, final boolean resolve)
//                throws ClassNotFoundException {
//            final byte[] bytes = definitions.get(name);
//            if (bytes != null) {
//                return defineClass(name, bytes, 0, bytes.length);
//            }
//            return super.loadClass(name, resolve);
//        }
//
//    }
//
//}
