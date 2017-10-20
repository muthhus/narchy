package nars.op.java;

import com.google.common.primitives.Primitives;
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
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.map.CustomConcurrentHashMap.*;
import static nars.Op.ATOM;
import static nars.Op.PROD;
import static nars.Op.VAR_DEP;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP;


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


    @Nullable Task feedback(Object instance, Method method, Object[] args, boolean explicit, Object result) {
        if (methodExclusions.contains(method.getName()))
            return null;

        Atom in = (Atom) instances.inverse().get(instance);

        Term[] targs = opTerms(method, instance, args, result);

        Term op = $.func(in, targs);

        TaskBuilder g = $.belief(op,
                invocationGoalFreq, invocationGoalConf).
                present(nar);
        g.log("Invoked" /* via VM */);
        return g.apply(nar);
    }

    @NotNull
    private Term[] opTerms(Method method, Object instance, Object[] args, Object result) {

        //TODO handle static methods

        boolean isVoid = method.getReturnType() == void.class;

        Term[] x = new Term[isVoid ? 2 : 3];
        x[0] = $.the(method.getName());
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

        Atom o = (Atom) $.the(id);

        Class clazz = proxyCache.computeIfAbsent(instance, (c) -> {
            ProxyFactory p = new ProxyFactory();
            p.setSuperclass(c);


            nar.onOp(id, new Operator.AtomicExec(operator(c), 0.5f));

            return p.createClass();
        });


        try {
            return the(o, (T) clazz.getConstructors()[0].newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
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
                orgs = object(maWrapped ? methodArgs.subterms().theArray() : new Term[] { methodArgs });
                types = Util.map(x -> Primitives.unwrap(x.getClass()),
                        new Class[orgs.length], orgs);
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
                Object inst = instances.get(ins);
                assert(inst!=null);
                mm.bindTo(inst).invokeWithArguments(orgs);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                invokingGoal.set(null);
            }

        };
    }



	private static Method findMethod(Class<?> clazz, Predicate<Method> predicate) {
		Preconditions.notNull(clazz, "Class must not be null");
		Preconditions.notNull(predicate, "Predicate must not be null");

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
				if (m!=null)
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
		if (!methodName.equals(candidate.getName())) {
			return false;
		}
		if (parameterTypes.length != candidate.getParameterCount()) {
			return false;
		}
		// trivial case: parameter types exactly match
		if (Arrays.equals(parameterTypes, candidate.getParameterTypes())) {
			return true;
		}
		// param count is equal, but types do not match exactly: check for method sub-signatures
		// https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> lowerType = parameterTypes[i];
			Class<?> upperType = candidate.getParameterTypes()[i];
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

	static boolean isGeneric(Method method) {
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

        Task cause = invokingGoal.get();

        boolean explicit = cause != null;

        Task fb = feedback(obj, wrapped, args, explicit, result);
        if (fb!=null) {

            if (explicit) {
                ((NALTask) fb).causeMerge(cause);
                ((NALTask) fb).priMax(cause.priElseZero());
            } else {
                fb.priMax(nar.priDefault(fb.punc()));
            }

            nar.input(fb);
        }

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
