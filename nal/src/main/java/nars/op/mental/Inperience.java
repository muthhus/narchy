package nars.op.mental;

import com.google.common.util.concurrent.AtomicDouble;
import nars.*;
import nars.budget.Budget;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Operator;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 *
 * https://www.youtube.com/watch?v=ia4wMU-vfrw
 */
public class Inperience {


    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE = 0.75f; //0.92
    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE = 0.92f;

    //internal experience has less durability?
    public static final float INTERNAL_EXPERIENCE_PROBABILITY = 0.01f;

    //less probable form
    public static final float INTERNAL_EXPERIENCE_RARE_PROBABILITY =
            INTERNAL_EXPERIENCE_PROBABILITY / 2.0f;


//    //internal experience has less durability?
//    public static float INTERNAL_EXPERIENCE_DURABILITY_MUL = 0.1f; //0.1
//    //internal experience has less priority?
//    public static float INTERNAL_EXPERIENCE_PRIORITY_MUL = 0.1f; //0.1

    @NotNull
    private final NAR nar;


    /** minimum expectation necessary to create a concept
     *  original value: 0.66
     * */
    @NotNull
    public final AtomicDouble conceptCreationExpectation = new AtomicDouble(0.66);


//    public boolean isEnableWantBelieve() {
//        return enableWantBelieve;
//    }
//    public void setEnableWantBelieve(boolean val) {
//        enableWantBelieve =val;
//    }

//    public static double getMinCreationBudgetSummary() {
//        return MINIMUM_BUDGET_SUMMARY_TO_CREATE;
//    }
//
//    public static void setMinCreationBudgetSummary(double val) {
//        MINIMUM_BUDGET_SUMMARY_TO_CREATE = (float) val;
//    }
//
//    public static double getMinCreationBudgetSummaryWonderEvaluate() {
//        return MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE;
//    }
//
//    public static void setMinCreationBudgetSummaryWonderEvaluate(double val) {
//        MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE = (float) val;
//    }


    public static final Operator believe = $.oper("believe");
    public static final Operator want = $.oper("want");
    public static final Operator wonder = $.oper("wonder");
    public static final Operator evaluate = $.oper("evaluate");
    public static final Operator anticipate = $.oper("anticipate");

    static final Atomic[] NON_INNATE_BELIEF_ATOMICs = {
            $.oper("remind"),
            $.oper("doubt"),
            $.oper("consider"),
            evaluate,
            $.oper("hestitate"),
            wonder,
            believe,
            want
    };


    public static boolean random(@NotNull Random r, float prob, int volume) {
        return r.nextFloat()*volume <= prob;
    }

    public Inperience(@NotNull NAR n) {

        this.nar = n;

        n.eventTaskProcess.on(this::experienceFromTaskInternal);

//        n.eventConceptProcess.on(p -> {
//            Task belief = p.belief();
//            if (belief == null) return;
//            Task task = p.task();
//
//            Random r = p.nar().random;
//
//            int vol = Math.max(task.term().volume(), belief.term().volume());
//            if (random(r, INTERNAL_EXPERIENCE_RARE_PROBABILITY, vol)) {
//                nonInnate(task, belief, randomNonInnate(r) );
//            }
//
//            if (belief.op().isImplication() &&
//                    random(r, INTERNAL_EXPERIENCE_PROBABILITY, vol) ) {
//                internalizeImplication(task, belief, p);
//            }
//        });
    }


    @Nullable
    public static Operator reify(char punc) {
        Operator opTerm;
        switch (punc) {
            case Symbols.BELIEF:
                opTerm = believe;
                break;
            case Symbols.GOAL:
                opTerm = want;
                break;
            case Symbols.QUESTION:
                opTerm = wonder;
                break;
            case Symbols.QUEST:
                opTerm = evaluate;
                break;
            default:
                opTerm = null;
        }
        return opTerm;
    }

    @Nullable
    public static Compound reify(@NotNull Task s, Term self, float conceptCreationExpectation) {

        Truth tr = s.truth();
        Term[] arg = new Term[1 + (tr == null ? 1 : 2)];
        Compound<?> tt = s.term();

        boolean negated = tt.op() == Op.NEG;
        int k = 0;

        arg[k++] = !negated ? tt : $.neg(tt) /* unwrap negation */;

        if (tr != null) {
            arg[k++] = tr.toWordTerm(conceptCreationExpectation, negated);
        }

        arg[k] = self;

        Compound operation = $.exec(reify(s.punc()), arg);
//        if (operation == null) {
//            throw new RuntimeException("Unable to create Inheritance: " + opTerm + ", " + Arrays.toString(arg));
//        }
        return operation;
    }



    public static Atomic randomNonInnate(@NotNull Random r) {
        return NON_INNATE_BELIEF_ATOMICs[r.nextInt(NON_INNATE_BELIEF_ATOMICs.length)];
    }


//    public Task experienceFromBelief(Premise nal, Budget b, Sentence belief) {
//        return experienceFromTask(nal,
//                new Task(belief.clone(), b, null),
//                false);
//    }

//    public Task experienceFromTask(Premise nal, Task task, boolean full) {
//        if (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY) {
//            return experienceFromTaskInternal(nal, task, full);
//        }
//        return null;
//    }

    protected void experienceFromTaskInternal(@NotNull final Task task) {

        final Term self = nar.self;

        if (!random(nar.random, INTERNAL_EXPERIENCE_PROBABILITY, task.term().volume()))
            return;

        // if(OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY ||
        //         (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY && (task.sentence.punctuation==Symbols.QUESTION || task.sentence.punctuation==Symbols.QUEST))) {
        //char punc = task.getPunctuation();
        Budget b = task.budget();
        if (task.isQuestOrQuestion()) {
            if (b.summaryLessThan(MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE)) {
                return;
            }
        } else if (b.summaryLessThan(MINIMUM_BUDGET_SUMMARY_TO_CREATE)) {
            return;
        }

        Term content = task.term();
        // to prevent infinite recursions
        if (Op.isOperation(content)/* ||  Memory.randomNumber.nextFloat()>Global.INTERNAL_EXPERIENCE_PROBABILITY*/) {
            return;
        }

        Compound ret = reify(task, self, conceptCreationExpectation.floatValue());
        if (ret == null) {
            return;
        }



//        float pri = Global.DEFAULT_JUDGMENT_PRIORITY * INTERNAL_EXPERIENCE_PRIORITY_MUL;
//        float dur = Global.DEFAULT_JUDGMENT_DURABILITY * INTERNAL_EXPERIENCE_DURABILITY_MUL;
//        if (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY) {
//            pri *= INTERNAL_EXPERIENCE_PRIORITY_MUL;
//            dur *= INTERNAL_EXPERIENCE_DURABILITY_MUL;
//        }

        long now = nar.time();

        nar.input(new MutableTask(ret, Symbols.BELIEF, 1f, nar)
                        //.parent(task)
                        .time(now, now)
                        //.truth(1.0f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
                        //.budget(pri, dur)
                        .because("Inperience"));
    }



    private void internalizeImplication(@NotNull Task task, @NotNull Task belief, @NotNull Premise nal) {
        Compound taskTerm = task.term();
        Compound beliefTerm = belief.term();

        if (beliefTerm.temporal() && beliefTerm.dt() > 0) {
            //1. check if its (&/,term,+i1,...,+in) =/> anticipateTerm form:
            boolean valid = true;
            Term impsub = beliefTerm.term(0);
            if (impsub.op() == Op.CONJ) {
                Compound conj = (Compound) impsub;
                if (!conj.term(0).equals(taskTerm)) {
                    valid = false; //the expected needed term is not included
                }
            } else {
                if (!impsub.equals(taskTerm)) {
                    valid = false;
                }
            }

            //TODO use interval?


            if (valid) {
                Compound c = $.exec(anticipate, beliefTerm.term(1));
                //if (c!=null) {
                    //long interval = (impsub instanceof Interval ? ((Interval)impsub).duration() : 0);
                    //int interval = 0;

                    beliefReasonDerive(task, belief,
                            c,
                            0);
                //}
            }
        }
    }

    void nonInnate(@NotNull Task task, @NotNull Task belief, @NotNull Operator op) {
        //the operators which dont have a innate belief
        //also get a chance to reveal its effects to the system this way

        Compound c = $.exec(op, $.p( belief.term() ) );
        if (c!=null)
            beliefReasonDerive(task, belief, c, 0);
    }

    void beliefReasonDerive(@NotNull Task parent, Task belief, @NotNull Compound new_term, long delay) {

        //TODO should this be a mew stamp or attached to parent.. originally it was a fresh new stamp from memory

        long now = nar.time();

        nar.input(new MutableTask(new_term, Symbols.GOAL, 1f, nar)
                        /*.budget(Global.DEFAULT_GOAL_PRIORITY * INTERNAL_EXPERIENCE_PRIORITY_MUL,
                                Global.DEFAULT_GOAL_DURABILITY * INTERNAL_EXPERIENCE_DURABILITY_MUL)*/
                        //.parent(parent, belief)
                        .time(now, now + delay)
                        .because("Inner Belief")
        );
    }


//
//    //TODO
//    public static void experienceFromBelief(Memory memory, Task task, Task belief) {
//        //Task T=new Task(belief.clone(),new Budget(task),null);
//        ///InternalExperienceFromTask(memory,T,false);
//    }

//    public static void InternalExperienceFromTask(Memory memory, Task task, boolean full) {
//        if(!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY) {
//            InternalExperienceFromTaskInternal(memory,task,full);
//        }
//    }
//
//    public static boolean InternalExperienceFromTaskInternal(Memory memory, Task task, boolean full) {
//        if(!enabled) {
//            return false;
//        }
//
//        // if(OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY ||
//        //         (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY && (task.sentence.punctuation==Symbols.QUESTION || task.sentence.punctuation==Symbols.QUEST))) {
//        {
//            if(task.sentence.punctuation == Symbols.QUESTION || task.sentence.punctuation == Symbols.QUEST) {
//                if(task.budget.summary()<MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE) {
//                    return false;
//                }
//            }
//            else
//            if(task.budget.summary()<MINIMUM_BUDGET_SUMMARY_TO_CREATE) {
//                return false;
//            }
//        }
//
//        Term content=task.getTerm();
//        // to prevent infinite recursions
//        if (content instanceof Operation/* ||  Memory.randomNumber.nextDouble()>Global.INTERNAL_EXPERIENCE_PROBABILITY*/) {
//            return true;
//        }
//        Sentence sentence = task.sentence;
//        TruthValue truth = new DefaultTruth(1.0f, Global.DEFAULT_JUDGMENT_CONFIDENCE);
//        Stamp stamp = task.sentence.stamp.clone();
//        stamp.setOccurrenceTime(memory.time());
//        Term ret=toTerm(sentence, memory);
//        if (ret==null) {
//            return true;
//        }
//        Sentence j = new Sentence(ret, Symbols.JUDGMENT, truth, stamp);
//        Budget newbudget=new Budget(
//                Global.DEFAULT_JUDGMENT_CONFIDENCE*INTERNAL_EXPERIENCE_PRIORITY_MUL,
//                Global.DEFAULT_JUDGMENT_PRIORITY*INTERNAL_EXPERIENCE_DURABILITY_MUL,
//                BudgetFunctions.truthToQuality(truth));
//
//        if(!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY) {
//            newbudget.setPriority(task.getPriority()*INTERNAL_EXPERIENCE_PRIORITY_MUL);
//            newbudget.setDurability(task.getDurability()*INTERNAL_EXPERIENCE_DURABILITY_MUL);
//        }
//
//        Task newTask = new Task(j, (Budget) newbudget,
//                full ? null : task);
//        memory.addNewTask(newTask, "Remembered Action (Internal Experience)");
//        return false;
//    }
}
