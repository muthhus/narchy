package nars.derive;

import jcog.math.ByteShuffler;
import nars.$;
import nars.control.Derivation;
import nars.derive.op.UnifyTerm;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static nars.time.Tense.ETERNAL;

/** AIKR value-determined fork (aka choice-point) */
public class ValueFork extends Fork {

    //private final int id;
    final Taskify[] conc;

    /** cache of value vaulues */
    final float[] value;

    final AtomicLong now = new AtomicLong(ETERNAL);

    /** the term which a derivation will encounter signaling
     * that it may continue here after evaluating it among other choices */
    public final Choice choice;

    private float valueTotal = 0;
    final static float epsilon = 0.01f;

    public static ValueFork the(PrediTerm[] branches, List<ValueFork> choices) {
        int choiceID = choices.size();
        Choice choice = new Choice(choiceID);
        ValueFork v = new ValueFork(branches, choice);
        choices.add(v);
        return v;
    }

    protected ValueFork(PrediTerm[] branches, Choice choice) {
        super(branches);
        this.choice = choice;

        conc = new Taskify[branches.length];
        value = new float[branches.length];
        int n = 0;
        for (PrediTerm b : branches) {
            PrediTerm fin = AndCondition.last(b);
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude)fin;
            conc[n++] = ((Taskify)(AndCondition.last(u.eachMatch)));
        }
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new ValueFork(transformedBranches(f), choice);
    }

    private void update() {
        int i = 0;
        float total = 0;
        for (Taskify c : conc) {
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


    /** remembers the possiblity of a choice which can be pursued
     * (ie. according to value rank) */
    public static class Choice extends AbstractPred<Derivation> {

        public final int id;

        protected Choice(int id) {
            super($.func("choice", $.the(id)));
            this.id = id;
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
