///*
// * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package br.ufpr.gres.testcase;
//
//import br.ufpr.gres.core.classpath.ClassDetails;
//import br.ufpr.gres.testcase.junit.JUnitFailure;
//import br.ufpr.gres.testcase.junit.JUnitResult;
//import br.ufpr.gres.testcase.junit.JUnitResultBuilder;
//import br.ufpr.gres.util.comparator.AlphanumComparator;
//import org.junit.Test;
//import org.junit.runner.JUnitCore;
//import org.junit.runner.Result;
//import org.junit.runners.model.FrameworkMethod;
//import org.junit.runners.model.TestClass;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.util.*;
//
///**
// *
// * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
// * @version 1.0
// */
//public class TestCase {
//
//    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);
//    private final ClassDetails testClass;
//
//    private ArrayList<Method> testCasesMethods;
//    private final ArrayList<String> testCasesNames;
//
//    /**
//     * Test results
//     */
//    private final Map<String, TestResultType> testResults = new TreeMap<>(new AlphanumComparator());
//
//    public TestCase(ClassDetails testClass) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        this.testClass = testClass;
//        this.testCasesMethods = new ArrayList<>();
//        this.testCasesNames = new ArrayList<>();
//
//        load();
//    }
//
//    /**
//     * Determine if a class contains JUnit tests
//     *
//     * @param cls
//     * @return
//     */
//    public static boolean isTest(Class<?> cls) {
//        if (Modifier.isAbstract(cls.getModifiers())) {
//            return false;
//        }
//
//        TestClass tc;
//
//        try {
//            tc = new TestClass(cls);
//        } catch (IllegalArgumentException e) {
//            return false;
//        } catch (RuntimeException e) {
//            //this can happen if class has Annotations that are not available on classpath
//            throw new RuntimeException("Failed to analyze class " + cls.getName() + " due to: " + e);
//        }
//
//        // JUnit 4
//        try {
//            List<FrameworkMethod> methods = new ArrayList<>(tc.getAnnotatedMethods(Test.class));
//            for (FrameworkMethod method : methods) {
//                List<Throwable> errors = new ArrayList<>();
//                method.validatePublicVoidNoArg(false, errors);
//                if (errors.isEmpty()) {
//                    return true;
//                }
//            }
//        } catch (IllegalArgumentException e) {
//            return false;
//        }
//
//        // JUnit 3
//        Class<?> superClass = cls;
//        while ((superClass = superClass.getSuperclass()) != null) {
//            if (superClass.getCanonicalName().equals(Object.class.getCanonicalName())) {
//                break;
//            } else if (superClass.getCanonicalName().equals(junit.framework.TestCase.class.getCanonicalName())) {
//                return true;
//            }
//        }
//
//        // TODO add support for other frameworks, e.g., TestNG ?
//        return false;
//    }
//
//    public ArrayList<String> getTestCasesNames() {
//        return this.testCasesNames;
//    }
//
//    public ClassDetails getTestClassDetails() {
//        return this.testClass;
//    }
//
//    /**
//     * Create a instance of the test case
//     */
//    private void load() {
//        try {
//            // Determine if a class contains JUnit tests
//            if (!isTest(this.clazzInstance)) {
//                throw new Exception("The test class " + testClass.getClassName() + " does not contains JUnit tests.");
//            }
//
//            // read testcases from the test set class
//            testCasesMethods = new ArrayList(Arrays.asList(this.clazzInstance.getDeclaredMethods()));
//
//            if (testCasesMethods.isEmpty()) {
//                throw new Exception("The test class " + testClass.getClassName() + " does not contains tests.");
//            }
//
//            // Read the test case names
//            testCasesMethods.forEach((currentTestCase) -> {
//                // Define all how PASS, after is updated in runTest method
//                testResults.put(currentTestCase.getName(), TestResultType.PASS);
//                testCasesNames.add(currentTestCase.getName());
//            });
//
//            this.testCasesNames.sort(new AlphanumComparator());
//        } catch (Exception ex) {
//            logger.error("Error for read the test class " + testClass.getClassName(), ex);
//        }
//    }
//
//    /**
//     * Compute the result of a test under the original program
//     */
//    public void runTest() {
//        if (this.clazzInstance != null) {
//            JUnitCore core = new JUnitCore();
//            Result result = core.run(this.clazzInstance);
//            JUnitResultBuilder builder = new JUnitResultBuilder();
//            JUnitResult junitResult = JUnitResultBuilder.build(result);
//
//            if (!junitResult.wasSuccessful()) {
//                for (JUnitFailure failure : junitResult.getFailures()) {
//                    this.testResults.put(failure.getDescriptionMethodName(), TestResultType.FAIL);
//                }
//            }
//        }
//    }
//}
