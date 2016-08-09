package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Created by me on 12/17/15.
 */
public final class TermNotEquals extends AtomicBoolCondition {

    public final byte[] aPath;
    public final byte[] bPath;

    //0=task, 1=belief term pattern
    private final int a, b;
    @NotNull
    private final String id;

    /** TODO the shorter path should be set for 'a' if possible, because it will be compared first */
    public TermNotEquals(int a, byte[] aPath, int b, byte[] bPath) {
        this.a = a;
        this.aPath = aPath;
        this.b = b;
        this.bPath = bPath;
        String s = getClass().getSimpleName() + '(' +
                        Integer.toString(a) + ':' + Arrays.toString(aPath) + ',' +
                        Integer.toString(b) + ':' + Arrays.toString(bPath) +
                ')';
        s = s.replace('[', '(').replace(']',')'); //make the path into a product ( )
        this.id = s;
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        Term ca = a == 0 ? ff.taskTerm : ff.beliefTerm;
        Term cb = b == 0 ? ff.taskTerm : ff.beliefTerm;

        Term ta;
        if ((ta = resolve(ca, aPath)) == null)
            return false;
        Term tb;
        if ((tb = resolve(cb, bPath)) == null)
            return false;

        return !ta.equals(tb);
    }

    public static Term resolve(@NotNull Term ca, @NotNull byte[] aPath) {
        if (aPath.length == 0)
            return ca;
        else {
            return (ca instanceof Compound) ?
                    ((Compound) ca).subterm(aPath) : null;
        }
    }

}
