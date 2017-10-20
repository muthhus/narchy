package nars.op.java;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import javassist.tools.reflect.Reflection;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import jcog.Util;
import jcog.map.CustomConcurrentHashMap;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.op.Operator;
import nars.task.NALTask;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.time.Tense;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import javax.script.Invocable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static jcog.map.CustomConcurrentHashMap.*;
import static nars.Op.ATOM;
import static nars.Op.VAR_DEP;


/**
 * Operated Objects - Dynamic proxy classes for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * <p>
 * TODO -
 *  use guava proxies and remove javassist dependency
 *  reflect the available methods first, add them as individual operators
 *      and intercept those correctly. this will avoid dispatch lookup
 *      on each invocation
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

    static final Map<Class, Class> proxyCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64);
    final static Map<Term, Method> methodCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64); //cache: (class,method) -> Method
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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

    final static ThreadLocal<Task> invokingGoal = new ThreadLocal<>();

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

    public static boolean isMethodVisible(Method m) {
        String n = m.getName();
        return !methodExclusions.contains(n) &&

                //javassist wrapper method HACK todo use something more specific this could trigger a false positive
                !n.contains("_d")

                && (m.getDeclaringClass() != Object.class);
    }


    @Override
    protected Term classInPackage(Term classs, Term packagge) {
        Term t = $.inst(classs, packagge);
        nar.believe(metadataPriority, t,
                Tense.ETERNAL,
                metadataBeliefFreq, metadataBeliefConf);
        return t;
    }


    @Override
    protected void onInstanceChange(Term oterm, Term prevOterm) {

        Term s = $.sim(oterm, prevOterm);
        if (s instanceof Compound)
            nar.believe(metadataPriority, s,
                    Tense.ETERNAL,
                    metadataBeliefFreq, metadataBeliefConf);

    }


    @Nullable Task feedback(Object instance, Method method, Object[] args, Object result) {
        if (methodExclusions.contains(method.getName()))
            return null;

        Term classmethod = $.the(instance.getClass().getSimpleName() + "_" +  method.getName());
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
        x[0] = term(instance);
        x[1] = args.length != 1 ? $.p(terms(args)) : term(args[0]) /* unwrapped singleton */;
        if (!isVoid)
            x[2] = term(result);

        return x;
    }

    private Term[] terms(Object[] args) {
        //TODO use direct array creation, not Stream
        return Stream.of(args).map(this::term).toArray(Term[]::new);
    }

    private Term[] terms(TermContainer args) {
        //TODO use direct array creation, not Stream
        return Stream.of(args).map(this::term).toArray(Term[]::new);
    }

    /**
     * creates a new instance to be managed by this
     */
    @NotNull
    public <T> T the(String id, Class<? extends T> instance, Object... args) {
        Class clazz = proxyCache.computeIfAbsent(instance, (c) -> {
            ProxyFactory p = new ProxyFactory();
            p.setSuperclass(c);

            String opName = c.getSimpleName();

            nar.onOp(opName, new Operator.AtomicExec(operator(null /* TODO */), 0.5f));

            return p.createClass();
        });


        try {
            return the((Atom) $.the(id), (T) clazz.getConstructors()[0].newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    private BiConsumer<Task, NAR> operator(MethodHandle mm) {
        return (task, n) -> {
            TermContainer args = Operator.args(task);
            int a = args.subs();
            if (!(a == 3 || (a == 4 && args.sub(3).op() == VAR_DEP))) {
                //this is likely a goal from the NAR to itself about a desired result state
                //used during reasoning
                //anyway it is invalid for invocation (u
                return;
            }

            Term method = args.sub(0);
            if (method.op() != ATOM)
                return;

            Term instance = args.sub(1);
            Object inst = instances.get(instance);
            if (inst == null)
                return;

            Term methodArgs = args.sub(2);
            int m = methodArgs.subs();
            Object[] orgs;
            if (m == 0) {
                orgs = ArrayUtils.EMPTY_OBJECT_ARRAY;
            } else {
                orgs = object(methodArgs.subterms().theArray());
//
//                Class[] types = Util.map(x -> Primitives.unwrap(x.getClass()),
//                        new Class[orgs.length], orgs);
//                mm = method(c, methodName, types);
//                if (mm == null)
//                    return;
            }

            invokingGoal.set(task);
            try {

                mm.bindTo(inst).invokeWithArguments(orgs);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                invokingGoal.set(null);
            }

        };
    }

    private MethodHandle method(Class c, String methodName, Class<?>[] types) {
        try {
            Method m = ReflectionUtils.findMethod(c, methodName, types).orElse(null);
            if (m == null)
                return null;
            m.trySetAccessible();
            return MethodHandles.lookup().unreflect(m);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    @Nullable
    @Override
    public final Object invoke(Object obj, Method wrapped, Method wrapper, Object[] args) throws Throwable {

        Object result = wrapper.invoke(obj, args);

        if (methodExclusions.contains(wrapped.getName()))
            return result; //pass-through

        Task fb = feedback(obj, wrapped, args, result);

        Task cause = invokingGoal.get();
        if (cause != null) {
            ((NALTask) fb).causeMerge(cause);
            ((NALTask) fb).priMax(cause.priElseZero());
        } else {
            fb.priMax(nar.priDefault(fb.punc()));
        }

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
