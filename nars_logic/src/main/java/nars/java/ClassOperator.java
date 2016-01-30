package nars.java;

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

import static nars.java.NALObjects.isMethodVisible;

/**
 * Created by me on 1/29/16.
 */
public class ClassOperator extends TermFunction {

    private final Class klass;
    final Map<Term, MethodOperator> methods;

    public ClassOperator(Class c, AtomicBoolean enableInvoke, NALObjects context) {
        super(c.getSimpleName());
        this.klass = c;
        methods = Global.newHashMap();

        //add operators for public methods
        for (Method m : c.getMethods()) {
            if (isMethodVisible(m) && Modifier.isPublic(m.getModifiers())) {
                methods.computeIfAbsent(methodTerm(m), M -> {
                    return new MethodOperator(enableInvoke, m, context);
                });
            }
        }
    }

    public static Term methodTerm(Method m) {
        return $.the(m.getName());
    }

    @Override
    public final void execute(@NotNull Execution e) {
        ThreadLocal<Task> localTask = MethodOperator.currentTask;

        localTask.set(e.task); //HACK

        super.execute(e);

        localTask.set(null);
    }

    @Nullable
    @Override
    public final Object function(Compound x, TermBuilder i) {
        Term methodTerm = x.term(0);
        MethodOperator methFunc = methods.get(methodTerm);
        if (methFunc!=null) {
            return methFunc.function(x, i);
        }
        return null;
    }
}
