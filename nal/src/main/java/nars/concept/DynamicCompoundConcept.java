package nars.concept;

import nars.*;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.nal.Stamp;
import nars.task.RevisionTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static nars.Op.NEG;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;

/**
 * Adds support for dynamically calculated truth values
 */
public class DynamicCompoundConcept extends CompoundConcept {

    public final NAR nar;

    public DynamicCompoundConcept(Compound term, @NotNull Bag termLinks, @NotNull Bag taskLinks, @NotNull NAR nar) {
        super(term, termLinks, taskLinks, nar);
        this.nar = nar;
    }

    public static final class DynTruth {
        @NotNull final List<Truth> t;
        @Nullable final List<Task> e;
        @Nullable final Budget b;

        public DynTruth(List<Truth> t, List<Task> e, Budget b) {
            this.t = t;
            this.e = e;
            this.b = b;
        }

        @Nullable public long[] evidence() {
            return e == null ? null : Stamp.zip((Collection) e);
        }

        @Nullable public Truth truth(Op op, NAR nar) {
            return truth(op, nar.confMin.floatValue());
        }

        @Nullable public Truth truth(@NotNull Op op, float confMin) {
            switch (op) {
                case CONJ:
                    return TruthFunctions.intersection(t, confMin);
                default:
                    throw new UnsupportedOperationException("aggregate truth for " + op + " not implemented or not applicable");
            }
        }
    }

    @NotNull
    @Override
    protected BeliefTable newBeliefTable(int eCap, int tCap) {
        return new DynamicBeliefTable(true, tCap);
    }
    @NotNull
    @Override
    protected BeliefTable newGoalTable(int eCap, int tCap) {
        return new DynamicBeliefTable(false, tCap);
    }

    private class DynamicBeliefTable extends DefaultBeliefTable {

        private final boolean beliefOrGoal;

        public DynamicBeliefTable(boolean beliefOrGoal, int tCap) {

            super(tCap);
            this.beliefOrGoal = beliefOrGoal;
        }


        @Nullable
        public Truth truth(long when, long now) {
            DynTruth d = truth(when, now, term(), false);
            return d != null ? d.truth(op(), nar) : super.truth(when, now);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, Compound template, boolean evidence) {
            return truth(when, ETERNAL, template, evidence);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, long now, Compound template, boolean evidence) {

            int n = size();
            final List<Truth> t = $.newArrayList(n);
            final List<Task> e = evidence ? $.newArrayList(n) : null;
            Budget b = evidence ? new RawBudget() : null;

            Term[] subs = template.terms();

            for (Term s : subs) {
                if (!(s instanceof Compound)) {
                    return null;
                }

                Term ss = s; //original subterm for dt relative calculation

                boolean negated = s.op()==NEG;
                if (negated)
                    s = $.unneg(s).term();

                Concept p = nar.concept(s);
                if (p == null)
                    return null;

                BeliefTable table = beliefOrGoal ? p.beliefs() : p.goals();
                if (table.isEmpty()) {
                    return null;
                }

                int dt = template.subtermTime(ss);


                @Nullable Truth nt = null;
                if (p instanceof DynamicCompoundConcept) {
                    @Nullable DynTruth ndt = ((DynamicBeliefTable)table).truth(when + dt, now, (Compound) s, false);
                    if (ndt!=null) {
                        nt = ndt.truth(s.op(), nar);
                    }
                } else {
                    nt = table.truth(when + dt, now);
                }

                if (nt==null) {
                    return null;
                }
                t.add($.negIf(nt,negated));

                if (evidence) {
                    @Nullable Task bt = table.top(when+dt, now);
                    if (bt != null) {
                        Budget btb = bt.budget();
                        if (!btb.isDeleted())
                            b = btb;
                        else
                            BudgetMerge.plusBlend.apply(b, btb, 1f);

                        e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                    }
                }
            }

            return new DynTruth(t, e, b);
        }


        @Override
        public Task match(@NotNull Task target, long now) {

            Task x = super.match(target, now);

            //experimental dynamic eval
            long occThresh = 1;
            if (x == null || Math.abs(now - x.occurrence() ) >= occThresh) {

                //template which may contain temporal relationship to emulate
                Compound template = x!=null ?  x.term() : term();

                DynTruth dt = truth(now, template, true);
                if (dt!=null) {
                    Truth y = dt.truth(op(), nar);
                    if (y!=null) {

                        RevisionTask xx = new RevisionTask(template, beliefOrGoal ? Symbols.BELIEF : Symbols.GOAL,
                                y, now, now, dt.evidence());
                        xx.budget(dt.b);
                        xx.log("Dynamic");

                        nar.inputLater(xx);

                        //System.err.println(xx + "\tvs\t" + x);

                        x = xx;
                    }

                }
            }

            return x;
        }
    }
}
