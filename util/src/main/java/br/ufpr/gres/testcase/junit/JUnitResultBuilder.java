/*
 * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.ufpr.gres.testcase.junit;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.List;

/**
 * Creates a JUnitResult instance from a org.junit.runner.Result object.
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class JUnitResultBuilder {

    /**
     * Translates <i>part</i> of the org.junit.runner.Result object into an independent object.
     *
     * @param result
     * @return
     */
    public static JUnitResult build(Result result) {
        boolean wasSuccessful = result.wasSuccessful();
        int failureCount = result.getFailureCount();
        int runCount = result.getRunCount();

        JUnitResult junitResult = new JUnitResult(wasSuccessful, failureCount,
                                                  runCount);
        
        List<Failure> failures = result.getFailures();

        for (Failure failure : failures) {
            String descriptionMethodName = failure.getDescription().getMethodName();
            String exceptionClassName = failure.getException().getClass().toString();
            String message = failure.getMessage();
            String trace = failure.getTrace();

            String testSourceName = result.getClass().getSimpleName() + '.' + descriptionMethodName;
            
            String[] sb = failure.getTrace().split("\\n");
            String lineNumber = "";
            for (String sb1 : sb) {
                if (sb1.contains(testSourceName)) {
                    lineNumber = sb1.substring(sb1.indexOf(':') + 1, sb1.indexOf(')'));
                }
            }

            boolean isAssertionError = (failure.getException() instanceof AssertionError);

            JUnitFailure junitFailure = new JUnitFailure(message,
                                                         exceptionClassName, descriptionMethodName,
                                                         isAssertionError, trace, lineNumber);

            for (StackTraceElement elem : failure.getException().getStackTrace()) {
                String elemToString = elem.toString();
                junitFailure.addToExceptionStackTrace(elemToString);
            }

            junitResult.addFailure(junitFailure);
        }
        return junitResult;
    }
}
