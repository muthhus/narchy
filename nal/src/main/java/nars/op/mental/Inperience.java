package nars.op.mental;

import jcog.bag.impl.CurveBag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.control.CauseChannel;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.term.Terms.normalizedOrNull;
import static nars.term.atom.Atomic.the;
import static nars.time.Tense.ETERNAL;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 * <p>
 * https://www.youtube.com/watch?v=ia4wMU-vfrw
 */
public class Inperience extends TaskLeak<Task, PriReference<Task>> {

    public static final Logger logger = LoggerFactory.getLogger(Inperience.class);


//    //internal experience has less durability?
//    public static final float INTERNAL_EXPERIENCE_PROBABILITY = 0.01f;
//    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE = 0.75f; //0.92
//    public static float MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE = 0.92f;
//
//    //less probable form
//    public static final float INTERNAL_EXPERIENCE_RARE_PROBABILITY =
//            INTERNAL_EXPERIENCE_PROBABILITY / 2.0f;


//    //internal experience has less durability?
//    public static float INTERNAL_EXPERIENCE_DURABILITY_MUL = 0.1f; //0.1
//    //internal experience has less priority?
//    public static float INTERNAL_EXPERIENCE_PRIORITY_MUL = 0.1f; //0.1


    /**
     * minimum conf necessary to create a concept
     * original value: 0.66
     */
    @NotNull
    public final FloatParam confMin = new FloatParam(0.5f);

    /**
     * max frequency difference from either 0.0 or 1.0 to be polarized enough.
     * use the < 0.5 value here, ex: 0.1 means that 0..0.1 and 0.9..1.0 will be accepted
     */
    @NotNull
    public final FloatParam freqMax = new FloatParam(0.1f);
    final CauseChannel<Task> in;

//    float beliefFactor = 1f;
//    float questionFactor = 0.5f;

    /**
     * multiplier for he sensory task priority to determine inperienced task priority
     */
    private final float priFactor = 0.5f;

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


    public static final Atomic believe = the("believe");
    public static final Atomic want = the("want");
    public static final Atomic wonder = the("wonder");
    public static final Atomic evaluate = the("evaluate");
    public static final Atomic anticipate = the("anticipate");

    public static final ImmutableSet<Atomic> operators = Sets.immutable.of(
            believe, want, wonder, evaluate, anticipate);

    static final Atomic[] NON_INNATE_BELIEF_ATOMICs = {
            the("remind"),
            the("doubt"),
            the("consider"),
            evaluate,
            the("hestitate"),
            wonder,
            believe,
            want
    };
    @NotNull
    private final NAR nar;

//

    public Inperience(@NotNull NAR n, float rate, int capacity) {
        super(
            new CurveBag(PriMerge.max, new ConcurrentHashMap<>(capacity), n.random(), capacity), rate, n
        );
        this.nar = n;

        in = nar.newInputChannel(this);

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

    @Override
    protected void in(Task task, @NotNull Consumer<PriReference<Task>> each) {


        if (task.isCommand() || task.isInput()
            /*|| task instanceof InperienceTask*/) //for disabling recursive inperience
            return;

        boolean full = bag.isFull();


        if (task.isBeliefOrGoal()) {
            //check for sufficient truth polarization
            if (full && task.conf() <= confMin.floatValue())
                return; //too low confidence

            float f = task.freq();
            float fm = freqMax.floatValue();
            if (!(f <= fm) && !(f >= (1f - fm)))
                return;

            //belief = true;
        } else {
            //belief = false;
        }

        float p = task.priSafe(-1);
        if (p >= 0)
            each.accept(new PLink<>(task, p));

        // if(OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY ||
        //         (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY && (task.sentence.punctuation==Symbols.QUESTION || task.sentence.punctuation==Symbols.QUEST))) {
        //byte punc = task.getPunctuation();
//        Budget b = task.budget();
//        if (task.isQuestOrQuestion()) {
//            if (b.summaryLessThan(MINIMUM_BUDGET_SUMMARY_TO_CREATE_WONDER_EVALUATE)) {
//                return;
//            }
//        } else if (b.summaryLessThan(MINIMUM_BUDGET_SUMMARY_TO_CREATE)) {
//            return;
//        }

//        Term content = task.term();
//        // to prevent infinite recursions
//        if (Op.isOperation(content)/* ||  Memory.randomNumber.nextFloat()>Global.INTERNAL_EXPERIENCE_PROBABILITY*/) {
//            return;
//        }


//        float pri = Global.DEFAULT_JUDGMENT_PRIORITY * INTERNAL_EXPERIENCE_PRIORITY_MUL;
//        int dur = Global.DEFAULT_JUDGMENT_DURABILITY * INTERNAL_EXPERIENCE_DURABILITY_MUL;
//        if (!OLD_BELIEVE_WANT_EVALUATE_WONDER_STRATEGY) {
//            pri *= INTERNAL_EXPERIENCE_PRIORITY_MUL;
//            dur *= INTERNAL_EXPERIENCE_DURABILITY_MUL;
//        }


    }

    @Override
    protected float onOut(@NotNull PriReference<Task> b) {

        Task task = b.get();

        //try {
        //        if (r == null)
//            return null;
//
//        //unnegate and check for an apparent atomic term which may need decompressed in order to be the task's content
//        boolean negated;
//        Term s = r;
//        if (r.op() == NEG) {
//            s = r.unneg();
//            if (s instanceof Variable)
//                return null; //throw new InvalidTaskException(r, "unwrapped variable"); //should have been prevented earlier
//
//            negated = true;
//            if (s instanceof Compound) {
//                return (Compound) r; //its normal compound inside the negation, handle it in Task constructor
//            }
//        } else if (r instanceof Compound) {
//            return (Compound) r; //do not uncompress any further
//        } else if (r instanceof Variable) {
//            return null;
//        } else {
//            negated = false;
//        }
//
//        if (!(s instanceof Compound)) {
//            Compound t = compoundOrNull(nar.post(s));
//            if (t == null)
//                return null; //throw new InvalidTaskException(r, "undecompressible");
//            else
//                return (Compound) $.negIf(t, negated); //done
////            else
////            else if (s.op()==NEG)
////                return (Compound) $.negIf(post(s.unneg(), nar));
////            else
////                return (Compound) $.negIf(s, negated);
//        }
//        //its a normal negated compound, which will be unnegated in task constructor
//        return (Compound) s;
        Term r = normalizedOrNull(compoundOrNull(reify(task, nar.self())), nar.terms);
        if (r != null) {

            long now = nar.time();

            long start = task.start();
            long end;
            if (start == ETERNAL)
                end = start = now;
            else {
                end = task.end();
            }

            in.input( (Task)
                new NALTask(r, BELIEF,
                    new DiscreteTruth(1, nar.confDefault(Op.BELIEF)),
                    now, start, end, task.stamp()
                )
                    .log("Inperience")
                    .pri(task.priElseZero() * priFactor)
            );

            return 1;
        }
//        } catch (ClassCastException ignored) {
//            //happens rarely, due to circularity while trying to create something like: want((x<->want),...
//
//            //System.err.println(task);
//        }

        return 0;
    }


//    private boolean isExperienceTerm(@NotNull Compound term) {
//        return term.op() == INH && operators.contains(term.subterm(1) /* predicate of the inheritance */);
//    }


    @NotNull
    public static Atomic reify(byte punc) {
        Atomic opTerm;
        switch (punc) {
            case BELIEF:
                opTerm = believe;
                break;
            case GOAL:
                opTerm = want;
                break;
            case QUESTION:
                opTerm = wonder;
                break;
            case QUEST:
                opTerm = evaluate;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return opTerm;
    }

    @Nullable
    public Compound reify(@NotNull Task s, Term self) {

        Truth tr = s.truth();
        Term[] arg = new Term[1 + 1];

        int k = 0;

        arg[k++] = self;
        arg[k/*++*/] = nar.terms.queryToDepVar($.negIf(s.term(), tr != null && tr.isNegative())); //unwrapping negation here isnt necessary sice the term of a task will be non-negated


        try {
            Compound ff = $.func(reify(s.punc()), arg);
            return Terms.compoundOrNull($.negIf(ff, false));

        } catch (Exception e) {
            logger.error(" {}", e);

        }

        return null;
    }


    public static Atomic randomNonInnate(@NotNull Random r) {
        return NON_INNATE_BELIEF_ATOMICs[r.nextInt(NON_INNATE_BELIEF_ATOMICs.length)];
    }

//    private static class InperienceTask extends NALTask {
//
//        public InperienceTask(Compound r, Truth t, long now, long start, long end, Task task) {
//            super(r, Op.BELIEF, t, now, start, end, task.stamp());
//        }
//
//        @Override
//        public ITask[] run(NAR n) {
//            logger.info(" {}", this);
//            return super.run(n);
//        }
//    }

//    public static boolean random(@NotNull Random r, float prob, int volume) {
//        return r.nextFloat()*volume <= prob;
//    }

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


//    private void internalizeImplication(@NotNull Task task, @NotNull Task belief, @NotNull Premise nal) {
//        Compound taskTerm = task.term();
//        Compound beliefTerm = belief.term();
//
//        if (beliefTerm.temporal() && beliefTerm.dt() > 0) {
//            //1. check if its (&/,term,+i1,...,+in) =/> anticipateTerm form:
//            boolean valid = true;
//            Term impsub = beliefTerm.term(0);
//            if (impsub.op() == Op.CONJ) {
//                Compound conj = (Compound) impsub;
//                if (!conj.term(0).equals(taskTerm)) {
//                    valid = false; //the expected needed term is not included
//                }
//            } else {
//                if (!impsub.equals(taskTerm)) {
//                    valid = false;
//                }
//            }
//
//            //TODO use interval?
//
//
//            if (valid) {
//                Compound c = $.func(anticipate, beliefTerm.term(1));
//                //if (c!=null) {
//                //long interval = (impsub instanceof Interval ? ((Interval)impsub).duration() : 0);
//                //int interval = 0;
//
//                input(task, belief,
//                        c,
//                        0);
//                //}
//            }
//        }
//    }

//    void nonInnate(@NotNull Task task, @NotNull Task belief, @NotNull Atomic op) {
//        //the Atomics which dont have a innate belief
//        //also get a chance to reveal its effects to the system this way
//
//        Compound c = $.func(op, $.p(belief.term()));
//        if (c != null)
//            input(task, belief, c, 0);
//    }
//
//    void input(@NotNull Task parent, Task belief, @NotNull Compound new_term, long delay) {
//
//        //TODO should this be a mew stamp or attached to parent.. originally it was a fresh new stamp from memory
//
//        long now = nar.time();
//        long when = now + delay;
//
//        nar.input(
//            new ImmutableTask(new_term, GOAL, $.t(1f, nar.confidenceDefault(GOAL)), now, when, when, new long[] { nar.time.nextStamp() })
//                .log("Inperience")
//        );
//    }


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
