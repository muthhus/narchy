package nars.op.sys.java;

import nars.$;
import nars.Global;
import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.TermFunction;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.op.sys.java.Lobjects.isMethodVisible;

/**
 * Reacts to operations with 4 arguments:
 *   1. method identifier (Atom)
 *   2. instance identifier (Atom)
 *   3. arguments (Product)
 *   4. result varaible (VarDep)
 *
 * The method identifier selects which MethodOperator to use as an invocation function
 */
public class ClassOperator extends TermFunction {

    @NotNull
    public final Class klass;
    @NotNull
    final Map<Term, MethodOperator> methods;

    public ClassOperator(@NotNull Class c, AtomicBoolean enableInvoke, Lobjects context) {
        super(c.getSimpleName());
        this.klass = c;
        methods = Global.newHashMap();

        //add operators for public methods
        for (Method m : c.getMethods()) {
            if (isMethodVisible(m) && Modifier.isPublic(m.getModifiers())) {
                methods.computeIfAbsent(methodTerm(m), M -> new MethodOperator(enableInvoke, m, context));
            }
        }
    }

    public static Term methodTerm(@NotNull Method m) {
        return $.the(m.getName());
    }

    @Override
    public final void execute(@NotNull Task e) {
        ThreadLocal<Task> localTask = MethodOperator.currentTask;

        localTask.set(e);

        super.execute(e);

        localTask.set(null);
    }

    @Override protected final void feedback(@NotNull Task e, Object y) {

        if (y instanceof Lobjects.JavaInvoked)
            return; //ignore, it has already been reported

        super.feedback(e, y);
    }

    @Nullable
    @Override
    public final Object function(@NotNull Compound x, TermBuilder i) {
        if (x.size() < 4) {
            //see arguments in this class's documentation
            return null;
        }
        MethodOperator methFunc = methods.get(x.term(0));
        return methFunc != null ? methFunc.function(x, i) : null;
    }
}
