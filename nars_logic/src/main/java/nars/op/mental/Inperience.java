package nars.op.mental;

import com.google.common.util.concurrent.AtomicDouble;
import nars.*;
import nars.budget.Budget;
import nars.nal.nal8.Operator;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.compound.Compound;
import nars.truth.Truth;

import java.util.Arrays;
import java.util.Random;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 *
 * https://www.youtube.com/watch?v=ia4wMU-vfrw
 */
public class Inperience {


    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE = 0.5f; //0.92
    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE = 0.92f;

    //internal experience has less durability?
    public static final float INTERNAL_EXPERIENCE_PROBABILITY = 0.01f;

    //less probable form
    public static final float INTERNAL_EXPERIENCE_RARE_PROBABILITY =
            INTERNAL_EXPERIENCE_PROBABILITY / 4.0f;


    //internal experience has less durability?
    public static float INTERNAL_EXPERIENCE_DURABILITY_MUL = 0.1f; //0.1
    //internal experience has less priority?
    public static float INTERNAL_EXPERIENCE_PRIORITY_MUL = 0.1f; //0.1

    @Deprecated
    public static boolean enabled = true;
    private final NAR nar;


    /** minimum expectation necessary to create a concept
     *  original value: 0.66
     * */
    public AtomicDouble conceptCreationExpectation = new AtomicDouble(0.66);


//    public boolean isEnableWantBelieve() {
//        return enableWantBelieve;
//    }
//    public void setEnableWantBelieve(boolean val) {
//        enableWantBelieve =val;
//    }

    public static double getMinCreationBudgetSummary() {
        return MINIMUM_BUDGET_SUMMARY_TO_CREATE;
    }

    public static void setMinCreationBudgetSummary(double val) {
        MINIMUM_BUDGET_SUMMARY_TO_CREATE = (float) val;
    }

    public static double getMinCreationBudgetSummaryWonderEvaluate() {
        return MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE;
    }

    public static void setMinCreationBudgetSummaryWonderEvaluate(double val) {
        MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE = (float) val;
    }


    public static final Operator believe = Operator.the("believe");
    public static final Operator want = Operator.the("want");
    public static final Operator wonder = Operator.the("wonder");
    public static final Operator evaluate = Operator.the("evaluate");
    public static final Operator anticipate = Operator.the("anticipate");

    static final Operator[] nonInnateBeliefOperators = {
            Operator.the("remind"),
            Operator.the("doubt"),
            Operator.the("consider"),
            evaluate,
            Operator.the("hestitate"),
            wonder,
            believe,
            want
    };


    /**
     * whether it is full internal experience, or minimal (false)
     */
    public boolean isFull() {
        return false;
    }


    public static boolean random(Random r, float prob, int volume) {
        return r.nextFloat()*volume <= prob;
    }

    public Inperience(NAR n) {

        this.nar = n;

        n.memory.eventTaskProcess.on(tp -> experienceFromTaskInternal(tp.getTask()));

        n.memory.eventConceptProcess.on(p -> {
            Task belief = p.getBelief();
            if (belief == null) return;
            Task task = p.getTask();

            Random r = p.memory().random;

            int vol = Math.max(task.term().volume(), belief.term().volume());
            if (random(r, INTERNAL_EXPERIENCE_RARE_PROBABILITY, vol)) {
                nonInnate(task, belief, p, randomNonInnate(r) );
            }

            if (belief.term().op().isImplication() &&
                    random(r, INTERNAL_EXPERIENCE_PROBABILITY, vol) ) {
                internalizeImplication(task, belief, p);
            }
        });
    }


    public static Compound toTerm(Task s, Term self, float conceptCreationExpectation) {
        Operator opTerm;
        switch (s.getPunctuation()) {
            case Symbols.JUDGMENT:
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
                return null;
        }

        Truth tr = s.getTruth();
        Term[] arg = new Term[1 + (tr == null ? 1 : 2)];
        arg[0] = s.term();
        int k = 1;

        if (tr != null) {
            arg[k++] = tr.toWordTerm(conceptCreationExpectation);
        }
        arg[k] = self;

        Compound operation = $.oper(opTerm, arg);
        if (operation == null) {
            throw new RuntimeException("Unable to create Inheritance: " + opTerm + ", " + Arrays.toString(arg));
        }
        return operation;
    }



    public static Operator randomNonInnate(Random r) {
        return nonInnateBeliefOperators[r.nextInt(nonInnateBeliefOperators.length)];
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

    protected void experienceFromTaskInternal(final Task task) {

        final Term self = nar.memory.self();

        // if(OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY ||
        //         (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY && (task.sentence.punctuation==Symbols.QUESTION || task.sentence.punctuation==Symbols.QUEST))) {
        //char punc = task.getPunctuation();
        Budget b = task.getBudget();
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

        Compound ret = toTerm(task, self, conceptCreationExpectation.floatValue());
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
        nar.input(new MutableTask(ret).judgment()
                        .parent(task).time(now, now)
                        //.truth(1.0f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
                        //.budget(pri, dur)
                        .because("Innerperience"));
    }



    private static void internalizeImplication(Task task, Task belief, Premise nal) {
        Compound taskTerm = task.term();
        Compound beliefTerm = belief.term();

        if (beliefTerm.hasT() && beliefTerm.t() > 0) {
            //1. check if its (&/,term,+i1,...,+in) =/> anticipateTerm form:
            boolean valid = true;
            Term impsub = beliefTerm.term(0);
            if (impsub.op() == Op.CONJUNCTION) {
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
                //long interval = (impsub instanceof Interval ? ((Interval)impsub).duration() : 0);
                int interval = 0;

                beliefReasonDerive(task, belief,
                        $.oper(anticipate, beliefTerm.term(1)),
                        nal, interval);
            }
        }
    }

    private static void nonInnate(Task task, Task belief, Premise nal, Operator op) {
        //the operators which dont have a innate belief
        //also get a chance to reveal its effects to the system this way

            beliefReasonDerive(task, belief,
                    $.oper(op, belief.term()),
                    nal, 0);
    }

    protected static void beliefReasonDerive(Task parent, Task belief, Compound new_term, Premise p, long delay) {

        //TODO should this be a mew stamp or attached to parent.. originally it was a fresh new stamp from memory

        long now = p.time();

        p.nar().input(new MutableTask(new_term).goal()
                        /*.truth(1, Global.DEFAULT_JUDGMENT_CONFIDENCE)
                        .budget(Global.DEFAULT_GOAL_PRIORITY * INTERNAL_EXPERIENCE_PRIORITY_MUL,
                                Global.DEFAULT_GOAL_DURABILITY * INTERNAL_EXPERIENCE_DURABILITY_MUL)*/
                        .parent(parent, belief)
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
