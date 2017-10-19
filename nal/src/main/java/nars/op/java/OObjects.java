package nars.op.java;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import jcog.map.CustomConcurrentHashMap;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static jcog.map.CustomConcurrentHashMap.*;


/**
 * Operated Objects - Dynamic proxy classes for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * <p>
 * TODO option to include stack traces in conjunction with invocation
 */
public class OObjects extends DefaultTermizer implements Termizer, MethodHandler {

    @NotNull
    public static final Set<String> methodExclusions = Set.of(
            "hashCode",
            "notify",
            "notifyAll",
            "wait",
            "finalize",
            "stream",
            "getHandler",
            "setHandler",
            "toString",
            "equals"
    );

    static final Map<Class, ProxyFactory> proxyCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64);

    //final Map<Class, ClassOperator> classOps = Global.newHashMap();
    //final Map<Method, MethodOperator> methodOps = Global.newHashMap();

    public final NAR nar;

    /**
     * for externally-puppeted method invocation goals
     */
    private final float invocationGoalFreq = 1.0f;
    private final float invocationGoalConf = 0.9f;
    /**
     * for meta-data beliefs about (classes, objects, packages, etc..)
     */
    private final float metadataBeliefFreq = 1.0f;
    private final float metadataBeliefConf = 0.99f;
    private final float metadataPriority = 0.1f;


    public OObjects(NAR n) {
        nar = n;
    }

    static Term operation(Method overridden, Term[] args) {
        //dereference class to origin, not using a wrapped class
        Class c = overridden.getDeclaringClass();

        //HACK
        if (c.getName().contains("_$$_")) ////javassist wrapper class
            c = c.getSuperclass();

        return $.func((Atomic) $.the(c.getSimpleName()), args);
    }

    //final AtomicBoolean lock = new AtomicBoolean(false);

    public static boolean isMethodVisible(Method m) {
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
    protected Term classInPackage(Term classs, Term packagge) {
        Term t = $.inst(classs, packagge);
        nar.believe(metadataPriority, t,
                Tense.ETERNAL,
                metadataBeliefFreq, metadataBeliefConf);
        return t;
    }

//    @Override
//    protected void onInstanceOfClass(Object o, Term oterm, Term clas) {
//        /** only point to type if non-numeric? */
//        //if (!Primitives.isWrapperType(instance.getClass()))
//
//        //nar.believe(Instance.make(oterm, clas));
//    }

//    protected void onInstanceOfClass(Term identifier, Term clas) {
////        if (metadata) {
////            nar.believe(metadataPriority, $.inst(identifier, clas),
////                    Tense.ETERNAL,
////                    metadataBeliefFreq, metadataBeliefConf);
////        }
//    }

    @Override
    protected void onInstanceChange(Term oterm, Term prevOterm) {

        Term s = $.sim(oterm, prevOterm);
        if (s instanceof Compound)
            nar.believe(metadataPriority, s,
                    Tense.ETERNAL,
                    metadataBeliefFreq, metadataBeliefConf);

    }


    @Nullable Task externallyInvoked(@NotNull Object instance, @NotNull Method method, @NotNull Object[] args, Object result) {
        if (methodExclusions.contains(method.getName()))
            return null;


        Term op = operation(method, getMethodInvocationTerms(method, instance, args, result));

        TaskBuilder g = $.belief(op,
                invocationGoalFreq, invocationGoalConf).
                present(nar);
        g.log("Invoked" /* via VM */);
        return g.apply(nar);
    }

    @NotNull
    private Term[] getMethodInvocationTerms(@NotNull Method method, Object instance, Object[] args, Object result) {

        //TODO handle static methods

        boolean isVoid = method.getReturnType() == void.class;

        Term[] x = new Term[isVoid ? 3 : 4];
        x[0] = $.the(method.getName());
        x[1] = term(instance);
        x[2] = $.p(terms(args));
        if (!isVoid)
            x[3] = term(result);

        return x;
    }

    private Term[] terms(Object[] args) {
        //TODO use direct array creation, not Stream
        return Stream.of(args).map(this::term).toArray(Term[]::new);
    }
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

//    @NotNull public <T> T theOrNull(String id, @NotNull Class<? extends T> instance, Object... args)  {
//        try {
//            return the(id, instance, args);
//        } catch (Exception e) {
//            return null;
//        }
//    }

    /**
     * creates a new instance to be managed by this
     */
    @NotNull
    public <T> T the(String id, Class<? extends T> instance, Object... args) {
        ProxyFactory factory = proxyCache.computeIfAbsent(instance, (c) -> {
            ProxyFactory p = new ProxyFactory();
            p.setSuperclass(c);
            return p;
        });

        Class clazz = factory.createClass();

        try {
            return the((Atom) $.the(id), (T) clazz.getConstructors()[0].newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    @Nullable
    public Object invokeVolition(Method method, Object instance, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(instance, args);
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

    final static ThreadLocal<Task> invokingGoal = new ThreadLocal<>();

    @Nullable
    @Override
    public final Object invoke(@NotNull Object obj, @NotNull Method wrapped, @NotNull Method wrapper, @NotNull Object[] args) throws Throwable {


        Object result = wrapper.invoke(obj, args);

        if (methodExclusions.contains(wrapped.getName()))
            return result; //pass-through


        Task fb = invokingGoal.get();

        if (fb == null) {
            fb = externallyInvoked(obj, wrapped, args, result);
        }

//            if (invokingGoal == null) {
        //just execute it
//                resultwrapper.invoke(obj, args);
//            } else {
//                MethodOperator.setCurrentTask(invokingGoal);
//            }

        if (fb!=null)
            nar.input(fb);


        return result;
    }


    private <T> T the(String identifier, T wrappedInstance) {
        return the((Atom) $.the(identifier), wrappedInstance);
    }

    /**
     * the id will be the atom term label for the created instance
     */
    <T> T the(Atom identifier, T wrappedInstance) {


        synchronized (instances) {
            map(identifier, wrappedInstance);
        }

//        ((ProxyObject) wrappedInstance).setHandler(
////                delegate == null ?
////                this :
//                new DelegateHandler<>(delegate)
//        );
        ((ProxyObject) wrappedInstance).setHandler(this);


        return wrappedInstance;
    }

//
//    /** shared log entry marker instance to prevent duplicate execution
//     * if a wrapper has already been
//     * invoked and the return value already determined */
//    public final static class JavaInvoked {
//        public final static JavaInvoked the = new JavaInvoked();
//
//        protected JavaInvoked() {         }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return "Java Invoked";
//        }
//    }


//    @Override
//    public Term term(Object o) {
//        Term i = instances.get(o);
//        if (i!=null)
//            return i;
//        return super.term(o);
//    }


}
