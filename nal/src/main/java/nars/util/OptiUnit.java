package nars.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import nars.$;
import org.jetbrains.annotations.Nullable;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class OptiUnit<T> extends RunListener {

//    final ListMultimap<SortedMap<String, Object>, SortedMap<String, Object>> log =
//            MultimapBuilder.hashKeys().arrayListValues().build();

    final List<Experiment> log = Collections.synchronizedList($.newArrayList());

    private final Class<? extends T>[] tests;
    private final Function<T, SortedMap<String, Object>> get;

    //JUnit specific:
    private final RunNotifier rn;




    /**
     * records modifications to an object
     * for an immutable representation of experiment's preconditions, etc.
     *
     *      * invoke method with arguments
     *      * set field value
     *
     * each action consists of a key and value pair.  the order they are applied
     * should not matter, and so the keys will be stored sorted lexicographically
     *
     * it uses nashorn javascript engine to evaluate the generated expressions
     * upon the given object.
     *
     */
    public static class Tweaks<X> extends TreeMap<String, Object> {

        private Object obj;

        static final ScriptEngineManager engineManager = new ScriptEngineManager();
        static final NashornScriptEngine JS = (NashornScriptEngine) engineManager.getEngineByName("nashorn");
        private final Bindings ctx = JS.createBindings();


        public Tweaks() {
            this(null);
        }

        public Tweaks(@Nullable X obj) {
            to(obj);
        }

        public Tweaks<X> to(@Nullable X obj) {
            ctx.put("thiz", this.obj = obj);
            return this;
        }

        public Tweaks<X> set(String fieldExpression, Object value) {
            try {
                JS.eval("thiz." + fieldExpression + " = " + value, ctx);
                put(fieldExpression, value);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return this;
        }
//
        public Tweaks<X> call(String methodExpression, Object... param) {
            String paramStr = Joiner.on(",").join(param);
            try {
                JS.eval("thiz." + methodExpression + "(" + paramStr + ")", ctx);
                put(methodExpression + "(", paramStr);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return this;
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

        rn = new RunNotifier();
        rn.addListener(this);
    }

    public OptiUnit<T> run(Function<T, SortedMap<String, Object>> setup) {
        try {
            new Suite(new BuildMyRunners(setup), tests).run(rn);

        } catch (Throwable t) {
            t.printStackTrace();
            logger.error(" {}", t);
        }

        return this;
    }


    public class Experiment<S/*,E */> extends Statement {

        public final String id;

        private final S subject;

        private final Function<S, SortedMap<String, Object>> setCause;
        private final Function<S, SortedMap<String, Object>> getEffect;

        public SortedMap<String, Object> cause = null;
        public SortedMap<String, Object> effect = null;

        //JUnit specific:
        private final Statement run;
        private final FrameworkMethod experiment;

        private boolean traceErrors = false;


        /**
         * @param instance
         * @param experiment
         * @param s
         * @param setCause   applies experiment preconditions, while recording them to a 'cause' description
         * @param getEffect  observes experiment postconditions, while recording them to an 'effect' description
         */
        public Experiment(S instance, FrameworkMethod experiment, Statement s, Function<S, SortedMap<String, Object>> setCause, Function<S, SortedMap<String, Object>> getEffect) {
            super();
            this.id = experiment.toString() + " " + Util.UUIDbase64();
            this.run = s;
            this.experiment = experiment;
            this.subject = instance;
            this.setCause = setCause;
            this.getEffect = getEffect;
        }

        public void print(PrintStream p) {
            p.println(id);
            BiConsumer<String, Object> printKeyValue = (k, v) -> {
                p.append(k).append('\t').append(toString(v)).append('\n');
            };
            cause.forEach(printKeyValue);
            effect.forEach(printKeyValue);
            p.println();
        }

        public String toString(Object v) {
            return v.toString();
        }

        @Override
        public void evaluate() throws Throwable {
            Throwable error = null;
            try {
                cause = setCause.apply(subject);

                run.evaluate();
            }
            catch (Throwable t) {
                error = t;
            }

            effect = getEffect.apply(subject);

            log.add(this);


            if (error != null && traceErrors)
                logger.trace("{} {}", error);

        }


    }

    final static Logger logger = LoggerFactory.getLogger(OptiUnit.class);

    private class MyRunner extends BlockJUnit4ClassRunner {

        private final Function<T, SortedMap<String, Object>> set;

        public MyRunner(Class<?> testClass, Function<T, SortedMap<String, Object>> set) throws InitializationError {
            super(testClass);
            this.set = set;
        }

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
    }

    private class BuildMyRunners extends RunnerBuilder {
        private final Function<T, SortedMap<String, Object>> set;

        public BuildMyRunners(Function<T, SortedMap<String, Object>> set) {
            this.set = set;
        }

        @Override
        public Runner runnerForClass(Class<?> testClass) throws Throwable {
            return new MyRunner(testClass, set);
        }
    }

   public void print(File out) throws FileNotFoundException {
        print(new PrintStream(new FileOutputStream(out)));
    }

    public void print(PrintStream out) {
        log.forEach(e -> e.print(out));
    }

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
