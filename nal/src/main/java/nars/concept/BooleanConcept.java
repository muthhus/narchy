package nars.concept;

import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.table.BeliefTable;
import nars.concept.table.DynamicBeliefTable;
import nars.nal.Stamp;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Operator;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dynamically updates a truth value based on truth aggregation of the concepts referred by parameters
 */
public class BooleanConcept extends CompoundConcept {
    @NotNull
    public final NAR nar;

    public static final Operator AND_OP = $.oper("and");
    public static final Operator OR_OP = $.oper("or");
    @Nullable
    private final Compound params;


    public interface BooleanModel {
        @NotNull Operator op();

        /** set the truth and evidence on the task before returning */
        @Nullable MutableTask update(@NotNull NAR nar, long now, Termed<Compound> content, boolean beliefOrGoal, @NotNull Termed[] args);
    }

    static final BooleanModel AND = new DefaultBooleanModel(true);
    static final BooleanModel OR = new DefaultBooleanModel(false);

    static final class DefaultBooleanModel implements BooleanModel {

        private final boolean mode;

        public DefaultBooleanModel(boolean andOrOr) {
            this.mode = andOrOr;
        }

        @NotNull
        @Override public Operator op() {  return mode ? AND_OP : OR_OP; }

        @Override public MutableTask update(@NotNull NAR nar, long now, @NotNull Termed<Compound> content, boolean beliefOrGoal, @NotNull Termed[] args) {

            boolean mode = this.mode;

            float f = mode ? 1f : 0f;
            float c = mode ? 1f : 0f;


            LongArrayList ev = new LongArrayList(Param.STAMP_CAPACITY);
            int evidencePerArg = Math.max(Param.STAMP_CAPACITY / args.length, 1);

            for (Termed t : args) {

                Concept subtermConcept = nar.concept(t);
                if (subtermConcept != null) {
                    BeliefTable b = beliefOrGoal ? subtermConcept.beliefs() : subtermConcept.goals();
                    Task at = b.top(now);
                    if (at != null) {

                        //Truth ct = at.projectTruth(now, now, /* eternalize if weak */ false);
                        Truth ct = b.truth(now);

                        if (mode) {
                            f *= ct.freq();
                            c *= ct.conf();
                        } else {
                            float cf = ct.freq();
                            if (cf > f)
                                f = cf;
                            float cc = ct.conf();
                            if (cc > c)
                                c = cc;
                        }


                        long[] ae = at.evidence();
                        int aen = ae.length - 1;
                        for (int i = 0; i < Math.min(ae.length, evidencePerArg); i++) {
                            ev.add(ae[aen - i]); //most recent
                        }

                    }
                }
            }

            if (c > 0) {
                MutableTask task = new MutableTask(content, beliefOrGoal ? '.' : '!', new DefaultTruth(f, c));

                if (!ev.isEmpty()) {
                    return task.evidence(Stamp.toSetArray(ev));
                }

            }

            return null; //task.truth(Truth.Zero);


        }
    }

    protected BooleanModel model;

    @NotNull public static BooleanConcept And(@NotNull NAR nar, Term... args) {
        return new BooleanConcept(nar, AND, args);
    }
    @NotNull public static BooleanConcept Or(@NotNull NAR nar, Term... args) {
        return new BooleanConcept(nar, OR, args);
    }

    public BooleanConcept(@NotNull NAR nar, @NotNull BooleanModel model, @NotNull Term... args)  {
        super($.exec(model.op(),$.sete(args)), nar);


        if (args.length < 2)
            throw new RuntimeException("too few args");
        if (args.length > Param.STAMP_CAPACITY)
            throw new RuntimeException("too many args");

        this.params = (Compound) term().subterm(0,0);// (({...}) --> ^...)

        this.model = model;
        this.nar = nar;
    }

//    @Override
//    public boolean link(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
//        if (super.link(b, scale, minScale, nar, conceptOverflow)) {
//
//            //intercept activated tasklinks with compounds present in the subterms
//            if (b instanceof Task) {
//                Task t = (Task) b;
//
//                //TODO defer the update to one run at the end of the cycle like Sensor and Motor
//                char punc = t.punc();
//                if ((punc == Symbols.BELIEF || punc == Symbols.GOAL) && params.containsTerm(t.term()) /* recursively? */) {
//
//                    ((DynamicBeliefTable)tableFor(punc)).changed();
//
//                    //char punc = t.punc();
//                    //long now = nar.time();
//                    /*if (punc == '.')
//                         ((DynamicBeliefTable) beliefs()).updateTask(now);
//                    else if (punc == '!')
//                        ((DynamicBeliefTable) goals()).updateTask(now);*/
//                }
//            }
//            return true;
//        }
//        return false;
//    }

//    @NotNull
//    @Override
//    public BeliefTable beliefs() {
//        if (beliefs == null) {
//            beliefs = newBeliefTable(0);
//        }
//        executeLater(nar);
//        return beliefs;
//    }
//
//    @NotNull
//    @Override
//    public BeliefTable goals() {
//        if (goals == null) {
//            goals = newGoalTable(0);
//        }
//        executeLater(nar);
//        return goals;
//    }

//    @Override
//    public void run() {
//        super.run();
//
//        //TODO only update belief or goal if changed, not both
//        long now = nar.time();
//        ((DynamicBeliefTable) beliefs()).updateTask(now);
//        ((DynamicBeliefTable) goals()).updateTask(now);
//    }


    @NotNull
    @Override
    protected BeliefTable newBeliefTable() {
        return new BooleanConceptTable(true);
    }

    @NotNull
    @Override
    protected BeliefTable newGoalTable() {
        return new BooleanConceptTable(false);
    }

    private class BooleanConceptTable extends DynamicBeliefTable {
        private final boolean beliefOrGoal; //or goals

        public BooleanConceptTable(boolean beliefOrGoal) {
            super();
            this.beliefOrGoal = beliefOrGoal;
        }

        @NotNull
        @Override
        public NAR nar() {
            return nar;
        }

        @Nullable @Override
        protected Task update(long now) {


            MutableTask result = model.update(nar, now, BooleanConcept.this, beliefOrGoal, params.terms());

            return result != null ? result.present(now).normalize(nar) : null;
        }

//        @Override
//        public void remove(@NotNull Task belief, List<Task> displ) {
//            //nothing, maybe force update
//            if (belief == this.current) {
//                current = null;
//            }
//        }


        @Override
        public float confMax(float minFreq, float maxFreq) {
            return current!=null ? current.conf() : 0;
        }
    }
}
