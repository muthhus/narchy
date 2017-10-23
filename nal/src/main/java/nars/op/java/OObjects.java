package nars.op.java;

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import jcog.Util;
import jcog.map.CustomConcurrentHashMap;
import nars.*;
import nars.op.Operator;
import nars.task.LatchingSignalTask;
import nars.task.NALTask;
import nars.task.SignalTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.map.CustomConcurrentHashMap.*;
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
    final static Map<Term, Method> methodCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64); //cache: (class,method) -> Method


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

    public OObjects(NAR n) {
        nar = n;
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

    class Instance extends Atom {

        /**
         * reference to the actual object
         */
        public final Object object;



        /**
         * current (previous) value
         */
        public final ConcurrentHashMap<Method, ValueSignalTask> value = new ConcurrentHashMap();

        public Instance(String id, Object object) {
            super(id);
            this.object = object;

            nar.onOp(this, new Operator.AtomicExec(operator(object.getClass()), 0.5f));
        }

        @NotNull
        private Term[] opTerms(Method method, Object[] args, Object result) {

            //TODO handle static methods

            boolean isVoid = method.getReturnType() == void.class;

            Term[] x = new Term[isVoid ? 2 : 3];
            x[0] = $.the(method.getName());
            switch (args.length) {
                case 0:
                    x[1] = Op.ZeroProduct; break;
                case 1:
                    x[1] = OObjects.this.term(args[0]); break; /* unwrapped singleton */
                default:
                    x[1] = $.p(terms(args)); break;
            }
            assert(x[1]!=null): "could not termize: " + Arrays.toString(args);
            if (!isVoid) {
                x[2] = OObjects.this.term(result);
                assert(x[2]!=null): "could not termize: " + result;
            }

            return x;
        }

        public Object update(Object obj, Method method, Object[] args, Object nextValue) {
            Task cause = invokingGoal.get();
            boolean explicit = cause != null;

            float pri = nar.priDefault(BELIEF);
            long now = nar.time();


            ValueSignalTask p1 = value.get(method);
            if (p1!=null && Objects.equals(p1.value, nextValue)) {
                //just continue the existing task
                p1.priMax(pri); //rebudget
                p1.grow(now);
                return nextValue;
            }

            Term f = $.func(id, opTerms(method, args, nextValue)).normalize();

            ValueSignalTask next = new ValueSignalTask(f,
                    BELIEF, $.t(invocationBeliefFreq,nar.confDefault(BELIEF)),
                    now, now, nar.time.nextStamp(), nextValue);

            if (Param.DEBUG)
                next.log("Invocation" /* via VM */);

            if (explicit) {
                next.causeMerge(cause);
                next.priMax(cause.priElseZero());
                cause.pri(0); //drain
                cause.meta("@", next);
            } else {
                next.priMax(pri);
            }


            SignalTask prev = value.put(method, next);

            if (prev!=null) {
                prev.end(now); //dont need to re-input prev, this takes care of it
                next.priMax(pri);

                NALTask prevEnd = new NALTask(prev.term(),
                        BELIEF, $.t(1f - invocationBeliefFreq, nar.confDefault(BELIEF)),
                        now, now, now, nar.time.nextInputStamp());
                prevEnd.priMax(pri);
                if (Param.DEBUG)
                    prevEnd.log("Invoked");

                nar.input(prevEnd);
            }

            nar.input(next);

            return nextValue;
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
     * creates a new instance to be managed by this
     */
    @NotNull
    public <T> T the(String id, Class<? extends T> instance, Object... args) {

        Class clazz = proxyCache.computeIfAbsent(instance, (c) -> {
            ProxyFactory p = new ProxyFactory();
            p.setSuperclass(c);
            return p.createClass();
        });


        try {

            T wrappedInstance = (T) clazz.getDeclaredConstructor(typesOf(args)).newInstance(args);

            ((ProxyObject) wrappedInstance).setHandler(this);

            Instance ii = new Instance(id, wrappedInstance);
            put(ii, wrappedInstance);

            return wrappedInstance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    private BiConsumer<Task, NAR> operator(Class c) {
        return (task, n) -> {

            Term taskTerm = task.term();
            TermContainer args = Operator.args(taskTerm);
            int a = args.subs();
            if (!(a == 2 || (a == 3 && args.sub(2).op() == VAR_DEP))) {
                //this is likely a goal from the NAR to itself about a desired result state
                //used during reasoning
                //anyway it is invalid for invocation (u
                return;
            }


            Term method = args.sub(0);
            if (method.op() != ATOM)
                return;

            Term methodArgs = args.sub(1);

            boolean maWrapped = methodArgs.op() == PROD;

            int aa = maWrapped ? methodArgs.subs() : 1;

            Object[] orgs;
            Class[] types;
            if (aa == 0) {
                orgs = ArrayUtils.EMPTY_OBJECT_ARRAY;
                types = ArrayUtils.EMPTY_CLASS_ARRAY;
            } else {
                orgs = object(maWrapped ? methodArgs.subterms().theArray() : new Term[]{methodArgs});
                types = typesOf(orgs);
            }

            MethodHandle mm;
            try {

                Method x = findMethod(c, method.toString(), types);

                if (x == null) {
                    x = findMethod(Object.class, method.toString(), types);
                }

                if (x == null)
                    return;

                x.trySetAccessible();

                mm = MethodHandles.lookup().unreflect(x);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }

            if (mm == null)
                return;


            invokingGoal.set(task);
            try {
                Atom ins = Operator.func(taskTerm);
                Object inst = termToObj.get(ins);
                assert (inst != null);
                mm.bindTo(inst).invokeWithArguments(orgs);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                invokingGoal.set(null);
            }

        };
    }

    private Class[] typesOf(Object[] orgs) {
        Class[] types;
        types = Util.map(x -> Primitives.unwrap(x.getClass()),
                new Class[orgs.length], orgs);
        return types;
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
    private static boolean hasCompatibleSignature(Method candidate, String methodName, Class<?>[] parameterTypes) {

        if (parameterTypes.length != candidate.getParameterCount()) {
            return false;
        }
        if (!methodName.equals(candidate.getName())) {
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
    public final Object invoke(Object obj, Method wrapped, Method wrapper, Object[] args) throws Throwable {

        Object result = wrapper.invoke(obj, args);

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
        Preconditions.notBlank(methodName, "Method name must not be null or blank");
        Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
        Preconditions.containsNoNullElements(parameterTypes, "Individual parameter types must not be null");

        return findMethod(clazz, method -> hasCompatibleSignature(method, methodName, parameterTypes));
    }
}
