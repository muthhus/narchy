package nars.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

public class OptiUnit<T> extends RunListener {

    final ListMultimap<SortedMap<String, Object>, SortedMap<String, Object>> log =
            MultimapBuilder.hashKeys().arrayListValues().build();

    private final Class<? extends T>[] tests;
    private final Function<T, SortedMap<String, Object>> get;


    /**
     * records modifications to an object
     */
    public static class TweakMap<X> extends TreeMap<String, Object> {

        private final Object obj;

        static final ScriptEngineManager engineManager = new ScriptEngineManager();
        static final NashornScriptEngine JS = (NashornScriptEngine) engineManager.getEngineByName("nashorn");
        private final Bindings ctx;


        public TweakMap(Object obj) {
            this.obj = obj;


            ctx = JS.createBindings();
            ctx.put("thiz", obj);
        }

        public void set(String fieldExpression, Object value) {
            try {
                JS.eval("thiz." + fieldExpression + " = " + value, ctx);
                put(fieldExpression, value);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
//
        public void call(String methodExpression, Object... param) {
            String paramStr = Joiner.on(",").join(param);
            try {
                JS.eval("thiz." + methodExpression + "(" + paramStr + ")", ctx);
                put(methodExpression + "(", paramStr);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }

//        public void setField(String field, Object value) {
//            try {
//                Field f = obj.getClass().getField(field);
//                f.trySetAccessible();
//                f.set(obj, value);
//                put(field, value);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
    }

    public OptiUnit(Function<T, SortedMap<String, Object>> get, Class<? extends T>... tests) {
        this.get = get;
        this.tests = tests;
    }

    public void run(Function<T, SortedMap<String, Object>> set) {
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
                                    @Override
                                    protected Object runReflectiveCall() throws Throwable {
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
            }, tests).run(rn);

        } catch (InitializationError initializationError) {
            initializationError.printStackTrace();
        }
    }


    public class Experiment<S/*,E */> extends Statement {


        private final Statement run;
        private final S subject;
        private final FrameworkMethod experiment;

        private final Function<S, SortedMap<String, Object>> setCause;
        private final Function<S, SortedMap<String, Object>> getEffect;

        SortedMap<String, Object> cause = null;
        SortedMap<String, Object> effect = null;

        /**
         * @param instance
         * @param experiment
         * @param s
         * @param setCause   applies experiment preconditions, while recording them to a 'cause' description
         * @param getEffect  observes experiment postconditions, while recording them to an 'effect' description
         */
        public Experiment(S instance, FrameworkMethod experiment, Statement s, Function<S, SortedMap<String, Object>> setCause, Function<S, SortedMap<String, Object>> getEffect) {
            super();
            this.run = s;
            this.experiment = experiment;
            this.subject = instance;
            this.setCause = setCause;
            this.getEffect = getEffect;

        }

        @Override
        public void evaluate() throws Throwable {
            Throwable error = null;
            try {
                cause = setCause.apply(subject);

                cause.put("_", experiment.toString());

                run.evaluate();
            } catch (Throwable t) {
                error = t;
            }
            effect = getEffect.apply(subject);

            if (error != null)
                logger.error("{} {}", error);


            logger.info(" {}\n\t{}", cause, effect);
            log.put(cause, effect);
        }


    }

    final static Logger logger = LoggerFactory.getLogger(OptiUnit.class);

//    abstract public static interface Experiment<O,P> {
//
////        /** the focus of the experiment; represents the variables being studied */
////        public final O object;
////
////        /** the controlled experimental procedure conducted on the subject.
////         * an model or process that can be applied to various subjects, but is separate from the subject  */
////        public final P procedure;
////
////        protected Experiment(O object, P procedure) {
////            this.object = object;
////            this.procedure = procedure;
////        }
//
//        /** produce the observations/postconditions which represent the effect of the process on the object */
//        SortedMap<String,Object> run(O object, P procedure);
//
//    }

}
