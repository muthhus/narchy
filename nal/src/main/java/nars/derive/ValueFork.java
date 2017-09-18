package nars.derive;

import jcog.math.ByteShuffler;
import nars.control.Derivation;
import nars.derive.op.UnifySubtermThenConclude;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static nars.time.Tense.ETERNAL;

/** AIKR value-determined fork (aka choice-point) */
public class ValueFork extends Fork {

    //private final int id;
    final MakeTask[] conc;

    /** cache of value vaulues */
    final float[] value;

    final AtomicLong now = new AtomicLong(ETERNAL);
    private float valueTotal = 0;
    final static float epsilon = 0.01f;

    public ValueFork(PrediTerm[] branches) {
        super(branches);
        conc = new MakeTask[branches.length];
        value = new float[branches.length];
        int n = 0;
        for (PrediTerm b : branches) {
            PrediTerm fin = AndCondition.last(b);
            UnifySubtermThenConclude u = (UnifySubtermThenConclude)fin;
            conc[n++] = ((MakeTask)(AndCondition.last(u.eachMatch)));
        }
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new ValueFork(transformedBranches(f));
    }

    private void update() {
        int i = 0;
        float total = 0;
        for (MakeTask c : conc) {
            float v = value[i++] = Math.max(c.channel.gain(), epsilon);
            total += v;
        }
        this.valueTotal = total;
    }

    @Override
    public boolean test(@NotNull Derivation d) {

        long now = d.time;
        if (this.now.getAndSet(now) != now) {
            update();
        }

        int before = d.now();

        int branches = cache.length;
        if (branches == 1) {
            cache[0].test(d);
            return d.revertAndContinue(before);
        } else {
            ByteShuffler b = d.shuffler;
            byte[] order = b.shuffle(d.random, branches, true); //must get a copy because recursion will re-use the shuffler's internal array

            int ttl = d.ttl;

            for (int i = 0; i < branches; i++) {

//                int subTTL = Math.round(ttl * (value[i] / valueTotal));
//                int reserve = d.getAndSetTTL(subTTL) - subTTL;

                cache[order[i]].test(d);

//                d.addTTL(reserve);

                if (!d.revertAndContinue(before))
                    return false;
            }
        }
        return true;
    }




//        @Override
//        public String toString() {
//            return id + "(to=" + cache.length + ")";
//        }
}
