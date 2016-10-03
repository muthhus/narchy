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

    public static TermNotEquals the(int a, byte[] aPath, int b, byte[] bPath) {
        //sort
        if (a < b) {
            return new TermNotEquals(a, aPath, b, bPath);
        } else if (a > b) {
            return new TermNotEquals(b, bPath, a, aPath);
        } else {
            //sort by the path
            int pc = Arrays.compare(aPath, bPath);
            if (pc < 0) {
                return new TermNotEquals(a, aPath, b, bPath);
            } else if (pc > 0) {
                return new TermNotEquals(b, bPath, a, aPath);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /** TODO the shorter path should be set for 'a' if possible, because it will be compared first */
    protected TermNotEquals(int a, byte[] aPath, int b, byte[] bPath) {
        this.a = a;
        this.aPath = aPath;
        this.b = b;
        this.bPath = bPath;
        String s = getClass().getSimpleName() + '(' +
                        Integer.toString(a) + ((aPath.length > 0) ?  ':' + Arrays.toString(aPath) + ',' : ",") +
                        Integer.toString(b) + ((bPath.length > 0) ?  ':' + Arrays.toString(bPath) : "")+
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
    public boolean run(@NotNull PremiseEval ff, int now) {

        Term ta;
        if ((ta = resolve(ff, a, aPath)) == null)
            return false;

        Term tb;
        if ((tb = resolve(ff, b, bPath)) == null)
            return false;

        return !ta.equals(tb);
    }

    public static Term resolve(@NotNull PremiseEval ff, int aOrB, byte[] path) {
        return resolve(aOrB == 0 ? ff.taskTerm : ff.beliefTerm, path);
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
