package nars.concept;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
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
import nars.term.obj.Termject;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.apache.commons.collections4.IteratorUtils;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

import static nars.Op.NEG;
import static nars.nal.Tense.DTERNAL;

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


        @Override
        @Nullable
        public Truth truth(long when, long now) {
            DynTruth d = truth(when, now, term(), false);
            return d != null ? d.truth(op(), nar) : super.truth(when, now);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, Compound template, boolean evidence) {
            return truth(when, when, template, evidence);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, long now, Compound template, boolean evidence) {

            int n = size();
            final List<Truth> t = $.newArrayList(n);
            final List<Task> e = evidence ? $.newArrayList(n) : null;
            Budget b = evidence ? new RawBudget() : null;

            DynTruth d = new DynTruth(t, e, b);
            Term[] subs = template.terms();
            for (Term s : subs) {
                if (!(s instanceof Compound))
                    continue; //unusual but could happen

                if (!subTruth((Compound) s, template, when, now, d))
                    return null;
            }

            return d;
        }

        /** returns true if the subterm was evaluated successfully, false otherwise */
        private boolean subTruth(Compound subterm, Compound superterm, long when, long now, DynTruth d) {

            Compound ss = subterm; //original pre-unnegated subterm for dt relative calculation

            boolean negated = subterm.op()==NEG;
            if (negated)
                subterm = (Compound) ($.unneg(subterm).term());

            Iterator<Compound> unrolled = unroll(subterm);
            while (unrolled.hasNext()) {
                subterm = unrolled.next();

                Concept p = nar.concept(subterm);
                if (p == null)
                    return false;

                BeliefTable table = beliefOrGoal ? p.beliefs() : p.goals();
                if (table.isEmpty()) {
                    return false;
                }

                int dt = superterm.subtermTime(ss);
                if (dt == DTERNAL) dt = 0;

                //System.out.println(ss + " "+ dt + " in " + template);


                @Nullable Truth nt = null;
                if (p instanceof DynamicCompoundConcept) {
                    @Nullable DynamicCompoundConcept.DynTruth ndt = ((DynamicBeliefTable)table).truth(when + dt, now, subterm, false);
                    if (ndt!=null) {
                        nt = ndt.truth(subterm.op(), nar);
                    }
                } else {
                    nt = table.truth(when + dt, now);
                }

                if (nt==null) {
                    return false;
                }
                d.t.add($.negIf(nt,negated));

                if (d.e!=null) {
                    @Nullable Task bt = table.top(when+dt, now);
                    if (bt != null) {
                        Budget btb = bt.budget();
                        if (!btb.isDeleted())
                            BudgetMerge.plusBlend.apply(d.b, btb, 1f);

                        d.e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                    }
                }

            }

            return true;
        }

        /** unroll IntInterval's */
        private Iterator<Compound> unroll(Compound c) {
            if (!c.hasAny(Op.INT))
                return Iterators.singletonIterator(c); //no IntInterval's early exit

            Map<ByteList, Termject.IntInterval> intervals = new HashMap();
            c.pathsTo(x -> x instanceof Termject.IntInterval ? ((Termject.IntInterval)x) : null, (ByteList p, Termject.IntInterval x) -> {
               intervals.put(p.toImmutable(), x);
               return true;
            });

            switch (intervals.size()) {

                case 1: //1D
                    Map.Entry<ByteList, Termject.IntInterval> e = intervals.entrySet().iterator().next();
                    Termject.IntInterval i1 = e.getValue();
                    int max = i1.max();
                    int min = i1.min();
                    List<Compound> t = $.newArrayList(1+max-min);
                    for (int i = min; i <= max; i++) {
                        t.add( (Compound) $.terms.transform(c, e.getKey(), $.the(i) ));
                    }
                    return t.iterator();

                case 2: //2D
                    return Iterators.singletonIterator(c);

                default:
                    //either there is none, or too many -- just use the term directly
                    return Iterators.singletonIterator(c);

            }

        }


        @Override
        public Task match(@NotNull Task target, long now) {

            Task x = super.match(target, now);

            long then = target.occurrence();

            long occThresh = 1;
            if (x == null || Math.abs(then - x.occurrence() ) >= occThresh) {

                //template which may contain temporal relationship to emulate
                Compound template = x!=null ?  x.term() : term();

                DynTruth dt = truth(then, template, true);
                if (dt!=null) {
                    Truth y = dt.truth(op(), nar);
                    if (y!=null && !y.equals(x.truth())) {

                        RevisionTask xx = new RevisionTask(template, beliefOrGoal ? Symbols.BELIEF : Symbols.GOAL,
                                y, nar.time(), then, dt.evidence());
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
