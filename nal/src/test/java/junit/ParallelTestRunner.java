package junit;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * https://stackoverflow.com/a/16750680
 */
public class ParallelTestRunner extends Suite {

    public ParallelTestRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError {

        super(klass, builder);


        Filter v = new Filter() {
            @Override
            public boolean shouldRun(Description description) {
                Collection<Annotation> a = description.getAnnotations();
                return
                    !a.stream().anyMatch(
                        x -> x.toString().contains(".Ignore")
                ) /*&&
                    a.stream().anyMatch(
                        x -> x.toString().contains(".Test")
                )*/;
            }

            @Override
            public String describe() {
                return "..";
            }
        };

        try {
            filter(v);
        } catch (NoTestsRemainException e) {
            e.printStackTrace();
        }

        setScheduler(new RunnerScheduler() {

            private final ExecutorService service =
                    ForkJoinPool.commonPool();
            //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() );


            public void schedule(Runnable childStatement) {
                service.execute(()->{
                    try {
                        childStatement.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                });
                //service.submit(childStatement);
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