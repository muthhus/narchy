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

import java.util.ArrayList;
import java.util.List;

/**
 * The information regarding a failure from executing a JUnit test case needed by the JUnitAnalyzer
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class JUnitFailure {

    private final String descriptionMethodName;
    private final String exceptionClassName;
    private final ArrayList<String> exceptionStackTrace = new ArrayList<>();
    private final boolean isAssertionError;
    private final String message;
    private final String trace;
    private final String lineNumber;

    public JUnitFailure(String message, String exceptionClassName,
            String descriptionMethodName, boolean isAssertionError, String trace, String lineNumber) {
        super();
        this.message = message;
        this.exceptionClassName = exceptionClassName;
        this.descriptionMethodName = descriptionMethodName;
        this.isAssertionError = isAssertionError;
        this.trace = trace;
        this.lineNumber = lineNumber;
    }

    public void addToExceptionStackTrace(String elemToString) {
        exceptionStackTrace.add(elemToString);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JUnitFailure other = (JUnitFailure) obj;
        if (exceptionClassName == null) {
            if (other.exceptionClassName != null) {
                return false;
            }
        } else if (!exceptionClassName.equals(other.exceptionClassName)) {
            return false;
        }
//        if (exceptionStackTrace == null) {
//            if (other.exceptionStackTrace != null) {
//                return false;
//            }
//        } else
        if (!exceptionStackTrace.equals(other.exceptionStackTrace)) {
            return false;
        }
        if (isAssertionError != other.isAssertionError) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        if (descriptionMethodName == null) {
            if (other.descriptionMethodName != null) {
                return false;
            }
        } else if (!descriptionMethodName.equals(other.descriptionMethodName)) {
            return false;
        }
        if (trace == null) {
            if (other.trace != null) {
                return false;
            }
        } else if (!trace.equals(other.trace)) {
            return false;
        }

        if (lineNumber == null) {
            if (other.lineNumber != null) {
                return false;
            }
        } else if (!lineNumber.equals(other.lineNumber)) {
            return false;
        }
        return true;
    }

    public String getDescriptionMethodName() {
        return descriptionMethodName;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public List<String> getExceptionStackTrace() {
        return this.exceptionStackTrace;
    }

    public String getMessage() {
        return message;
    }

    public String getTrace() {
        return trace;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exceptionClassName == null) ? 0 : exceptionClassName.hashCode());
        result = prime * result + /*((exceptionStackTrace == null) ? 0 */ exceptionStackTrace.hashCode();
        result = prime * result + (isAssertionError ? 1231 : 1237);
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((descriptionMethodName == null) ? 0 : descriptionMethodName.hashCode());
        result = prime * result + ((trace == null) ? 0 : trace.hashCode());
        result = prime * result + ((lineNumber == null) ? 0 : lineNumber.hashCode());
        return result;
    }

    public boolean isAssertionError() {
        return this.isAssertionError;
    }
}
