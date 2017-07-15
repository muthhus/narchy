package nars.util;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;

import java.util.function.BiConsumer;

public class OptiUnit<T> extends RunListener {


    public OptiUnit(BiConsumer<T,FrameworkMethod> set,
                    BiConsumer<T,FrameworkMethod> get,
                    Class<? extends T>... c) {
        try {

            RunNotifier rn = new RunNotifier();
            rn.addListener(this);

            new Suite(new RunnerBuilder() {
                @Override
                public Runner runnerForClass(Class<?> testClass) throws Throwable {
                    return new BlockJUnit4ClassRunner(testClass) {

                        @Override
                        protected Statement methodBlock(FrameworkMethod method) {

                            Object test;
                            try {
                                test = new ReflectiveCallable() {
                                    @Override protected Object runReflectiveCall() throws Throwable {
                                        return createTest();
                                    }
                                }.run();
                            } catch (Throwable e) {
                                return new Fail(e);
                            }

                            Statement statement = methodInvoker(method, test);
                            statement = possiblyExpectingExceptions(method, test, statement);
                            statement = withPotentialTimeout(method, test, statement);
                            statement = withBefores(method, test, statement);
                            statement = withAfters(method, test, statement);
                            //statement = withRules(method, test, statement);

                            return new Experiment(test, method, statement, set, get);
                        }
                    };
                }
            }, c).run(rn);

        } catch (InitializationError initializationError) {
            initializationError.printStackTrace();
        }
    }

    private static class Experiment<T> extends Statement {

        private final Statement run;
        private final T test;
        private final FrameworkMethod method;
        private final BiConsumer<T, FrameworkMethod> get;
        private final BiConsumer<T, FrameworkMethod> set;

        public Experiment(T instance, FrameworkMethod method, Statement s, BiConsumer<T,FrameworkMethod> set, BiConsumer<T,FrameworkMethod> get) {
            super();
            this.run = s;
            this.method = method;
            this.test = instance;
            this.get = get; this.set = set;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                set();
                run.evaluate();
            } catch (Throwable t) {
                get();
                throw t;
            }
        }

        public void set() {
            this.set.accept(test, method);
        }

        public void get() {
            this.get.accept(test, method);
        }

    }

}
