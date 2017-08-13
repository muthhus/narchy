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
 * The information from executing a JUnit test case
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class JUnitResult {

    /**
     *
     */
    //private ExecutionTrace executionTrace;
    /**
     *
     */
    private int failureCount;
    /**
     *
     */
    private Class<?> junitClass;

    /**
     *
     */
    private final ArrayList<JUnitFailure> junitFailures = new ArrayList<>();
    /**
     *
     */
    private String name;
    /**
     *
     */
    private int runCount;
    /**
     *
     */
    private long runtime;
    /**
     *
     */
    private boolean successful;
    /**
     *
     */
    private String trace;

    /**
     *
     */
    public JUnitResult(String name) {
        this.successful = true;
        this.name = name;
        this.failureCount = 0;
        this.runCount = 0;
    }

    /**
     *
     */
    public JUnitResult(String name, Class<?> junitClass) {
        this.successful = true;
        this.name = name;
        this.failureCount = 0;
        this.runCount = 0;
        this.junitClass = junitClass;
    }

    /**
     *
     * @param wasSuccessful
     * @param failureCount
     * @param runCount
     */
    public JUnitResult(boolean wasSuccessful, int failureCount, int runCount) {
        this.successful = wasSuccessful;
        this.failureCount = failureCount;
        this.runCount = runCount;
    }

    /**
     *
     * @param junitFailure
     */
    public void addFailure(JUnitFailure junitFailure) {
        junitFailures.add(junitFailure);
    }

    /**
     *
     */
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
        JUnitResult other = (JUnitResult) obj;
        if (this.failureCount != other.failureCount) {
            return false;
        }
//        if (this.junitFailures == null) {
//            if (other.junitFailures != null) {
//                return false;
//            }
//        } else
        if (!this.junitFailures.equals(other.junitFailures)) {
            return false;
        }
        if (this.runCount != other.runCount) {
            return false;
        }
        return this.successful == other.successful;
    }

    /**
     *
     * @return
     */
    /*public ExecutionTrace getExecutionTrace() {
        return this.executionTrace;
    }*/
    /**
     *
     * @param et
     */
    /*public void setExecutionTrace(ExecutionTrace et) {
        this.executionTrace = et;
    }*/
    /**
     *
     * @return
     */
    public int getFailureCount() {
        return this.failureCount;
    }

    /**
     *
     * @return
     */
    public List<JUnitFailure> getFailures() {
        return junitFailures;
    }

    public Class<?> getJUnitClass() {
        return this.junitClass;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     *
     * @param n
     */
    public void setName(String n) {
        this.name = n;
    }

    /**
     *
     * @return
     */
    public int getRunCount() {
        return runCount;
    }

    /**
     *
     * @return
     */
    public long getRuntime() {
        return this.runtime;
    }

    /**
     *
     * @param r
     */
    public void setRuntime(long r) {
        this.runtime = r;
    }

    /**
     *
     * @param s
     */
    public void setSuccessful(boolean s) {
        this.successful = s;
    }

    /**
     *
     * @return
     */
    public String getTrace() {
        return this.trace;
    }

    /**
     *
     * @param t
     */
    public void setTrace(String t) {
        this.trace = t;
    }

    /**
     *
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.failureCount;
        result = prime * result
                + (/*(this.junitFailures == null) ? 0 : */this.junitFailures.hashCode());
        result = prime * result + this.runCount;
        result = prime * result + (this.successful ? 1231 : 1237);
        return result;
    }

    public void incrementFailureCount() {
        this.failureCount++;
    }

    public void incrementRunCount() {
        this.runCount++;
    }

    /**
     *
     * @return
     */
    public boolean wasSuccessful() {
        return this.successful;
    }

}
