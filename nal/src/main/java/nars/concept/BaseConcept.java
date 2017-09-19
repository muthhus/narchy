package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.builder.ConceptBuilder;
import nars.concept.state.ConceptState;
import nars.control.Activate;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.concept.state.ConceptState.Deleted;
import static nars.concept.state.ConceptState.New;

/**
 * concept of a compound term which can NOT name a task, so it has no task tables and ability to process tasks
 */
public class BaseConcept extends ConcurrentHashMap implements Concept, Termlike {

    @NotNull
    public final Term term;
    @NotNull
    public final BeliefTable beliefs;
    @NotNull
    public final BeliefTable goals;
    @NotNull
    public final QuestionTable quests;
    @NotNull
    public final QuestionTable questions;
    @NotNull
    public final Bag<Task, PriReference<Task>> taskLinks;
    @NotNull
    public final Bag<Term, PriReference<Term>> termLinks;
    @NotNull
    public transient ConceptState state = Deleted;
    private final Collection<Termed> templates;

    public BaseConcept(@NotNull Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder conceptBuilder) {
        this(term,
                beliefs != null ? beliefs : conceptBuilder.newBeliefTable(term, true),
                goals != null ? goals : conceptBuilder.newBeliefTable(term, false),
                conceptBuilder.newQuestionTable(term, true), conceptBuilder.newQuestionTable(term, false), conceptBuilder.newLinkBags(term));
    }

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public BaseConcept(@NotNull Term term,
                       @NotNull BeliefTable beliefs, @NotNull BeliefTable goals,
                       @NotNull QuestionTable questions, @NotNull QuestionTable quests,
                       @NotNull Bag[] bags) {
        super(0);
        assert (term.op().conceptualizable);
        this.term = term;
        this.termLinks = bags[0];
        this.taskLinks = bags[1];
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;
        this.state = New;

        templates = TermLinks.templates(term);
    }


    @Override
    public Activate activate(float pri, NAR n) {
        //store per 'self' term allowing a schizo NAR to assign different activations to each 'personality'
        Activate a = (Activate) computeIfAbsent(n.self(), (s) ->
                new Activate(BaseConcept.this, 0)
        );
        //TODO forget based on dt
        a.priAdd(pri);
        n.input(a);
        return a;
    }

    @Override
    public final Term term() {
        return term;
    }

    @Override
    public final boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return term.containsRecursively(t, inSubtermsOf);
    }

    @Override
    public final /*@NotNull*/ Op op() {
        return term.op();
    }


    @Override
    public @NotNull Bag<Task, PriReference<Task>> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term, PriReference<Term>> termlinks() {
        return termLinks;
    }


    @Override
    public Collection<Termed> templates() {
        return templates;
    }

    /**
     * used for setting an explicit OperationConcept instance via java; activates it on initialization
     */
    public BaseConcept(@NotNull Term term, @NotNull NAR n) {
        this(term, n.terms.conceptBuilder);
    }


    public BaseConcept(@NotNull Term term, @NotNull ConceptBuilder b) {
        this(term, b.newBeliefTable(term, true), b.newBeliefTable(term, false),
                b.newQuestionTable(term, true), b.newQuestionTable(term, false),
                b.newLinkBags(term));
    }

    @Override
    public final ConceptState state() {
        return state;
    }

    @Override
    @NotNull
    public QuestionTable quests() {
        return quests;
    }

    @Override
    @NotNull
    public QuestionTable questions() {
        return questions;
    }

    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @Override
    @NotNull
    public final BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @Override
    @NotNull
    public final BeliefTable goals() {
        return goals;
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs.setCapacity(be, bt);
        goals.setCapacity(ge, gt);

    }


    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof Concept && term.equals(((Concept) obj).term()));
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final String toString() {
        return term.toString();
    }

    @Override
    public int size() {
        return term.size();
    }

    /**
     * first-level only
     */
    @Deprecated
    @Override
    public boolean contains(@NotNull Termlike t) {
        return term.contains(t);
    }

    @Override
    public Term sub(int i, Term ifOutOfBounds) {
        return term.sub(i, ifOutOfBounds);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return term.AND(v);
    }

    @Override
    public boolean ANDrecurse(@NotNull Predicate<Term> v) {
        return term.ANDrecurse(v);
    }

    @Override
    public void recurseTerms(@NotNull Consumer<Term> v) {
        term.recurseTerms(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return term.OR(v);
    }

    @Override
    public boolean ORrecurse(@NotNull Predicate<Term> v) {
        return term.ORrecurse(v);
    }

    @Deprecated
    @Override
    public int vars() {
        return term.vars();
    }

    @Deprecated
    @Override
    public int varIndep() {
        return term.varIndep();
    }

    @Deprecated
    @Override
    public int varDep() {
        return term.varDep();
    }

    @Deprecated
    @Override
    public int varQuery() {
        return term.varQuery();
    }

    @Deprecated
    @Override
    public int varPattern() {
        return term.varPattern();
    }

    @Deprecated
    @Override
    public int complexity() {
        return term.complexity();
    }

    @Deprecated
    @Override
    public int structure() {
        return term.structure();
    }

    @Override
    public int volume() {
        return term.volume();
    }

    @Override
    public boolean isNormalized() {
        return term.isNormalized(); //compound concepts may be un-normalized
    }

    @Override
    public boolean isDynamic() {
        throw new UnsupportedOperationException("concepts should not be tested for evaluability");
    }


    @Override
    public ConceptState state(@NotNull ConceptState p) {
        ConceptState current = this.state;
        if (current != p) {
            this.state = p;
            termLinks.setCapacity(p.linkCap(this, true));
            taskLinks.setCapacity(p.linkCap(this, false));

            int be = p.beliefCap(this, true, true);
            int bt = p.beliefCap(this, true, false);

            int ge = p.beliefCap(this, false, true);
            int gt = p.beliefCap(this, false, false);

            beliefCapacity(be, bt, ge, gt);
            questions.capacity(p.questionCap(true));
            quests.capacity(p.questionCap(false));

        }
        return p;
    }

    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    @Override
    public void process(@NotNull Task t, @NotNull NAR n) {
        table(t.punc()).add(t, this, n);
    }

    @Override
    public float value(@NotNull Task t, float activation, long when, NAR n) {

        return Param.inputCost(t, n) * activation; //undo the input load in proportion to the activation factor

        //return Emotivation.preferConfidentAndRelevant(t, activation, when, n);
        //positive value based on the conf but also multiplied by the activation in case it already was known
        //return valueIfProcessedAt(t, activation, n.time(), n);

//            @Override
//    public float value(@NotNull Task t, NAR n) {
//        byte p = t.punc();
//        if (p == BELIEF || p == GOAL) {// isGoal()) {
//            //example value function
//            long s = t.end();
//
//            if (s!=ETERNAL) {
//                long now = n.time();
//                long relevantTime = p == GOAL ?
//                        now - n.dur() : //present or future goal
//                        now; //future belief prediction
//
//                if (s > relevantTime) //present or future TODO irrelevance discount for far future
//                    return (float) (0.1f + Math.pow(t.conf(), 0.25f));
//            }
//        }
//
//        //return super.value(t, activation, n);
//        return 0;
//    }
    }


    @Nullable
    public TaskTable table(byte punc) {
        switch (punc) {
            case BELIEF:
                return beliefs;
            case GOAL:
                return goals;
            case QUESTION:
                return questions;
            case QUEST:
                return quests;
            default:
                throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + "'");
        }
    }

    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs) beliefs.forEachTask(each);
        if (includeConceptQuestions) questions.forEachTask(each);
        if (includeConceptGoals) goals.forEachTask(each);
        if (includeConceptQuests) quests.forEachTask(each);
    }

    public void forEachTask(@NotNull Consumer<Task> each) {
        beliefs.forEachTask(each);
        questions.forEachTask(each);
        goals.forEachTask(each);
        quests.forEachTask(each);
    }

    @Override
    public void delete(@NotNull NAR nar) {
        termLinks.delete();
        taskLinks.delete();
        beliefs.clear();
        goals.clear();
        questions.clear();
        quests.clear();
        state(ConceptState.Deleted);
        clear();
    }

    @Override
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        List<Stream<Task>> s = new LinkedList<>();
        if (includeBeliefs) s.add(beliefs.stream());
        if (includeGoals) s.add(goals.stream());
        if (includeQuestions) s.add(questions.stream());
        if (includeQuests) s.add(quests.stream());
        return s.stream().flatMap(x -> x);
    }
}

//    /**
//     * apply derivation feedback and update NAR emotion state
//     */
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull CompoundConcept concept, @NotNull NAR nar) {
//
//        //update emotion happy/sad
//        Truth before = delta.before;
//        Truth after = delta.after;
//
//        float deltaSatisfaction, deltaConf, deltaFreq;
//
//
//        if (before != null && after != null) {
//
//            deltaFreq = after.freq() - before.freq();
//            deltaConf = after.conf() - before.conf();
//
//        } else {
//            if (before == null && after != null) {
//                deltaConf = after.conf();
//                deltaFreq = after.freq();
//            } else if (before!=null) {
//                deltaConf = -before.conf();
//                deltaFreq = -before.freq();
//            } else {
//                deltaConf = 0;
//                deltaFreq = 0;
//            }
//        }
//
//        Truth other;
//        int polarity = 0;
//
//        Time time = nar.time;
//        int dur = time.dur();
//        long now = time.time();
//        if (input.isBelief()) {
//            //compare against the current goal state
//            other = concept.goals().truth(now, dur);
//            if (other != null)
//                polarity = +1;
//        } else if (input.isGoal()) {
//            //compare against the current belief state
//            other = concept.beliefs().truth(now, dur);
//            if (other != null)
//                polarity = -1;
//        } else {
//            other = null;
//        }
//
//
//        if (other != null) {
//
//            float otherFreq = other.freq();
//
//            if (polarity==0) {
//
//                //ambivalence: no change
//                deltaSatisfaction = 0;
//
//            } else {
//
////                if (otherFreq > 0.5f) {
////                    //measure how much the freq increased since goal is positive
////                    deltaSatisfaction = +polarity * deltaFreq / (2f * (otherFreq - 0.5f));
////                } else {
////                    //measure how much the freq decreased since goal is negative
////                    deltaSatisfaction = -polarity * deltaFreq / (2f * (0.5f - otherFreq));
////                }
//
//                if (after!=null) {
//                    deltaSatisfaction = /*Math.abs(deltaFreq) * */ (2f * (1f - Math.abs(after.freq() - otherFreq)) - 1f);
//
//                    deltaSatisfaction *= (after.conf() * other.conf());
//
//                    nar.emotion.happy(deltaSatisfaction);
//                } else {
//                    deltaSatisfaction = 0;
//                }
//            }
//
//
//        } else {
//            deltaSatisfaction = 0;
//        }
//
//        feedback(input, delta, nar, deltaSatisfaction, deltaConf);
//
//    }
//
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull NAR nar, float deltaSatisfaction, float deltaConf) {
//        if (!Util.equals(deltaConf, 0f, TRUTH_EPSILON))
//            nar.emotion.confident(deltaConf, input.term());
//
//        input.feedback(delta, deltaConf, deltaSatisfaction, nar);
//    }

//    private void checkConsistency() {
//        synchronized (tasks) {
//            int mapSize = tasks.size();
//            int tableSize = beliefs().size() + goals().size() + questions().size() + quests().size();
//
//            int THRESHOLD = 50; //to catch when the table explodes and not just an off-by-one inconsistency that will correct itself in the next cycle
//            if (Math.abs(mapSize - tableSize) > THRESHOLD) {
//                //List<Task> mapTasks = new ArrayList(tasks.keySet());
//                Set<Task> mapTasks = tasks.keySet();
//                ArrayList<Task> tableTasks = Lists.newArrayList(
//                        Iterables.concat(beliefs(), goals(), questions(), quests())
//                );
//                //Collections.sort(mapTasks);
//                //Collections.sort(tableTasks);
//
//                System.err.println(mapSize + " vs " + tableSize + "\t\t" + mapTasks.size() + " vs " + tableTasks.size());
//                System.err.println(Joiner.on('\n').join(mapTasks));
//                System.err.println("----");
//                System.err.println(Joiner.on('\n').join(tableTasks));
//                System.err.println("----");
//            }
//        }
//    }

//    public long minTime() {
//        ageFactor();
//        return min;
//    }
//
//    public long maxTime() {
//        ageFactor();
//        return max;
//    }
//
//    public float ageFactor() {
//
//        if (min == ETERNAL) {
//            //invalidated, recalc:
//            long t[] = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };
//
//            beliefs.range(t);
//            goals.range(t);
//
//            if (t[0] == Long.MAX_VALUE) {
//                min = max= 0;
//            } else {
//                min = t[0];
//                max = t[1];
//            }
//
//        }
//
//        //return 1f;
//        long range = max - min;
//        /* history factor:
//           higher means it is easier to hold beliefs further away from current time at the expense of accuracy
//           lower means more accuracy at the expense of shorter memory span
//     */
//        float historyFactor = Param.TEMPORAL_DURATION;
//        return (range == 0) ? 1 :
//                ((1f) / (range * historyFactor));
//    }

