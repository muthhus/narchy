package nars.concept;

import com.google.common.collect.Iterators;
import nars.*;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.nal.Stamp;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.table.QuestionTable;
import nars.task.RevisionTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.obj.Termject;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.truth.Truthed;
import nars.util.Util;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.Param.TRUTH_EPSILON;
import static nars.time.Tense.DTERNAL;

/**
 * Adds support for dynamically calculated truth values
 */
public class DynamicConcept extends CompoundConcept {

    @NotNull
    public final NAR nar;

    public DynamicConcept(@NotNull Compound term, @NotNull Bag termLinks, @NotNull Bag taskLinks, @NotNull NAR nar) {
        super(term, termLinks, taskLinks, nar);
        this.nar = nar;
        this.beliefs = newBeliefTable(nar, true, 1,1 /* initial space */);
        this.goals = newBeliefTable(nar, false, 1,1 /* initial space */);
    }

    public static final class DynTruth implements Truthed {
        //@NotNull private final List<Truth> t;
        @Nullable final List<Task> e;
        private final float confMin;

        float freq, conf; //running product

        public DynTruth(Op o, float confMin, List<Task> e) {
            if (o!=CONJ)
                throw new UnsupportedOperationException("aggregate truth for " + o + " not implemented or not applicable");
            this.confMin = confMin;
            //this.t = t;
            this.e = e;
            freq = conf = 1f;
        }

        @NotNull public Budget budget() {
            //RawBudget b = new RawBudget();
            int s = e.size();
            assert(s > 0);
            float f = 1f / s;
//            for (Task x : e) {
//                BudgetMerge.plusBlend.apply(b, x.budget(), f);
//            }
//            return b;
            return BudgetFunctions.fund(e, f);
        }

        @Nullable public long[] evidence() {
            return e == null ? null : Stamp.zip(e);
        }

        @Override @Nullable public Truth truth() {
            return conf <= 0 ? null : $.t(freq, conf);
        }

        public boolean add(@Nullable Truth truth) {
            if (truth == null)
                return false;

            //specific to Truth.Intersection:
            conf *= truth.conf();
            if (conf < confMin)
                return false;
            freq *= truth.freq();
            return true;
        }

        @Override
        public String toString() {
            return truth().toString();
        }
    }

    @Override
    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal, int eCap, int tCap) {
        return new DynamicBeliefTable(beliefOrGoal, eCap, tCap);
    }
//    @NotNull
//    @Override
//    protected BeliefTable newGoalTable(int eCap, int tCap) {
//        return new DynamicBeliefTable(false, tCap);
//    }


    public static class DynamicBeliefTask extends RevisionTask {


        public DynamicBeliefTask(@NotNull Termed<Compound> term, char punc, Truth conclusion, long creationTime, long occTime, long[] evidence) {
            super(term, punc, conclusion, creationTime, occTime, evidence);
        }
    }

    public class DynamicBeliefTable extends DefaultBeliefTable {

        private final boolean beliefOrGoal;

        public DynamicBeliefTable(boolean beliefOrGoal, int eCap, int tCap) {

            super(newEternalTable(eCap), newTemporalTable(tCap, nar));
            this.beliefOrGoal = beliefOrGoal;
        }


//        @Override
//        public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
//            //only allow input and dynamic belief tasks to be inserted; otherwise process a new dynamic result
//            if (!input.isInput() && (!(input instanceof DynamicBeliefTask))) {
//                DynamicBeliefTask d = generate(input.term(), input.occurrence(), input.budget());
//                if (d!=null) {
//                    input.delete(); //necessary to cause NAR to replace the Task in the index, so as not to seem as a duplicate
//                    nar.inputLater(d);
//                    return null;
//                }
//            }
//
////            //only allow input tasks
////            if (!input.isInput())
////                return null;
//
//            return super.add(input, questions, concept, nar);
//        }

        @Nullable public DynamicBeliefTask generate(@NotNull Compound template, long when) {
            return generate(template, when, null);
        }

        @Nullable public DynamicBeliefTask generate(@NotNull Compound template, long when, @Nullable Budget b) {
            DynTruth yy = truth(when, template, true);
            if (yy == null)
                return null;

            DynamicBeliefTask t = new DynamicBeliefTask(template, beliefOrGoal ? Symbols.BELIEF : Symbols.GOAL,
                    yy.truth(), nar.time(), when, yy.evidence());
            t.setBudget(
                b!=null ? b : yy.budget()
            );
            t.log("Dynamic");
            return t;
        }

        @Override
        @Nullable
        public Truth truth(long when, long now) {
            DynTruth d = dyntruth(when, now, false);
            return Truth.maxConf(d != null ? d.truth() : null, super.truth(when, now) /* includes only non-dynamic beliefs */ );
        }

        @Nullable
        protected DynTruth dyntruth(long when, long now, boolean evidence) {
            return truth(when, now, DynamicConcept.this.term(), DynamicConcept.this, false, evidence);
        }

        @Nullable public DynamicConcept.DynTruth truth(long when, @NotNull Compound template, boolean evidence) {
            return truth(when, when, template,DynamicConcept.this /*nar.concept(template)*/,
                    template.op() == NEG, evidence);
        }


        @Nullable public DynamicConcept.DynTruth truth(long when, int dt, boolean evidence) {
            return truth(when, (Compound)$.terms.the(term(), dt), evidence);
        }

        @Nullable private DynamicConcept.DynTruth truth(long when, long now, Compound template, @Nullable Concept templateConcept, boolean negated, boolean evidence) {

            if (templateConcept == null)
                return null;

            //if (template instanceof Compound) {
            DynTruth d = newDyn(evidence);
            int s = template.size();
            for (int i = 0; i < s; i++) {
                if (!subTruth(template, template.term(i), when, now, negated, d))
                    return null;
            }
            return d;
//            } else {
//                @NotNull BeliefTable table = beliefOrGoal ? templateConcept.beliefs() : templateConcept.goals();
//                if (table instanceof DynamicBeliefTable) {
//                    return ((DynamicBeliefTable)table).dyntruth(when, now, evidence);
//                } else {
//                    Task x = table.match(when, now);
//                    if (x == null)
//                        return null;
//                    else {
//                        DynTruth d = newDyn(evidence);
//                        if (d.add(x.truth().negated(negated))) {
//                            if (d.e != null)
//                                d.e.add(x);
//                        }
//                        return d;
//                    }
//                }
//            }

        }

        @NotNull
        private DynTruth newDyn(boolean evidence) {
            int n = size();
            final List<Task> e = evidence ? $.newArrayList(n) : null;
            return new DynTruth(op(), nar.confMin.floatValue(), e);
        }

        /** returns true if the subterm was evaluated successfully, false otherwise */
        private boolean subTruth(Compound superterm, @NotNull Term subterm, long when, long now, boolean neg, @NotNull DynTruth d) {

            Term ss = subterm; //original pre-unnegated subterm for dt relative calculation

            boolean negated = (subterm.op() == NEG) != neg;
            if (negated)
                subterm = subterm.unneg();

            if (subterm instanceof Compound) {
                Compound cs = (Compound) subterm;
                if (subterm.hasAny(Op.INT)) {

                    Iterator<Term> unrolled = unroll(cs);
                    if (unrolled!=null) {
                        while (unrolled.hasNext()) {
                            Term next = unrolled.next();
                            if (!(next instanceof Compound) || !subTruth((Compound)next, superterm, when, now, d, ss, negated))
                                return false;
                        }

                        return true;
                    }

                } else {
                    return subTruth(cs, superterm, when, now, d, ss, negated);
                }
            }

            return false;
        }

        private boolean subTruth(@NotNull Compound next, Compound template, long when, long now, @NotNull DynTruth d, @NotNull Term ss, boolean negated) {
            Concept subConcept = nar.concept(next);
            if (subConcept == null)
                return false;

            BeliefTable table = beliefOrGoal ? subConcept.beliefs() : subConcept.goals();
            boolean tableDynamic = table instanceof DynamicBeliefTable;
            if (!tableDynamic && table.isEmpty()) {
                return false;
            }

            int dt = template!=null ? template.subtermTime(ss) : 0;
            if (dt == DTERNAL) dt = 0;

            //System.out.println(ss + " "+ dt + " in " + template);


            @Nullable Truth nt = null;
            if (tableDynamic) {
                boolean evi = d.e!=null;
                @Nullable DynamicConcept.DynTruth ndt = ((DynamicBeliefTable)table).truth(when + dt, now, next, subConcept, negated, evi);
                //already negated via the parameter
                if (ndt!=null && d.add(ndt.truth())) {
                    if (d.e!=null) {
                        d.e.addAll(ndt.e);
                    }
                    return true;
                }
            } else {
                nt = table.truth(when + dt, now);
                if (nt!=null && d.add(nt.negated(negated))) {
                    if (d.e!=null) {
                        Task bt = table.match(when+dt, now);
                        if (bt != null) {
                            d.e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                        }
                    }
                    return true;
                }
            }

            return false;
        }

        /** unroll IntInterval's */
        private Iterator<Term> unroll(@NotNull Compound c) {
            if (!c.hasAny(Op.INT))
                return Iterators.singletonIterator(c); //no IntInterval's early exit

            Compound cc = c;

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
                    List<Term> t = $.newArrayList(1 + max - min);
                    for (int i = min; i <= max; i++) {
                        @Nullable Term c1 = $.terms.transform(cc, e.getKey(), $.the(i));
                        if (c1!=null)
                            t.add(c1);
                    }
                    return t.iterator();
                }

                case 2: //2D
                    Iterator<Map.Entry<ByteList, Termject.IntInterval>> ee = intervals.entrySet().iterator();
                    Map.Entry<ByteList, Termject.IntInterval> e1 = ee.next();
                    Map.Entry<ByteList, Termject.IntInterval> e2 = ee.next();
                    Termject.IntInterval i1 = e1.getValue();
                    Termject.IntInterval i2 = e2.getValue();
                    int max1 = i1.max(), min1 = i1.min(), max2 = i2.max(), min2 = i2.min();
                    List<Term> t = $.newArrayList( (1 + max2 - min2) * (1 + max1 - min1) ) ;

                    for (int i = min1; i <= max1; i++) {
                        for (int j = min2; j <= max2; j++) {
                            Term c1 = $.terms.transform(cc, e1.getKey(), $.the(i));
                            if (!(c1 instanceof Compound))
                                //throw new RuntimeException("how not transformed to compound");
                                continue;
                            Term c2 = $.terms.transform((Compound)c1, e2.getKey(), $.the(j));
                            if (!(c2 instanceof Compound))
                                //throw new RuntimeException("how not transformed to compound");
                                continue;
                            t.add(c2);
                        }
                    }
                    return t.iterator();

                default:
                    //either there is none, or too many -- just use the term directly
                    return null;

            }

        }

        @Override
        public @Nullable Task match(long when, long now, @Nullable Task target) {
            Compound template = target!=null ? target.term() : term();

            Task y = generate(template, when);

            Task x = super.match(when, now, target);

            if (x == null) return y;
            if (y == null) return x;

            //choose the non-overlapping one
            if (target!=null) {
                if (Stamp.overlapping(x, target))
                    return y;
                if (Stamp.overlapping(y, target))
                    return x;
            }

            //choose higher confidence
            float xc = x.conf();
            float yc = y.conf();
            if (!Util.equals(xc, yc, TRUTH_EPSILON)) {
                return xc > yc ? x : y;
            }

            //choose based on originality (includes cyclic), but by default prefer the existing task not the dynamic one
            return (x.originality() > y.originality()) ? x : y;
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
