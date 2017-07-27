//package nars.op.java;
//
//import com.github.drapostolos.typeparser.TypeParser;
//import nars.Op;
//import nars.concept.OperationConcept;
//import nars.task.Task;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.TermIndex;
//import nars.truth.Truth;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.lang.reflect.Parameter;
//import java.util.Arrays;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//
///**
// * Created by me on 8/19/15.
// */
//public class MethodOperator  {
//
//    static final TypeParser parser = TypeParser.newBuilder().builder();
//    final static Logger logger = LoggerFactory.getLogger(MethodOperator.class);
//
//    @NotNull
//    private final Method method;
//    private final Parameter[] params;
//
//    private static final Object[] empty = new Object[0];
//    private final AtomicBoolean enable;
//    private final Lobjects context;
//    boolean feedback = true;
//
//    //public static final Atom ERROR = Atom.the("ERR");
//
//    @Nullable
//    public static final ThreadLocal<OperationConcept> currentTask = new ThreadLocal();
//
//    private static final boolean strict = false;
//
//    public MethodOperator(AtomicBoolean enable, @NotNull Method m, Lobjects context) {
//        //super(getParentMethodName(m));
//        /*
//            Class<?> sc = m.getDeclaringClass();
//
//            String superClass = sc.getSimpleName();
//            String methodName = m.getName();
//            return (superClass + '_' + methodName);
//         */
//
//        this.context = context;
//        method = m;
//        params = method.getParameters();
//        this.enable = enable;
//    }
//
//    public static OperationConcept invokingTask() {
//        return currentTask.get();
//    }
//
////    @NotNull
////    private static String getParentMethodName(@NotNull Method m) {
////        Class<?> sc = m.getDeclaringClass();
////
////        String superClass = sc.getSimpleName();
////        String methodName = m.getName();
////        return (superClass + '_' + methodName);
////    }
//
//
//    @Nullable
//    public Object function(@NotNull Compound o, TermIndex ti) {
//
//        if (!enable.get())
//            return null;
//
//        OperationConcept curTask = currentTask.get();
//        Object ll = null; //TODO curTask.lastLogged();
//
//        //Check if this was previously executed by the Java invocation pathway
//        if (ll instanceof Lobjects.JavaInvoked)
//            return ll; //signals already invoked
//
//        Term[] x = o.terms();
//
//        int pc = method.getParameterCount();
//        int requires, paramOffset;
//        if (Modifier.isStatic(method.getModifiers())) {
//            requires = 1 + 1;
//            paramOffset = 1;
//        }
//        else {
//            requires = 1 + 1 + 1;
//            paramOffset = 2;
//        }
//
//        if (x.length < requires) {
//            if (strict)
//                throw new RuntimeException("invalid argument count: needs " + requires + " but has " + Arrays.toString(x));
//            else
//                return null;
//        }
//
//        Object instance = paramOffset == 0 ? null : context.object(x[paramOffset-1]);
//        Lobjects ctx = this.context;
//
//        Object[] args;
//        if (pc == 0) {
//            args = empty;
//        }
//        else {
//            args = new Object[pc];
//
//
//            Term xv = x[paramOffset];
//            if (!(xv.op() == Op.PRODUCT)) {
//                if (strict)
//                    throw new RuntimeException("method parameters must be a product but is " + xv);
//                else
//                    return null;
//            }
//
//            Compound pxv = (Compound)xv;
//            if (pxv.size()!=pc) {
//                if (strict)
//                    throw new RuntimeException("invalid # method parameters; requires " + pc + " but " + pxv.size() + " given");
//                else
//                    return null;
//            }
//
//            Parameter[] par = this.params;
//            for (int i = 0; i < pc; i++) {
//                Term tt = pxv.term(i);
//                if (tt.op().isVar())
//                    return null;
//
//                Object a = ctx.object(tt);
//                Class<?> pt = par[i].getType();
//                if (!pt.isAssignableFrom(a.getClass())) {
//                    a = parser.parseType(a.toString(), pt);
//                }
//
//                args[i] = a;
//            }
//        }
//
//        try {
//
//            //Object result = Invoker.invoke(instance, method.getName(), args); /** from Boon library */
//
//
//
//
//
//            Object result = context.invokeVolition(curTask, method, instance, args);
//
//
//            if (feedback) {
//                if (result == null || (result instanceof Truth) || (result instanceof Task))
//                    return result; //raw values: null, truth value, or task
//                else
//                    return ctx.term(result); //termize it
//            } else {
//                return null;
//            }
//
//        } catch (Throwable e) {
//
//            logger.error("{}",e);
//
//            //System.err.println(method + " <- " + instance + " (" + instance.getClass() + " =?= " + method.getDeclaringClass() + "\n\t<<< " + Arrays.toString(args));
//            //nar.memory.eventError.emit(e);
//            context.volition.set(null);
//
//
//            return null;
//            //else
//              //  return context.term(e);
//        }
//
//    }
//
//    static void setCurrentTask(@Nullable Task m) {
//        OperationConcept ct = currentTask.get();
//
//        if (m == ct) return;
//
//        if ((m!=null && ct !=null) || (m == null && ct == null))
//            throw new RuntimeException("recursive invocation: " + ct + " vs. " + m);
//
//        currentTask.set(m);
//    }
//
//}
