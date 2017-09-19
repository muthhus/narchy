package nars.derive;

import jcog.Util;
import jcog.math.ByteShuffler;
import jcog.math.FloatSupplier;
import nars.$;
import nars.control.CauseChannel;
import nars.control.Derivation;
import nars.derive.op.UnifyTerm;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.function.Function;

/** AIKR value-determined fork (aka choice-point) */
public class ValueFork extends Fork {

    //private final int id;
    final Taskify[] conc;
    final ValueCache values;

    /** the term which a derivation will encounter signaling
     * that it may continue here after evaluating it among other choices */
    public final Choice choice;
    private final RoaringBitmap downstream;

    /** the causes that this is responsible for, ie. those that may be caused by this */
    public final CauseChannel[] causes;


    public static ValueFork the(PrediTerm[] branches, List<ValueFork> choices, RoaringBitmap downstream) {
        int branchID = choices.size();
        Choice choice = new Choice(branchID, downstream);
        ValueFork v = new ValueFork(branches, choice, downstream);
        choices.add(v);
        return v;
    }

    protected ValueFork(PrediTerm[] branches, Choice choice, RoaringBitmap downstream) {
        super(branches);

        this.choice = choice;
        this.downstream = downstream;

        conc = new Taskify[branches.length];
        int n = 0;
        for (PrediTerm b : branches) {
            PrediTerm fin = AndCondition.last(b);
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude)fin;
            conc[n++] = ((Taskify)(AndCondition.last(u.eachMatch)));
        }

        causes = Util.map(c->c.channel, new CauseChannel[n], conc);
        values = new ValueCache(c -> c::value, causes);
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new ValueFork(transformedBranches(f), choice, downstream);
    }

    private void update() {
        values.update();
    }

    @Override
    public boolean test(@NotNull Derivation d) {

        long now = d.time;
        values.update(now);

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


    /** remembers the possiblity of a choice which can be pursued
     * (ie. according to value rank) */
    static class Choice extends AbstractPred<Derivation> {

        public final int id;

        /** global cause channel ID's that this leads to */
        private final RoaringBitmap downstream;

        protected Choice(int id, RoaringBitmap downstream) {
            super($.func("try", /*$.the(id),*/ $.sete(downstream)));

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
