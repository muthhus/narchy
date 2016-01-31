package nars.java;

import com.gs.collections.api.map.MutableMap;
import com.gs.collections.impl.map.mutable.UnifiedMap;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.nal.Tense;
import nars.nal.nal8.Execution;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;



/**
 * Dynamic proxy for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * <p>
 * TODO option to include stack traces in conjunction with invocation
 */
public class NALObjects extends DefaultTermizer implements Termizer, MethodHandler {

    static final Variable returnValue = $.varDep("returnValue");
    @NotNull
    public static Set<String> methodExclusions = new HashSet<String>() {{
        add("hashCode");
        add("notify");
        add("notifyAll");
        add("wait");
        add("finalize");
        add("stream");
        add("getHandler");
        add("setHandler");
        add("toString");
        add("equals");
    }};

    //    final Map<Object, Term> instances = new com.google.common.collect.MapMaker()
//            .concurrencyLevel(4).weakKeys().makeMap();
    //final HashBiMap<Object,Term> instances = new HashBiMap();
    final MutableMap<Class, ProxyFactory> proxyCache = new UnifiedMap().asSynchronized();

    final Map<Class, ClassOperator> classOps = Global.newHashMap();
    final Map<Method, MethodOperator> methodOps = Global.newHashMap();
    /**
     * non-null if the method is being invoked by NARS,
     * in which case it will reference the task that invoked
     * feedback will be handled by the responsible MethodOperator's execution
     */
    final AtomicReference<Task> volition = new AtomicReference();
    /**
     * cache
     */
    //final Map<Method, Operator> methodOperators = new HashMap();
    public final NAR nar;

    //    /** for method invocation result beliefs  */
//    private float invocationResultFreq = 1f;
//    private float invocationResultConf = 0.9f;
    private final AtomicBoolean goalInvoke = new AtomicBoolean(true);
    /**
     * for externally-puppeted method invocation goals
     */
    private float invocationGoalFreq = 1.0f;
    private float invocationGoalConf = 0.9f;
    /**
     * for meta-data beliefs about (classes, objects, packages, etc..)
     */
    private float metadataBeliefFreq = 1.0f;
    private float metadataBeliefConf = 0.99f;
    private float metadataPriority = 0.1f;


    public NALObjects(NAR n) {
        nar = n;
    }

//    @NotNull
//    public static <N extends NAR> N the(@NotNull N n) throws Exception {
//        NALObjects nalObjects = new NALObjects(n);
//        return nalObjects.the("this", n);
//    }

    @NotNull
    static Compound getMethodOperator(@NotNull Method overridden, Term[] args) {
        //dereference class to origin, not using a wrapped class
        Class c = overridden.getDeclaringClass();

        //HACK
        if (c.getName().contains("_$$_")) ////javassist wrapper class
            c = c.getSuperclass();

        return $.exec($.operator(c.getSimpleName()), args );
    }

    //final AtomicBoolean lock = new AtomicBoolean(false);

    public static boolean isMethodVisible(@NotNull Method m) {
        String n = m.getName();
        return !methodExclusions.contains(n) &&

                //javassist wrapper method HACK todo use something more specific this could trigger a false positive
                !n.contains("_d")

                && (m.getDeclaringClass() != Object.class);
    }


//    /** when a proxy wrapped instance method is called, this can
//     *  parametrically intercept arguments and return value
//     *  and input them to the NAL in narsese.
//     */
//    @Override
//    public Object invoke(Object object, Method overridden, Method forwarder, Object[] args) throws Throwable {
//        Object result = forwarder.invoke(object, args);
//        return invoked( object, overridden, args, result);
//    }

    @Override
    protected Term termClassInPackage(Term classs, Term packagge) {
        Term t = $.inst(classs, packagge);
        nar.believe(metadataPriority, t,
                Tense.ETERNAL,
                metadataBeliefFreq, metadataBeliefConf);
        return t;
    }

    @Override
    protected void onInstanceOfClass(Object o, Term oterm, Term clas) {
        /** only point to type if non-numeric? */
        //if (!Primitives.isWrapperType(instance.getClass()))

        //nar.believe(Instance.make(oterm, clas));
    }

    protected void onInstanceOfClass(Term identifier, Term clas) {
//        if (metadata) {
//            nar.believe(metadataPriority, $.inst(identifier, clas),
//                    Tense.ETERNAL,
//                    metadataBeliefFreq, metadataBeliefConf);
//        }
    }

    @Override
    protected void onInstanceChange(Term oterm, Term prevOterm) {

        Term s = $.sim(oterm, prevOterm);
        if (s instanceof Compound)
            nar.believe(metadataPriority, ((Compound) s),
                    Tense.ETERNAL,
                    metadataBeliefFreq, metadataBeliefConf);

    }


    @Nullable MutableTask invokingGoal(@NotNull Object instance, @NotNull Method method, @NotNull Object[] args) {
        if (methodExclusions.contains(method.getName()))
            return null;

        Compound op = getOperation(method, getMethodInvocationTerms(method, instance, args));

        return $.goal(op,
                invocationGoalFreq, invocationGoalConf).
                present(nar.memory).because("Invoked" /* via VM */);
    }

    //TODO run in separate execution context to avoid synchronized
    @Nullable
    public void invoked(@Nullable Object result, @NotNull Task invokingGoal) {

//        if (!lock.compareAndSet(false,true)) {
//            return result;
//        }


        Task volitionTask = volition.get();

//        //TODO re-use static copy for 'VOID' and null-returning instances
//        if (invokingGoal != null) {
//            InvocationResult ir = new InvocationResult(effect);
//            ((MutableTask)invokingGoal).because(ir);
//        }


        if (volitionTask == null) {

            /** pretend as if it were a goal of its own volition, although it was invoked externally
             *  Master of puppets, I'm pulling your strings */
            nar.input(
                invokingGoal.log(JavaInvoked.the),
                Execution.result(nar, invokingGoal, term(result), Tense.Present).log("Java Return")
            );

        } else {
            //feedback will be returned via operation execution
            //System.out.println("VOLITION " + volitionTask);
        }


//        lock.set(false);
    }

    private Term[] getMethodInvocationTerms(@NotNull Method method, Object instance, Object[] args) {

        //TODO handle static methods

        boolean isVoid = method.getReturnType() == void.class;

        Term[] x = new Term[isVoid ? 3 : 4];
        x[0] = $.the(method.getName());
        x[1] = term(instance);
        x[2] = $.p(terms(args));
        if (!isVoid) {
            x[3] = returnValue;
        }
        return x;
    }

    private Term[] terms(Object[] args) {
        //TODO use direct array creation, not Stream
        return Stream.of(args).map(this::term).toArray(Term[]::new);
    }

//    @Override
//    public Operator getOperator(Method m) {
//        return methodOperators.computeIfAbsent(m, NALObjects::getMethodOperator);
//    }

//    //TODO use a generic Consumer<Task> for recipient/recipients of these
//    public final NAR nar;
//
//    public NALProxyMethodHandler(NAR n /* options */) {
//
//    }
    //    private final List<NALObjMethodHandler> methodHandlers = Global.newArrayList();
//
//    public NALObject() {
//    }
//
//    public NALObject add(NALObjMethodHandler n) {
//        methodHandlers.add(n);
//        return this;
//    }
//

    @NotNull public <T> T theOrNull(String id, @NotNull Class<? extends T> instance, Object... args)  {
        try {
            return the(id, instance, args);
        } catch (Exception e) {
            return null;
        }
    }

    /** creates a new instance to be managed by this */
    @NotNull public <T> T the(String id, @NotNull Class<? extends T> instance, Object... args) throws Exception {

        ProxyFactory factory = proxyCache.getIfAbsentPut(instance, ProxyFactory::new);
        factory.setSuperclass(instance);

        Class clazz = factory.createClass();


        //TODO create the proxy class directly from the class or instance

        //return Atom.the(Utf8.toUtf8(name));

        //        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        Atom identifier = $.the(id);

        T wrappedInstance = the(identifier, (T)clazz.getConstructors()[0].newInstance(args));

        ClassOperator co = classOps.computeIfAbsent(instance, i -> {
            ClassOperator co2 = new ClassOperator(i, goalInvoke, NALObjects.this);
            nar.onExec(co2);
            return co2;
        });

        onInstanceOfClass(identifier, term(instance));

        return wrappedInstance;
    }




    @Nullable
    public Object invokeVolition(Task currentTask, @NotNull Method method, Object instance, Object[] args) throws InvocationTargetException, IllegalAccessException {

        Object result = null;

        volition.set(currentTask);

        result = method.invoke(instance, args);

        volition.set(null);

        return result;
    }


//    final class DelegateHandler<X> implements MethodHandler {
//
//        private final X obj;
//
//        public DelegateHandler(X n) {
//            this.obj = n;
//        }
//
//        @Override public final Object invoke(Object o, Method method, Method method1, Object[] objects) throws Throwable {
//            final X obj = this.obj;
//            Object result = method.invoke(obj, objects);
//            return invoked( obj, method, objects, result);
//        }
//    }

    @Nullable
    @Override
    public final Object invoke(Object obj, @NotNull Method wrapped, @NotNull Method wrapper, Object[] args) throws Throwable {

        if (methodExclusions.contains(wrapped.getName()))
            return wrapper.invoke(obj, args);

        Task invokingGoal = MethodOperator.invokingTask();

        Object result;
        if (invokingGoal==null) {
            invokingGoal = invokingGoal(obj, wrapped, args);
            if (invokingGoal == null) {
                //just execute it
                return wrapper.invoke(obj, args);
            } else {
                MethodOperator.setCurrentTask(invokingGoal);
            }
        }

        //else {


            result = wrapper.invoke(obj, args);

            invoked(result, invokingGoal);

            MethodOperator.setCurrentTask(null);

        //}

        return result;
    }

//    public <T> T build(String id, Class<? extends T> classs) throws Exception {
//        return build(id, classs, null);
//    }

    /**
     * the id will be the atom term label for the created instance
     */
    @NotNull
    <T> T the(Atom identifier, @NotNull T wrappedInstance) throws Exception {


        map(identifier, wrappedInstance);

//        ((ProxyObject) wrappedInstance).setHandler(
////                delegate == null ?
////                this :
//                new DelegateHandler<>(delegate)
//        );
        ((ProxyObject) wrappedInstance).setHandler(this);



        return wrappedInstance;
    }

    public void setGoalInvoke(boolean b) {
        goalInvoke.set(b);
    }


    /** shared log entry marker instance to prevent duplicate execution
     * if a wrapper has already been
     * invoked and the return value already determined */
    public final static class JavaInvoked {
        public final static JavaInvoked the = new JavaInvoked();

        protected JavaInvoked() {         }

        @NotNull
        @Override
        public String toString() {
            return "Java Invoked";
        }
    }


//    @Override
//    public Term term(Object o) {
//        Term i = instances.get(o);
//        if (i!=null)
//            return i;
//        return super.term(o);
//    }


}
