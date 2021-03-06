package nars.op.java;

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import jcog.TODO;
import jcog.Util;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.list.FasterList;
import jcog.memoize.SoftMemoize;
import nars.*;
import nars.control.CauseChannel;
import nars.op.AtomicExec;
import nars.op.Operator;
import nars.task.ITask;
import nars.task.LatchingSignalTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;


/**
 * Operated Objects - Dynamic proxy classes for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * <p>
 * TODO option to include stack traces in conjunction with invocation
 */
public class OObjects extends DefaultTermizer implements MethodHandler {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(OObjects.class);

    public static final float DESIRE_THRESH = 0.6f;

    @NotNull
    public final Set<String> methodExclusions = Sets.newConcurrentHashSet(Set.of(
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
    ));

    static final Map<Class, Class> proxyCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64);
    static final Map<Term, Method> methodCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64); //cache: (class,method) -> Method


    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    //final Map<Class, ClassOperator> classOps = Global.newHashMap();
    //final Map<Method, MethodOperator> methodOps = Global.newHashMap();

    public final NAR nar;

    /**
     * for externally-puppeted method invocation goals
     */
    private final float invocationBeliefFreq = 1.0f;
    private final float invocationBeliefConfFactor = 1f;
    /**
     * for meta-data beliefs about (classes, objects, packages, etc..)
     */
    private final float metadataBeliefFreq = 1.0f;
    private final float metadataBeliefConf = 0.99f;
    private final float metadataPriority = 0.1f;

    final static ThreadLocal<Task> invokingGoal = new ThreadLocal<>();
    private final CauseChannel<ITask> in;
    private final static SoftMemoize<Pair<Pair<Class, String>, List<Class>>, MethodHandle> methodArgCache = new SoftMemoize<>((xx) -> {

        Class c = xx.getOne().getOne();
        String m = xx.getOne().getTwo();
        List<Class> types = xx.getTwo();
        Method x = findMethod(c, m, types.isEmpty() ? ArrayUtils.EMPTY_CLASS_ARRAY : ((FasterList<Class>) types).array());
        if (x == null)
            return null;

        x.trySetAccessible();

        try {
            return MethodHandles.lookup().unreflect(x);
        } catch (IllegalAccessException e) {
            logger.warn("{} {} {} {}", c, m, types, e);
            return null;
        }

    }, 512, true /* soft */);

    public OObjects(NAR n) {
        nar = n;
        in = n.newCauseChannel(this);
    }


    @Override
    protected Term classInPackage(Term classs, Term packagge) {
        Term t = $.inst(classs, packagge);
//        nar.believe(metadataPriority, t,
//                Tense.ETERNAL,
//                metadataBeliefFreq, metadataBeliefConf);
        return t;
    }


    @Override
    protected void onInstanceChange(Term oterm, Term prevOterm) {

        Term s = $.sim(oterm, prevOterm);
//        if (s instanceof Compound)
//            nar.believe(metadataPriority, s,
//                    Tense.ETERNAL,
//                    metadataBeliefFreq, metadataBeliefConf);

    }

    static class ValueSignalTask extends LatchingSignalTask {

        final Object value;

        public ValueSignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp, Object value) {
            super(t, punct, truth, start, end, stamp);
            this.value = value; //weakref?
        }
    }

    interface InstanceMethodValueModel {

        Object update(Instance instance, Task cause, Object obj, Method method, Object[] args, Object nextValue);
    }

    /**
     * TODO not fully tested, and missing Quench support
     */
    public class ExtendedMethodValueModel implements InstanceMethodValueModel {
        /**
         * current (previous) value
         */
        public final ConcurrentHashMap<Method, ValueSignalTask> value = new ConcurrentHashMap();

        @Override
        public Object update(Instance instance, Task cause, Object obj, Method method, Object[] args, Object nextValue) {

            float pri = nar.priDefault(BELIEF);

            // this essentially synchronizes on each (method,resultValue) tuple
            // so it can form a coherent, synchronzed sequence of value change events


            List<Task> pending = $.newArrayList(2); //max 2
            Term nextTerm = instance.opTerm(method, args, nextValue);

            value.compute(method, (m, p1) -> {

                long now = nar.time();
                if (p1 != null && Objects.equals(p1.value, nextValue)) {
                    //just continue the existing task

                    p1.priMax(pri); //rebudget
                    p1.grow(now);
                    return p1; //keep
                }


                float f = invocationBeliefFreq;
                Term nt = nextTerm;
                if (nt.op() == NEG) {
                    nt = nt.unneg();
                    f = 1 - f;
                }
                ValueSignalTask next = new ValueSignalTask(nt,
                        BELIEF, $.t(f, nar.confDefault(BELIEF)),
                        now, now, nar.time.nextStamp(), nextValue);

                if (Param.DEBUG)
                    next.log("Invocation" /* via VM */);

//                if (explicit) {
                next.causeMerge(cause);
                next.priMax(cause.priElseZero());
                //cause.pri(0); //drain
                cause.meta("@", next);
//                } else {
//                    next.priMax(pri);
//                }


                if (p1 != null && !p1.equals(nt)) {
                    p1.end(Math.max(p1.start(), now - 1)); //dont need to re-input prev, this takes care of it. ends in the cycle previous to now
                    next.priMax(pri);

                    NALTask prevEnd = new NALTask(p1.term(),
                            BELIEF, $.t(1f - invocationBeliefFreq, nar.confDefault(BELIEF)),
                            now, now, now, nar.time.nextInputStamp());
                    prevEnd.priMax(pri);
                    if (Param.DEBUG)
                        prevEnd.log("Invoked");

                    pending.add(prevEnd);
                }

                pending.add(next);
                return next;
            });

            in.input(pending);
            return nextValue;
        }
    }

    final InstanceMethodValueModel pointTasks = new PointMethodValueModel();
    final Function<String, InstanceMethodValueModel> valueModel = (x) -> pointTasks /* memoryless */;

    public class PointMethodValueModel implements InstanceMethodValueModel {


        @Override
        public Object update(Instance instance, Task cause, Object obj, Method method, Object[] args, Object nextValue) {
            float f = invocationBeliefFreq;
            Term nextTerm = instance.opTerm(method, args, nextValue);
            Term nt = nextTerm;
            if (nt.op() == NEG) {
                nt = nt.unneg();
                f = 1 - f;
            }
            long now = nar.time();
            NALTask next = new NALTask(nt,
                    BELIEF, $.t(f, nar.confDefault(BELIEF)),
                    now, now, now, nar.time.nextInputStamp());

            if (Param.DEBUG)
                next.log("Invoked");

            float pri = nar.priDefault(BELIEF);


            if (cause != null) {
                next.causeMerge(cause);
                next.priMax(cause.priElseZero());
                //cause.pri(0); //drain
                cause.meta("@", next);
            } else {
                next.priMax(pri);
            }


            if (cause != null && !next.term().equals(cause.term())) {
                //input quenching invocation belief term corresponding to the goal
                NALTask quench = new NALTask(cause.term(), BELIEF, $.t(f, nar.confDefault(BELIEF)),
                        now, now, now, nar.time.nextInputStamp());
                quench.priMax(next.priElseZero());
                quench.causeMerge(next);
                quench.meta("@", next);
                if (Param.DEBUG)
                    quench.log("InvoQuench");
                in.input(quench);
            }

            in.input(next);

            return nextValue;
        }
    }


    class Instance extends Atom {

        /**
         * reference to the actual object
         */
        public final Object object;

        final InstanceMethodValueModel belief;

        /**
         * for VM-caused invocations: if true, inputs a goal task since none was involved. assists learning the interface
         */
        static private final boolean goalMimic = true;

        public Instance(String id, Object object) {
            super(id);
            this.object = object;
            this.belief = valueModel.apply(id);

            nar.onOp(this, new MethodExec(object));
        }

        public Object update(Object obj, Method method, Object[] args, Object nextValue) {
            Task cause = invokingGoal.get();

            if (cause == null && goalMimic) {
                cause = goalMimic(obj, method, args);
            }

            Object o = belief.update(this, cause, obj, method, args, nextValue);

            return o;
        }

        private Task goalMimic(Object obj, Method method, Object[] args) {
            long now = nar.time();
            NALTask g = new NALTask(opTerm(method, args,
                    method.getReturnType() == void.class ? null : $.varDep(1)), GOAL,
                    $.t(1f, nar.confDefault(GOAL)), now, now, now, nar.time.nextInputStamp());
            g.priMax(nar.priDefault(GOAL));
            g.meta("mimic", "");
            if (Param.DEBUG)
                g.log("Mimic");
            in.input(g);
            return g;
        }

        private Term opTerm(Method method, Object[] args, Object result) {

            //TODO handle static methods

            boolean isVoid = result == null && method.getReturnType() == void.class;
            Term[] x = new Term[isVoid ? 2 : 3];
            x[0] = $.the(method.getName());
            switch (args.length) {
                case 0:
                    x[1] = Op.ZeroProduct;
                    break;
                case 1:
                    x[1] = OObjects.this.term(args[0]);
                    break; /* unwrapped singleton */
                default:
                    x[1] = $.p(terms(args));
                    break;
            }
            assert (x[1] != null) : "could not termize: " + Arrays.toString(args);

            boolean negate = false;

            if (result instanceof Term) {
                Term tr = (Term) result;
                if (tr.op() == NEG) {
                    tr = tr.unneg();
                    negate = true;
                }
                x[2] = tr;
            } else {
                boolean isBoolean = method.getReturnType() == boolean.class;
                if (isBoolean) {

                    boolean b = (Boolean) result;
                    if (!b) {
                        result = true;
                        negate = true;
                    }
                }

                if (!isVoid) {
                    x[2] = OObjects.this.term(result);
                    assert (x[2] != null) : "could not termize: " + result;
                }
            }

            return $.func(toString(), x).negIf(negate).normalize();
        }

    }

    private class MethodExec extends AtomicExec {
        public MethodExec(Object object) {
            super(operator(object.getClass()), DESIRE_THRESH);
        }

        @Override
        protected boolean exePrefilter(Task x) {
            TermContainer args = validArgs(Operator.args(x));
            if (args == null)
                return false;
            if (null == validMethod(args.sub(0)))
                return false;
            //TODO other prefilter conditions
            return true;
        }
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
     * wraps a provided instance in an intercepting proxy class
     */
    public <T> T the(String id, T instance) {

        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(instance.getClass());
        try {
            register(id, instance);
            return (T) f.create(ArrayUtils.EMPTY_CLASS_ARRAY, ArrayUtils.EMPTY_OBJECT_ARRAY,
                    (self, thisMethod, proceed, args) ->
                            invoked(instance, thisMethod, args, thisMethod.invoke(instance, args))
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

//        T newInstance = (T)Proxy.newProxyInstance(instance.getClass().getClassLoader(),
//                new Class[] { instance.getClass() }, new AbstractInvocationHandler() {
//            @Override
//            protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
//                Object result = method.invoke(proxy, args);
//                invoked(proxy, method, args, result);
//                return result;
//            }
//        });

    }

    /**
     * creates a new instance to be managed by this
     */
    @NotNull
    public <T> T a(String id, Class<? extends T> instance, Object... args) {

        Class clazz = proxyCache.computeIfAbsent(instance, (c) -> {
            ProxyFactory p = new ProxyFactory();
            p.setSuperclass(c);
            return p.createClass();
        });


        try {

            T newInstance = (T) clazz.getDeclaredConstructor(typesOfArray(args)).newInstance(args);
            ((ProxyObject) newInstance).setHandler(this);

            return register(id, newInstance);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T register(String id, T wrappedInstance) {

        Instance ii = new Instance(id, wrappedInstance);
        put(ii, wrappedInstance);

        return wrappedInstance;
    }


    private BiConsumer<Task, NAR> operator(Class c) {
        return (task, n) -> {

            Term taskTerm = task.term();
            TermContainer args = validArgs(Operator.args(taskTerm));
            if (args == null)
                return;

            Term method = validMethod(args.sub(0));
            if (method == null)
                return;

            Term methodArgs = args.sub(1);

            boolean maWrapped = methodArgs.op() == PROD;

            int aa = maWrapped ? methodArgs.subs() : 1;

            Object[] orgs;
            List<Class> types;
            if (aa == 0) {
                orgs = ArrayUtils.EMPTY_OBJECT_ARRAY;
                types = List.of();
            } else {
                orgs = object(maWrapped ? methodArgs.subterms().arrayShared() : new Term[]{methodArgs});
                types = typesOf(orgs);
            }

            MethodHandle mm;

            mm = methodArgCache.apply(Tuples.pair(Tuples.pair(c, method.toString()), types));
            if (mm == null)
                return;


            Atom ins = Operator.func(taskTerm);
            Object inst = termToObj.get(ins);
            assert (inst != null);

            if (invokingGoal.get() != null) {
                throw new TODO("we need a stack: " + invokingGoal.get() + " -> " + task);
            }
            invokingGoal.set(task);

            try {
                mm.bindTo(inst).invokeWithArguments(orgs);
            } catch (Throwable throwable) {
                logger.error("{} {} {} {}", task, inst, mm, args);
                throwable.printStackTrace();
            } finally {
                invokingGoal.set(null);
            }

        };
    }

    private TermContainer validArgs(TermContainer args) {
        int a = args.subs();
        if (!(a == 2 || (a == 3 && args.sub(2).op() == VAR_DEP))) {
            //this is likely a goal from the NAR to itself about a desired result state
            //used during reasoning
            //anyway it is invalid for invocation (u
            return null;
        }
        return args;
    }

    private Term validMethod(Term method) {
        if (method.op() != ATOM)
            return null;
        if (methodExclusions.contains(method.toString()))
            return null;

        //TODO check a cached list of the reflected methods of the target class

        return method;
    }


    private Class[] typesOfArray(Object[] orgs) {
        Class[] types;
        types = Util.map(x -> Primitives.unwrap(x.getClass()),
                new Class[orgs.length], orgs);
        return types;
    }

    private FasterList<Class> typesOf(Object[] orgs) {
        return new FasterList<>(typesOfArray(orgs));
    }


    private static Method findMethod(Class<?> clazz, Predicate<Method> predicate) {
//		Preconditions.notNull(clazz, "Class must not be null");
//		Preconditions.notNull(predicate, "Predicate must not be null");

        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            // Search for match in current type
            Method[] methods = current.isInterface() ? current.getMethods() : current.getDeclaredMethods();
            for (Method method : methods) {
                if (predicate.test(method)) {
                    return method;
                }
            }

            // Search for match in interfaces implemented by current type
            for (Class<?> ifc : current.getInterfaces()) {
                Method m = findMethod(ifc, predicate);
                if (m != null)
                    return m;
            }
        }

        return null;
    }

    /**
     * Determine if the supplied candidate method (typically a method higher in
     * the type hierarchy) has a signature that is compatible with a method that
     * has the supplied name and parameter types, taking method sub-signatures
     * and generics into account.
     */
    private static boolean hasCompatibleSignature(Method candidate, String method, Class<?>[] parameterTypes) {

        if (parameterTypes.length != candidate.getParameterCount()) {
            return false;
        }
        if (!method.equals(candidate.getName())) {
            return false;
        }

        // trivial case: parameter types exactly match
        Class<?>[] ctp = candidate.getParameterTypes();
        if (Arrays.equals(parameterTypes, ctp)) {
            return true;
        }
        // param count is equal, but types do not match exactly: check for method sub-signatures
        // https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> lowerType = parameterTypes[i];
            Class<?> upperType = ctp[i];
            if (!upperType.isAssignableFrom(lowerType)) {
                return false;
            }
        }
        // lower is sub-signature of upper: check for generics in upper method
        if (isGeneric(candidate)) {
            return true;
        }
        return false;
    }

    private static boolean isGeneric(Method method) {
        return isGeneric(method.getGenericReturnType())
                || Arrays.stream(method.getGenericParameterTypes()).anyMatch(OObjects::isGeneric);
    }

    private static boolean isGeneric(Type type) {
        return type instanceof TypeVariable || type instanceof GenericArrayType;
    }

    @Nullable
    @Override
    public final Object invoke(Object obj, Method wrapper, Method wrapped, Object[] args) {

        Object result;
        try {
            result = wrapped.invoke(obj, args);
        } catch (Throwable t) {
            logger.error("{} args={}: {}", obj, args, t);
            result = t;
        }

        return invoked(obj, wrapper, args, result);
    }

    @Nullable Object invoked(Object obj, Method wrapped, Object[] args, Object result) {
        if (methodExclusions.contains(wrapped.getName()))
            return result;

        Instance in = (Instance) objToTerm.get(obj);
        if (in == null)
            return result;

        return in.update(obj, wrapped, args, result);
    }


    /**
     * @see org.junit.platform.commons.support.ReflectionSupport#findMethod(Class, String, Class...)
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Preconditions.notNull(clazz, "Class must not be null");
        Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
        Preconditions.containsNoNullElements(parameterTypes, "Individual parameter types must not be null");

        return findMethod(clazz, method -> hasCompatibleSignature(method, methodName, parameterTypes));
    }
}
