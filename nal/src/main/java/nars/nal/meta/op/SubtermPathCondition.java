package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.Derivation;
import nars.nal.meta.TaskBeliefSubterms;
import nars.term.Compound;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Created by me on 10/6/16.
 */
public abstract class SubtermPathCondition extends AtomicBoolCondition {
    @NotNull
    public final byte[] aPath;
    @NotNull
    public final byte[] bPath;
    //0=task, 1=belief term pattern
    protected final int a;
    protected final int b;
    @NotNull
    protected final String id;

    public SubtermPathCondition(@NotNull TaskBeliefSubterms x) {
        this(x.aPath, x.a, x.bPath, x.b);
    }

    public SubtermPathCondition(@NotNull byte[] aPath, int a, @NotNull byte[] bPath, int b) {
        this.bPath = bPath;
        this.a = a;
        this.aPath = aPath;
        this.b = b;
        String s = getClass().getSimpleName() + '(' +
                Integer.toString(a) + ((aPath.length > 0) ?  ':' + Arrays.toString(aPath) + ',' : ",") +
                Integer.toString(b) + ((bPath.length > 0) ?  ':' + Arrays.toString(bPath) : "")+
                ')';
        s = s.replace('[', '(').replace(']',')'); //make the path into a product ( )
        this.id = s;
    }


    @Nullable
    public static Term resolve(@NotNull Derivation ff, int aOrB, @NotNull byte[] path) {
        return resolve(aOrB == 0 ? ff.taskTerm : ff.beliefTerm, path);
    }

    public static Term resolve(@NotNull Term ca, @NotNull byte[] aPath) {
        if (aPath.length == 0)
            return ca;
        else {
            return (ca instanceof Compound) ? ((Compound) ca).subterm(aPath) : null;
        }
    }

    public static @Nullable byte[] nonCommutivePathTo(@NotNull Term term, @NotNull Term arg1) {
        byte[] p = term.pathTo(arg1);
        if (p == null) return null;
        if (p.length == 0) return p;
        //verify that the path does not select a subterm of a commutive term

        for (int i = 0; i < 1 + p.length; i++) {
            Term s = ((Compound) term).subterm(ArrayUtils.subarray(p, 0, i));
            if (s.isCommutative())
                return null;
        }
        return p;
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean run(@NotNull Derivation ff, int now) {

        Term ta;
        if ((ta = resolve(ff, a, aPath)) == null)
            return false;

        Term tb;
        if ((tb = resolve(ff, b, bPath)) == null)
            return false;

        return eval(ta, tb);
    }

    protected abstract boolean eval(Term a, Term b);
}
