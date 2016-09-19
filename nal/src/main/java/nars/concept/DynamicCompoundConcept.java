package nars.concept;

import com.google.common.collect.Iterators;
import nars.*;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.nal.Stamp;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.RevisionTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.truth.Truth;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

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
        @NotNull private final List<Truth> t;
        @Nullable final List<Task> e;
        @Nullable final Budget b;
        private final float confMin;

        @Deprecated float freq, conf; //running product

        public DynTruth(Op o, float confMin, List<Truth> t, List<Task> e, Budget b) {
            if (o!=CONJ)
                throw new UnsupportedOperationException("aggregate truth for " + o + " not implemented or not applicable");
            this.confMin = confMin;
            this.t = t;
            this.e = e;
            this.b = b;
            freq = conf = 1f;
        }

        @Nullable public long[] evidence() {
            return e == null ? null : Stamp.zip((Collection) e);
        }

        @Nullable public Truth truth() {
            if (t.isEmpty())
                return null;
            return $.t(freq, conf);
        }

        public boolean add(@NotNull Truth truth) {
            //specific to Truth.Intersection:
            conf *= truth.conf();
            if (conf < confMin)
                return false;
            freq *= truth.freq();
            return true;
        }

//        @Nullable public Truth truth(@NotNull Op op, float confMin) {
//            switch (op) {
//                case CONJ:
//                    return TruthFunctions.intersection(t, confMin);
//                default:
//                    throw new UnsupportedOperationException("aggregate truth for " + op + " not implemented or not applicable");
//            }
//        }
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
            return d != null ? d.truth() : super.truth(when, now);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, Compound template, boolean evidence) {
            return truth(when, when, template, evidence);
        }

        @Nullable private DynamicCompoundConcept.DynTruth truth(long when, long now, Compound template, boolean evidence) {

            int n = size();
            final List<Truth> t = $.newArrayList(n);
            final List<Task> e = evidence ? $.newArrayList(n) : null;
            Budget b = evidence ? new RawBudget() : null;

            DynTruth d = new DynTruth(op(), nar.confMin.floatValue(), t, e, b);
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
                        nt = ndt.truth();
                    }
                } else {
                    nt = table.truth(when + dt, now);
                }

                if (nt==null) {
                    return false;
                }
                if (!d.add($.negIf(nt,negated)))
                    return false;

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
                {
                    Map.Entry<ByteList, Termject.IntInterval> e = intervals.entrySet().iterator().next();
                    Termject.IntInterval i1 = e.getValue();
                    int max = i1.max();
                    int min = i1.min();
                    List<Compound> t = $.newArrayList(1 + max - min);
                    for (int i = min; i <= max; i++) {
                        @Nullable Term c1 = $.terms.transform(c, e.getKey(), $.the(i));
                        if (!(c1 instanceof Compound))
                            continue;
                        t.add((Compound) c1);
                    }
                    return t.iterator();
                }

                case 2: //2D
                {
                    Iterator<Map.Entry<ByteList, Termject.IntInterval>> ee = intervals.entrySet().iterator();
                    Map.Entry<ByteList, Termject.IntInterval> e1 = ee.next();
                    Map.Entry<ByteList, Termject.IntInterval> e2 = ee.next();
                    Termject.IntInterval i1 = e1.getValue();
                    Termject.IntInterval i2 = e2.getValue();
                    int max1 = i1.max(), min1 = i1.min(), max2 = i2.max(), min2 = i2.min();
                    List<Compound> t = $.newArrayList( (1 + max2 - min2) * (1 + max1 - min1) ) ;

                    for (int i = min1; i <= max1; i++) {
                        for (int j = min2; j <= max2; j++) {
                            Term c1 = $.terms.transform(c, e1.getKey(), $.the(i));
                            if (!(c1 instanceof Compound))
                                //throw new RuntimeException("how not transformed to compound");
                                continue;
                            Term c2 = $.terms.transform((Compound)c1, e2.getKey(), $.the(j));
                            if (!(c2 instanceof Compound))
                                //throw new RuntimeException("how not transformed to compound");
                                continue;
                            t.add((Compound)c2);
                        }
                    }
                    return t.iterator();
                }

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
                    Truth y = dt.truth();
                    if (y!=null && !y.equals(x.truth())) {

                        RevisionTask xx = new RevisionTask(template, beliefOrGoal ? Symbols.BELIEF : Symbols.GOAL,
                                y, nar.time(), then, dt.evidence());
                        xx.setBudget(dt.b);
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

//package nars.util.signal;
//
//import com.google.common.collect.TreeMultimap;
//import nars.NAR;
//import DefaultBeliefTable;
//import EternalTable;
//import TemporalBeliefTable;
//import nars.nal.Tense;
//import nars.task.Task;
//import nars.truth.Truth;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
///**
// * customized belief table for sensor and motor concepts
// */
//public class SensorBeliefTable extends DefaultBeliefTable {
//
//    public SensorBeliefTable(int eCap, int tCap) {
//        super(eCap, tCap);
//    }
//
//    @Override
//    protected Task addTemporal(@NotNull Task goal, List<Task> displaced, @NotNull NAR nar) {
//
//        if (!goal.isInput() && nar.taskPast.test(goal)) {
//            //this is a goal for a past time, reject
//            return null;
//        }
//
//        if (!isEmpty()) {
//            TemporalBeliefTable temporals = temporal;
//            if (temporals.isFull()) {
//                //remove any past goals
//                temporals.removeIf(nar.taskPast);
//            }
//        }
//
//        return super.addTemporal(goal, displaced, nar);
//    }
//
////    @Override
////    protected TemporalBeliefTable newTemporalBeliefTable(Map<Task, Task> mp, int initialTemporalCapacity) {
////        return new SensorTemporalBeliefTable(mp, initialTemporalCapacity);
////    }
//
//    public static class SensorTemporalBeliefTable implements TemporalBeliefTable {
//
//        //TODO
//
//
//        private int capacity;
//        @NotNull
//        private final TreeMultimap<Long, Task> map;
//
//        public SensorTemporalBeliefTable(@Deprecated Map<Task, Task> outerMap, int capacity) {
//            map = TreeMultimap.create(Long::compare, (Task ta, Task tb) -> {
//                return Float.compare(ta.conf(), tb.conf());
//            });
//            this.capacity = capacity;
//
//        }
//
//        @Override
//        public void clear() {
//            map.clear();
//        }
//
//        @Nullable
//        @Override
//        public Task get(@NotNull Object key) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Nullable
//        @Override
//        public Object remove(@NotNull Task key) {
//            return map.remove(key.occurrence(), key);
//        }
//
//        @Nullable
//        @Override
//        public Task put(@NotNull Task task, @NotNull Task task2) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public int size() {
//            return map.size();
//        }
//
//        @Override
//        public void forEachKey(@NotNull Consumer<? super Task> each) {
//            map.values().forEach(each);
//        }
//
//        @Override
//        public int capacity() {
//            return capacity;
//        }
//
//        @Override
//        public void setCapacity(int i) {
//            this.capacity = i;
//        }
//
//        @Override
//        public void topWhile(@NotNull Predicate<? super Task> each, int n) {
//            //TODO choose a radius of n around the current nar.time()
//
//
//            for (Long aLong : map.keySet()) {
//                for (Task task : map.get(aLong)) {
//                    if (!each.test(task))
//                        break;
//                    if (n-- == 0)
//                        break;
//                }
//
//            }
//        }
//
//        public int compare(@NotNull Task o1, @NotNull Task o2) {
//            return Long.compare(o1.occurrence(), o2.occurrence());
//        }
//
//        @Override
//        public @Nullable Task strongest(long when, long now, Task against) {
//            //map.keySet().higher(when);
//            return null;
//        }
//
//        @Override
//        public @Nullable Truth truth(long when, long now, EternalTable eternal) {
//             return strongest(when, now, null).projectTruth(when, now, false);
//        }
//
//        @Override
//        public Task add(@NotNull Task input, EternalTable eternal, List<Task> displ, @NotNull NAR nar) {
//            return null;
//        }
//
//        @Override
//        public void removeIf(@NotNull Predicate<Task> o) {
//
//        }
//
//        @Override
//        public long minTime() {
//            //TODO
//            return Tense.ETERNAL;
//        }
//
//        @Override
//        public long maxTime() {
//            //TODO
//            return Tense.ETERNAL;
//        }
//
//        @Override
//        public void minTime(long minT) {
//
//        }
//
//        @Override
//        public void maxTime(long maxT) {
//
//        }
//
//        @Nullable
//        @Override
//        public Iterator<Task> iterator() {
//            return null;
//        }
//    }
//}
