package junit;

import ch.qos.logback.core.util.ExecutorServiceUtil;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

import java.util.concurrent.*;

/** https://stackoverflow.com/a/16750680 */
public class ParallelTestRunner extends Suite {

    public ParallelTestRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError {

        super(klass, builder);


        setScheduler(new RunnerScheduler() {

            private final ExecutorService service =
                    ForkJoinPool.commonPool();
                    //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() );


            public void schedule(Runnable childStatement) {
                service.submit(childStatement);
            }

            public void finished() {
                try {
                    service.shutdown();
                    service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        });


    }
}