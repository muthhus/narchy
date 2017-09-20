package nars.derive;

import jcog.Util;
import jcog.math.ByteShuffler;
import nars.$;
import nars.control.Cause;
import nars.control.CauseChannel;
import nars.control.Derivation;
import nars.derive.op.UnifyTerm;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.function.Function;

/** AIKR value-determined fork (aka choice-point) */
public class ValueFork extends Fork {

    final Taskify[] conc;
//    final ValueCache values;

    /** the term which a derivation will encounter signaling
     * that it may continue here after evaluating it among other choices */
    public final Can can;
    private final RoaringBitmap downstream;

    /** the causes that this is responsible for, ie. those that may be caused by this */
    public final Cause[] causes;


    public static ValueFork the(PrediTerm[] branches, List<ValueFork> choices, RoaringBitmap downstream) {
        int branchID = choices.size();
        Can can = new Can(branchID, downstream);
        ValueFork v = new ValueFork(branches, can, downstream);
        choices.add(v);
        return v;
    }

    protected ValueFork(PrediTerm[] branches, Can can, RoaringBitmap downstream) {
        super(branches);

        this.can = can;
        this.downstream = downstream;

        conc = new Taskify[branches.length];
        int n = 0;
        for (PrediTerm b : branches) {
            PrediTerm fin = AndCondition.last(b);
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude)fin;
            conc[n++] = ((Taskify)(AndCondition.last(u.eachMatch)));
        }

        causes = Util.map(c->c.channel, new Cause[n], conc);
//        values = new ValueCache(c -> c::value, causes);
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new ValueFork(transformedBranches(f), can, downstream);
    }

    @Override
    public boolean test(@NotNull Derivation d) {

        long now = d.time;
//        values.update(now);

        int before = d.now();

        int branches = cache.length;
        if (branches == 1) {
            cache[0].test(d);
            return d.revertLive(before);
        } else {
            ByteShuffler b = d.shuffler;
            byte[] order = b.shuffle(d.random, branches, true); //must get a copy because recursion will re-use the shuffler's internal array

            int ttl = d.ttl;

            for (int i = 0; i < branches; i++) {

//                int subTTL = Math.round(ttl * (value[i] / valueTotal));
//                int reserve = d.getAndSetTTL(subTTL) - subTTL;

                cache[order[i]].test(d);

//                d.addTTL(reserve);

                if (!d.revertLive(before))
                    return false;
            }
        }
        return d.live();
    }


    /** remembers the possiblity of a choice which "can" be pursued
     * (ie. according to value rank) */
    static class Can extends AbstractPred<Derivation> {

        public final int id;

        /** global cause channel ID's that this leads to */
        private final RoaringBitmap downstream;

        protected Can(int id, RoaringBitmap downstream) {
            super($.func("can", /*$.the(id),*/ $.sete(downstream)));

            this.id = id;
            this.downstream = downstream;
        }

        @Override
        public boolean test(Derivation derivation) {
            derivation.canChoose(id);
            return true;
        }
    }


//        @Override
//        public String toString() {
//            return id + "(to=" + cache.length + ")";
//        }
}
