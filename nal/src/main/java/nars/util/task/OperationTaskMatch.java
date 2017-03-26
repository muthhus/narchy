package nars.util.task;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 6/1/16.
 */
abstract public class OperationTaskMatch extends TaskMatch {

    @NotNull
    private final ObjectIntHashMap argIndex;
    private final int numArgs;

    public OperationTaskMatch(@NotNull Compound pattern, @NotNull NAR n) {
        super(pattern, n);
        if (!Op.isOperation(pattern))
            throw new RuntimeException(pattern + " is not an operation compound pattern");

        this.argIndex = new ObjectIntHashMap<>();
        Compound args = (Compound) pattern.term(0);
        int i = 0;
        this.numArgs = args.size();
        for (Term t : args.terms()) {
            argIndex.put(t, i++);
        }


    }

    @Override
    protected final void eachMatch(Task task, @NotNull Map<Term, Term> xy) {
        Term[] args = new Term[numArgs];
        xy.forEach((k, v) -> {
            int i = argIndex.getIfAbsent(k, -1);
            if (i != -1) {
                args[i] = v;
            }
        });
        onMatch(args);
    }

    protected abstract void onMatch(Term[] args);
}
