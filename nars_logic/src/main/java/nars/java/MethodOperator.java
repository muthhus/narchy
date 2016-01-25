package nars.java;

import com.github.drapostolos.typeparser.TypeParser;
import nars.Global;
import nars.Op;
import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.TermFunction;
import nars.task.Task;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.compound.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by me on 8/19/15.
 */
public class MethodOperator extends TermFunction {


    static final TypeParser parser = TypeParser.newBuilder().build();

    @NotNull
    private final Method method;
    private final Parameter[] params;

    private static final Object[] empty = new Object[0];
    private final AtomicBoolean enable;
    private final NALObjects context;
    boolean feedback = true;

    //public static final Atom ERROR = Atom.the("ERR");

    @Nullable
    private static final ThreadLocal<Task> currentTask = new ThreadLocal();

    private static final boolean strict = false;

    public MethodOperator(AtomicBoolean enable, @NotNull Method m, NALObjects context) {
        super(getParentMethodName(m));

        this.context = context;
        method = m;
        params = method.getParameters();
        this.enable = enable;
    }

    public static Task invokingTask() {
        return currentTask.get();
    }

    @NotNull
    private static String getParentMethodName(@NotNull Method m) {
        Class<?> sc = m.getDeclaringClass();

        String superClass = sc.getSimpleName();
        String methodName = m.getName();
        return (superClass + '_' + methodName);
    }

    @Override
    public void execute(@NotNull Execution e) {
        ThreadLocal<Task> localTask = MethodOperator.currentTask;

        localTask.set(e.task); //HACK

        super.execute(e);

        localTask.set(null);
    }


    @Nullable
    @Override
    public Object function(@NotNull Compound o, TermBuilder ti) {

        if (!enable.get())
            return null;

        Term[] x = o.terms();

        int pc = method.getParameterCount();
        int requires, paramOffset;
        if (Modifier.isStatic(method.getModifiers())) {
            requires = 1;
            paramOffset = 0;
        }
        else {
            requires = 1 + 1;
            paramOffset = 1;
        }

        if (x.length < requires) {
            if (strict)
                throw new RuntimeException("invalid argument count: needs " + requires + " but has " + Arrays.toString(x));
            else
                return null;
        }

        Object instance = paramOffset == 0 ? null : context.object(x[0]);
        NALObjects ctx = this.context;

        Object[] args;
        if (pc == 0) {
            args = empty;
        }
        else {
            args = new Object[pc];


            Term xv = x[paramOffset];
            if (!xv.op(Op.PRODUCT)) {
                if (strict)
                    throw new RuntimeException("method parameters must be a product but is " + xv);
                else
                    return null;
            }

            Compound pxv = (Compound)xv;
            if (pxv.size()!=pc) {
                if (strict)
                    throw new RuntimeException("invalid # method parameters; requires " + pc + " but " + pxv.size() + " given");
                else
                    return null;
            }

            Parameter[] par = this.params;
            for (int i = 0; i < pc; i++) {
                Object a = ctx.object(pxv.term(i));
                Class<?> pt = par[i].getType();
                if (!pt.isAssignableFrom(a.getClass())) {
                    a = parser.parseType(a.toString(), pt);
                }

                args[i] = a;
            }
        }

        try {

            //Object result = Invoker.invoke(instance, method.getName(), args); /** from Boon library */


            Task curTask = currentTask.get();

            Object ll = curTask.getLogLast();
            Object result = ll instanceof NALObjects.InvocationResult ?
                    ((NALObjects.InvocationResult) ll).value :
                    context.invokeVolition(curTask, method, instance, args);


            if (feedback) {
                if (result instanceof Truth)
                    return result; //raw truth value
                else
                    return ctx.term(result); //termize it
            } else {
                return null;
            }

        } catch (Throwable e) {
            //System.err.println(method + " <- " + instance + " (" + instance.getClass() + " =?= " + method.getDeclaringClass() + "\n\t<<< " + Arrays.toString(args));
            //nar.memory.eventError.emit(e);
            context.volition.set(null);

            if (Global.DEBUG)
                throw new RuntimeException(e);
            else
                return context.term(e);
        }

    }

    static void setCurrentTask(Task m) {
        Task ct = currentTask.get();

        if (m == ct) return;

        if ((m!=null && ct !=null) || (m == null && ct == null))
            throw new RuntimeException("recursive invocation: " + ct + " vs. " + m);

        currentTask.set(m);
    }

}
