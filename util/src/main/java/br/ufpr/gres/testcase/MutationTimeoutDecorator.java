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
package br.ufpr.gres.testcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class MutationTimeoutDecorator {
    
    private static final Logger logger = LoggerFactory.getLogger(MutationTimeoutDecorator.class);
    
    private final long executionTime;
    
    public MutationTimeoutDecorator(final TestUnit child, final long executionTime) {
       // super(child);
        this.executionTime = executionTime;
    }
    
    public void execute(final ClassLoader loader, final ResultCollector rc) {
        final FutureTask<?> future = createFutureForChildTestUnit(loader, rc);
        executeFutureWithTimeOut(future, rc);
        if (!future.isDone()) {
            //this.timeOutSideEffect.apply();

        }
        
    }
    
    private void executeFutureWithTimeOut(final FutureTask<?> future, final ResultCollector rc) {
        try {
            future.get(this.executionTime, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException ex) {
            // swallow
        } catch (final ExecutionException e) {
            logger.error("Error in test case execution", e);
        }
    }
    
    private static FutureTask<?> createFutureForChildTestUnit(final ClassLoader loader, final ResultCollector rc) {
        final FutureTask<?> future = new FutureTask<>(createRunnable(loader,
                rc), null);
        final Thread thread = new Thread(future);
        thread.setDaemon(true);
        thread.setName("mutationTestThread");
        thread.start();
        return future;
    }
    
    private static Runnable createRunnable(final ClassLoader loader, final ResultCollector rc) {
        return () -> {
            try {
                //child().execute(loader, rc);
            } catch (final Throwable ex) {
                //rc.notifyEnd(child().getDescription(), ex);
            }

        };
    }
}
