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

        //ASSUMES belief, not goal table
        return new DefaultBeliefTable(tCap) {

            @Override
            public @Nullable Truth truth(long when) {
                DynTruth d = truth(when, false);
                if (d == null)
                    return super.truth(when);
                return d.truth(op(),nar);
            }

            @Nullable private DynTruth truth(long when, boolean evidence) {

                int n = size();
                final List<Truth> t = $.newArrayList(n);
                final List<Task> e = evidence ? $.newArrayList(n) : null;
                Budget b = evidence ? new RawBudget() : null;

                Term[] subs = term().terms();

                for (Term s : subs) {
                    if (!(s instanceof Compound) || s.hasTemporal()) {
                        return null;
                    }

                    boolean negated = s.op()==NEG;
                    if (negated)
                        s = $.unneg(s).term();

                    Concept p = nar.concept(s);
                    if (p == null || !p.hasBeliefs()) {
                        return null;
                    }

                    @Nullable Truth nt = p.belief(when);
                    if (nt==null) {
                        return null;
                    }
                    t.add($.negIf(nt,negated));

                    if (evidence) {
                        @Nullable Task bt = p.beliefs().top(when);
                        if (bt != null) {
                            Budget btb = bt.budget();
                            if (b == null && !btb.isDeleted())
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


                    DynTruth dt = truth(now, true);
                    if (dt!=null) {
                        Truth y = dt.truth(op(), nar);

                        ///@NotNull Termed<Compound> newTerm = term.dt() != 0 ? $.parallel(term.terms()) : term;

                        RevisionTask xx = new RevisionTask(term(), Symbols.BELIEF, y, now, now, dt.evidence());
                        xx.budget(dt.b);
                        xx.log("Dynamic");

                        nar.inputLater(xx);

                        //System.err.println(xx + "\tvs\t" + x);

                        x = xx;

                    }
                }

                return null;
            }
        };
    }
}
