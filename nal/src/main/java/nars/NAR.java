package nars;


import com.google.common.collect.Sets;
import jcog.data.MutableInteger;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import nars.Narsese.NarseseException;
import nars.concept.Concept;
import nars.conceptualize.state.ConceptState;
import nars.index.term.TermIndex;
import nars.op.Command;
import nars.op.Operator;
import nars.premise.DerivationBudgeting;
import nars.premise.PreferSimpleAndPolarized;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.Cycles;
import nars.util.data.Mix;
import nars.util.exe.Executioner;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.tuple.Twin;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.$.$;
import static nars.$.newArrayList;
import static nars.Op.*;
import static nars.term.Functor.f;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;
import static org.fusesource.jansi.Ansi.ansi;


/**
 * Non-Axiomatic Reasoner
 * <p>
 * Instances of this represent a reasoner connected to a Memory, and set of Input and Output channels.
 * <p>
 * All state is contained within   A NAR is responsible for managing I/O channels and executing
 * memory operations.  It executes a series sof cycles in two possible modes:
 * * step mode - controlled by an outside system, such as during debugging or testing
 * * thread mode - runs in a pausable closed-loop at a specific maximum framerate.
 * * Memory consists of the run-time state of a NAR, including: * term and concept
 * memory * clock * reasoner state * etc.
 * <p>
 * Excluding input/output channels which are managed by a NAR.
 * <p>
 * A memory is controlled by zero or one NAR's at a given time.
 * <p>
 * Memory is serializable so it can be persisted and transported.
 */
public class NAR extends Param implements Consumer<Task>, NARIn, NAROut, Cycles<NAR> {

    public static final Logger logger = LoggerFactory.getLogger(NAR.class);
    static final Set<String> logEvents = Sets.newHashSet("eventTaskProcess", "eventAnswer", "eventExecute");
    static final String VERSION = "NARchy v?.?";

    public final Executioner exe;
    private final @NotNull Random random;
    public final transient Topic<NAR> eventReset = new ArrayTopic<>();
    public final transient ArrayTopic<NAR> eventCycleStart = new ArrayTopic<>();
    public final transient Topic<Task> eventTaskProcess = new ArrayTopic<>();

    public final DerivationBudgeting budgeting = new PreferSimpleAndPolarized();

    @NotNull
    public final transient Emotion emotion;
    @NotNull
    public final Time time;
    /**
     * holds known Term's and Concept's
     */
    @NotNull
    public final TermIndex terms;


    private Focus focus = Focus.NULL_FOCUS;


    @NotNull
    private Atom self = Param.randomSelf();


    /**
     * maximum NAL level currently supported by this memory, for restricting it to activity below NAL8
     */
    int level;


    public NARLoop loop;

    public final Mix<Object, Task> mix = new Mix();


    //private final Collection<Object> on = $.newArrayList(); //registered handlers, for strong-linking them when using soft-index

    public final void printConceptStatistics() {
        printConceptStatistics(System.out);
    }

    public void printConceptStatistics(PrintStream out) {
        //Frequency complexity = new Frequency();
        Frequency clazz = new Frequency();
        Frequency policy = new Frequency();
        Frequency volume = new Frequency();
        Frequency rootOp = new Frequency();
        AtomicInteger i = new AtomicInteger(0);

        LongSummaryStatistics beliefs = new LongSummaryStatistics();
        LongSummaryStatistics goals = new LongSummaryStatistics();
        LongSummaryStatistics questions = new LongSummaryStatistics();
        LongSummaryStatistics quests = new LongSummaryStatistics();

        LongSummaryStatistics termlinksCap = new LongSummaryStatistics();
        LongSummaryStatistics termlinksUsed = new LongSummaryStatistics();
        LongSummaryStatistics tasklinksCap = new LongSummaryStatistics();
        LongSummaryStatistics tasklinksUsed = new LongSummaryStatistics();

        forEachConcept(c -> {
            i.incrementAndGet();
            //complexity.addValue(c.complexity());
            volume.addValue(c.volume());
            rootOp.addValue(c.op());
            clazz.addValue(c.getClass().toString());

            @Nullable ConceptState p = c.state();
            policy.addValue(p != null ? p.toString() : "null");

            if (!(c instanceof Functor)) {
                termlinksCap.accept(c.termlinks().capacity());
                termlinksUsed.accept(c.termlinks().size());
                tasklinksCap.accept(c.tasklinks().capacity());
                tasklinksUsed.accept(c.tasklinks().size());

                beliefs.accept(c.beliefs().size());
                goals.accept(c.goals().size());
                questions.accept(c.questions().size());
                quests.accept(c.quests().size());

            }

        });
        out.println("Total Concepts:\n" + i.get());
        out.println("\ntermLinksUsed:\n" + termlinksUsed);
        out.println("\ntermLinksCapacity:\n" + termlinksCap);
        out.println("\ntaskLinksUsed:\n" + tasklinksUsed);
        out.println("\ntaskLinksCapacity:\n" + tasklinksCap);
        //out.println("\nComplexity:\n" + complexity);
        out.println("\npolicy:\n" + policy);
        out.println("\nrootOp:\n" + rootOp);
        out.println("\nvolume:\n" + volume);
        out.println("\nclass:\n" + clazz);

    }


    public NAR(@NotNull Time time, @NotNull TermIndex terms, @NotNull Random rng, @NotNull Executioner exe) {

        this.random = rng;

        this.exe = exe;

        this.level = 8;

        this.time = time;

        this.terms = terms;

        this.emotion = new Emotion();

        if (terms.nar == null) //dont reinitialize if already initialized, for sharing
            terms.start(this);

        restart();
    }


    /**
     * Reset the system with an empty memory and reset clock.  Event handlers
     * will remain attached but enabled plugins will have been deactivated and
     * reactivated, a signal for them to empty their state (if necessary).
     */
    @NotNull
    public void reset() {

        synchronized (exe) {

            stop();

            clear();

            restart();

        }

    }

    public void clear() {
        synchronized (exe) {
            eventReset.emit(this);
        }
    }

    /**
     * initialization and post-reset procedure
     */
    protected void restart() {

        time.clear();

        exe.start(this);

    }


    public void setFocus(Focus focus) {
        this.focus = focus;
    }


    public void setSelf(String self) {
        setSelf((Atom) Atomic.the(self));
    }

    public void setSelf(Atom self) {
        this.self = self;
    }

    @NotNull
    public Task inputAndGet(@NotNull String taskText) throws Narsese.NarseseException {
        return inputAndGet(Narsese.the().task(taskText, this));
    }

    /**
     * parses and forms a Task from a string but doesnt input it
     */
    @NotNull
    public Task task(@NotNull String taskText) throws NarseseException {
        return Narsese.the().task(taskText, this);
    }

    @NotNull
    public List<Task> tasks(@NotNull String text) throws NarseseException {
        return Narsese.the().tasks(text, this);
    }


    @NotNull
    public List<Task> input(@NotNull String text) throws NarseseException, InvalidTaskException {
        List<Task> l = tasks(text);
        input(l);
        return l;
    }

    @NotNull
    public <T extends Term> T term(@NotNull String t) throws NarseseException {
        return terms.term(t);
    }


    /**
     * gets a concept if it exists, or returns null if it does not
     */
    @Nullable
    public final Concept concept(@NotNull String conceptTerm) throws NarseseException {
        return concept(term(conceptTerm));
    }

//    /** parses a term, returning it, or throws an exception (but will not return null) */
//    @NotNull public final Termed termOrException(@NotNull String conceptTerm) {
//        Termed t = term(conceptTerm);
//        if (t == null)
//            throw new NarseseException(conceptTerm);
//        return t;
//    }

    /**
     * ask question
     */
    @NotNull
    public void ask(@NotNull String termString) throws NarseseException {
        //TODO remove '?' if it is attached at end
        question(term(termString));
    }

    /**
     * ask question
     */
    public Task question(@NotNull Compound c) {
        return que(c, QUESTION);
    }

    public Task quest(@NotNull Compound c) {
        return que(c, QUEST);
    }

//    /**
//     * ask quest
//     */
//    @Nullable
//    public Task askShould(@NotNull String questString) throws NarseseException {
//        Term c = term(questString);
//        if (c instanceof Compound)
//            return askShould((Compound) c);
//        return null;
//    }
//
//    /**
//     * ask quest
//     */
//    @NotNull
//    public Task askShould(@NotNull Compound quest) {
//        return ask(quest, QUEST);
//    }

    @Nullable
    public Task goal(@NotNull String goalTermString, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        return goal($(goalTermString), tense, freq, conf);
    }

    /**
     * desire goal
     */
    @Nullable
    public Task goal(@NotNull Compound goalTerm, @NotNull Tense tense, float freq, float conf) {
        return goal(
                priorityDefault(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Compound term, @NotNull Tense tense, float freq, float conf) {
        return believe(term, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Compound term, @NotNull long when, float freq, float conf) {
        believe(priorityDefault(BELIEF), term, when, freq, conf);
        return this;
    }

    @NotNull
    public NAR believe(@NotNull Compound term, @NotNull Tense tense, float freq) {
        return believe(term, tense, freq, confDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Compound term, long when, float freq) {
        return believe(term, when, freq, confDefault(BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Compound term, @NotNull Tense tense, float freq) {
        return goal(term, tense, freq, confDefault(GOAL));
    }


    @Nullable
    public Task believe(float priority, @NotNull Compound term, @NotNull Tense tense, float freq, float conf) {
        return believe(priority, term, time(tense), freq, conf);
    }


    @NotNull
    public NAR believe(@NotNull Compound term, float freq, float conf) {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public Task goal(@NotNull Compound term, float freq, float conf) {
        return goal(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf) {
        try {
            believe(priorityDefault(BELIEF), term(term), time(tense), freq, conf);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public long time(@NotNull Tense tense) {
        return Tense.getRelativeOccurrence(tense, this);
    }

    @NotNull
    public NAR believe(@NotNull String termString, float freq, float conf) throws NarseseException {
        return believe(term(termString), freq, conf);
    }

    @NotNull
    public Task goal(@NotNull String termString) {
        try {
            return goal(term(termString), true);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public NAR believe(@NotNull String termString) throws NarseseException {
        return believe(termString, true);
    }

    @NotNull
    public NAR believe(@NotNull String termString, boolean isTrue) throws NarseseException {
        return believe(term(termString), isTrue);
    }

    @NotNull
    public Task goal(@NotNull String termString, boolean isTrue) throws NarseseException {
        return goal(term(termString), isTrue);
    }

    @NotNull
    public NAR believe(@NotNull Compound term) {
        return believe(term, true);
    }

    @NotNull
    public NAR believe(@NotNull Compound term, boolean trueOrFalse) {
        return believe(term, trueOrFalse, confDefault(BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Compound term) {
        return goal(term, true);
    }

    @NotNull
    public Task goal(@NotNull Compound term, boolean trueOrFalse) {
        return goal(term, trueOrFalse, confDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Compound term, boolean trueOrFalse, float conf) {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @NotNull
    public Task goal(@NotNull Compound term, boolean trueOrFalse, float conf) {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri, @NotNull Compound term, long occurrenceTime, float freq, float conf) throws InvalidTaskException {
        return input(pri, term, BELIEF, occurrenceTime, freq, conf);
    }


    @Nullable
    public Task goal(float pri, @NotNull Compound goal, long when, float freq, float conf) throws InvalidTaskException {
        return input(pri, goal, GOAL, when, freq, conf);
    }

    @Nullable
    public Task input(float pri, Compound term, byte punc, long occurrenceTime, float freq, float conf) throws InvalidTaskException {

        if (term == null) {
            throw new NullPointerException("null task term");
        }

        DiscreteTruth tr = new DiscreteTruth(freq, conf, confMin.floatValue());
        if (tr == null) {
            throw new InvalidTaskException(term, "insufficient confidence");
        }

        Task y = new NALTask((Compound) term, punc, tr, time(), occurrenceTime, occurrenceTime, new long[]{time.nextStamp()});
        y.setPri(pri);

        input(y);

        return y;
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Compound term, byte questionOrQuest) {
        return que(term, questionOrQuest, ETERNAL);
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Compound term, byte punc, long when) {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        assert ((punc == QUESTION) || (punc == QUEST)); //throw new RuntimeException("invalid punctuation");

        return inputAndGet(
                new NALTask(term, punc, null,
                        time(), when, when,
                        new long[]{time.nextStamp()}
                ).budget(this)
        );
    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    @NotNull
    public NAR logBudgetMin(@NotNull Appendable out, float priThresh) {
        return log(out, v -> {
            Prioritized b = null;
            if (v instanceof Prioritized) {
                b = ((Prioritized) v);
            } else if (v instanceof Twin) {
                if (((Twin) v).getOne() instanceof Prioritized) {
                    b = (Prioritized) ((Twin) v).getOne();
                }
            }
            return b != null && b.pri() > priThresh;
        });
    }

    /**
     * main task entry point
     */
    public void input(@NotNull ITask... t) {
        for (ITask x : t)
            exe.run(x);
    }


//    private boolean executable(Task input) {
//        if (input.isCommand())
//            return true;
//
//        Concept c = input.concept(this);
//        float be;
//        if (c == null)
//            be = 0;
//        else {
//            Truth b = c.belief(time(), time.dur());
//            if (b == null)
//                be = 0;
//            else
//                be = b.expectation();
//        }
//
//        return input.expectation() - be >= Param.EXECUTION_THRESHOLD;
//    }

//    @Deprecated
//    public @Nullable Task execute(Task cmd) {
//
//
//        Compound inputTerm = cmd.term();
//        if (inputTerm.hasAll(Operator.OPERATOR_BITS) && inputTerm.op() == INH) {
//            Term func = inputTerm.term(1);
//            if (func.op() == ATOM) {
//                Term args = inputTerm.term(0);
//                if (args.op() == PROD) {
//                    Concept funcConcept = concept(func);
//                    if (funcConcept != null) {
//                        Operator o = funcConcept.get(Operator.class);
//                        if (o != null) {
//
//
//                            /*if (isCommand)*/
//                            {
//                                Task result = o.run(cmd, this);
//                                if (result != null && result != cmd) {
//                                    //return input(result); //recurse
//                                    return result;
//                                }
//                            }
////                            } else {
////
////                                if (!cmd.isEternal() && cmd.start() > time() + time.dur()) {
////                                    inputAt(cmd.start(), cmd); //schedule for execution later
////                                    return null;
////                                } else {
////                                    if (executable(cmd)) {
////                                        Task result = o.run(cmd, this);
////                                        if (result != cmd) { //instance equality, not actual equality in case it wants to change this
////                                            if (result == null) {
////                                                return null; //finished
////                                            } else {
////                                                //input(result); //recurse until its stable
////                                                return result;
////                                            }
////                                        }
////                                    }
////                                }
////                            }
//
//                        }
//
//                    }
//                }
//            }
//        }
//
//        /*if (isCommand)*/
//        {
//            eventTaskProcess.emit(cmd);
//            return null;
//        }
//
//    }

//    /**
//     * override to perform any preprocessing of a task (applied before the normalization step)
//     */
//    @Nullable
//    public Task pre(@NotNull Task t) {
//        return t;
//    }

    @NotNull
    public Term pre(@NotNull Term t) {
        return t;
    }

    @NotNull
    public Compound pre(@NotNull Compound c) {
        Compound d = compoundOrNull(pre((Term) c));
        if (d == null)
            return c; //unchanged because post-processing resulted in invalid or non-compound
        else
            return d;
    }

    /**
     * override to apply any post-processing of a task before it is made available for external use (ex: decompression)
     */
    @NotNull
    public Task post(@NotNull Task t) {
        return t;
    }

    /**
     * override to apply any post-processing of a term before it is made available for external use (ex: decompression)
     */
    @NotNull
    public Term post(@NotNull Term t) {
        return t;
    }

    @Nullable
    public Compound post(@NotNull Compound c) {
        Compound d = compoundOrNull(post((Term) c));
        if (d == null)
            return c; //unchanged because post-processing resulted in invalid or non-compound
        else
            return d;
    }


//    protected void processDuplicate(@NotNull Task input, Task existing) {
//        if (existing != input) {
//
//            //different instance
//
//            float reactivation;
//            Budget e = existing.budget();
//            if (!existing.isDeleted()) {
//                float ep = e.priSafe(0);
//                reactivation = Util.unitize((input.priSafe(0) - ep) / ep);
//                if (reactivation > 0) {
//                    DuplicateMerge.merge(e, input, 1f);
//                }
//                input.feedback(null, Float.NaN, Float.NaN, this);
//            } else {
//                //this may never get called due to the replacement above
//                //attempt to revive deleted task
//                e.set(input.budget());
//                reactivation = 1f;
//            }
//
//            input.delete();
//
//            //re-activate only
//            if (reactivation > 0) {
//                Concept c = existing.concept(this);
//                if (c != null) {
//                    activateTask(existing, c, reactivation);
//                }
//            }
//
//        }
//    }


//    /**
//     * meta-reasoner evaluator
//     */
//    @Nullable
//    public void inputCommand(@NotNull Compound x) {
//
////        Term y;
////
////        if (x.op() == INH && x.isTerm(0, PROD) && x.isTerm(1, ATOM)) {
////            Term functor = x.term(1);
////            Term[] args = x.compound(0).terms();
////
////            Concept functorConcept = concept(functor);
////            if (functorConcept instanceof Functor) {
////                y = ((Functor) functorConcept).apply(args);
////            } else {
////                y = the("unknown_command_functor");
////            }
////
////        } else {
////            y = the("unknown_command_pattern");
////        }
////
////        Compound z;
////        if (y == null)
////            z = x;
////        else
////            z = func(self, x, y); //memoization of the command and its result
////
////        //logger.info(" {}", z);
//
////        eventTaskProcess.emit(command(z));
//
//        //return z;
//
//    }
//

    @Override
    public final void accept(@NotNull Task task) {
        input(task);
    }


    @Deprecated
    public final void on(@NotNull String atom, @NotNull Operator o) {
        on((Atom) Atomic.the(atom), o);
    }

    @Deprecated
    public final void on(@NotNull Atom a, @NotNull Operator o) {
//        DefaultConceptBuilder builder = (DefaultConceptBuilder) terms.conceptBuilder();
//        PermanentAtomConcept c = builder.withBags(a,
//                (termlink, tasklink) -> new PermanentAtomConcept(a, termlink, tasklink)
//        );
//        c.put(Operator.class, o);
//        terms.set(c);
        on(new Command(a) {

            @Override
            public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
                Compound c = t.term();
                //try {
                o.run((Atomic) (c.sub(1)), ((Compound) (t.term(0))).toArray(), nar);
                return t;
//                } catch (Throwable error) {
//                    if (Param.DEBUG)
//                        throw error;
//                    else
//                        return error(error);
//                }

                //return o.run(t, nar);
            }

        });
    }

    public final void on(@NotNull Command c) {
        terms.set(c);
    }

    public void input(Function<NAR, Task>... tasks) {
        for (Function<NAR, Task> x : tasks) {

            input(x.apply(this));

        }
    }

    public final int dur() {
        return time.dur();
    }

    /**
     * provides a Random number generator
     */
    public Random random() {
        return random;
    }

    /**
     * returns concept belief/goal truth evaluated at a given time
     */
    @Nullable
    public Truth truth(@Nullable Termed concept, byte punc, long when) {
        if (concept != null) {
            @Nullable Concept c = concept(concept);
            if (c != null) {
                BeliefTable table;
                switch (punc) {
                    case BELIEF:
                        table = c.beliefs();
                        break;
                    case GOAL:
                        table = c.goals();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                return table.truth(when, dur());
            }
        }
        return null;
    }

    @Nullable
    public Truth beliefTruth(String concept, long when) throws NarseseException {
        return truth(concept(concept), BELIEF, when);
    }

    @Nullable
    public Truth goalTruth(String concept, long when) throws NarseseException {
        return truth(concept(concept), GOAL, when);
    }

    @Nullable
    public Truth beliefTruth(Termed concept, long when) {
        return truth(concept, BELIEF, when);
    }

    @Nullable
    public Truth goalTruth(Termed concept, long when) {
        return truth(concept, GOAL, when);
    }


    /**
     * Exits an iteration loop if running
     */
    public void stop() {
        synchronized (exe) {
            if (loop != null) {
                loop.stop();
                loop = null;
            }
            exe.stop();
        }
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public final void cycle() {
        time.cycle();

        emotion.cycle();

        exe.cycle(this);
    }


    /**
     * Runs multiple frames, unless already running (then it return -1).
     *
     * @return total time in seconds elapsed in realtime
     */
    @NotNull
    public final NAR run(int frames) {

        for (; frames > 0; frames--) {
            cycle();
        }

        return this;
    }

//    private void runAsyncFrameTasks() {
//        try {
//            int active = asyncPerFrame.getActiveCount();
//            if (active > 0) {
//
//                asyncPerFrame.awaitTermination(0, TimeUnit.MINUTES);
//                //asyncPerFrame.shutdown();
//                asyncPerFrame = null;
//
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    @NotNull
    public NAR trace(@NotNull Appendable out, Predicate<String> includeKey) {
        return trace(out, includeKey, null);
    }

    /* Print all statically known events (discovered via reflection)
    *  for this reasoner to a stream
    * */
    @NotNull
    public NAR trace(@NotNull Appendable out, Predicate<String> includeKey, @Nullable Predicate includeValue) {


        String[] previous = {null};

        Topic.all(this, (k, v) -> {

            if (includeValue != null && !includeValue.test(v))
                return;

            if (k.startsWith("event")) k = k.substring(5); //remove 'event' prefix

            try {
                outputEvent(out, previous[0], k, v);
            } catch (IOException e) {
                logger.error("outputEvent: {}", e.toString());
            }
            previous[0] = k;
        }, includeKey);

        return this;
    }

    @NotNull
    public NAR trace(@NotNull Appendable out) {
        return trace(out, k -> true);
    }

    @NotNull
    public NAR log() {
        return log(System.out);
    }

    @NotNull
    public NAR log(@NotNull Appendable out) {
        return log(out, null);
    }

    @NotNull
    public NAR log(@NotNull Appendable out, Predicate includeValue) {
        return trace(out, NAR.logEvents::contains, includeValue);
    }

    public void outputEvent(@NotNull Appendable out, String previou, @NotNull String chan, Object v) throws IOException {
        //indent each cycle
        if (!"eventCycleStart".equals(chan)) {
            out.append("  ");
        }

        if (!chan.equals(previou)) {
            out
                    //.append(ANSI.COLOR_CONFIG)
                    .append(chan)
                    //.append(ANSI.COLOR_RESET )
                    .append(": ");
            //previou = chan;
        } else {
            //indent
            for (int i = 0; i < chan.length() + 2; i++)
                out.append(' ');
        }

        if (v instanceof Object[]) {
            v = Arrays.toString((Object[]) v);
        } else if (v instanceof Task) {
            Task tv = ((Task) v);
            v = ansi()
                    .a(tv.originality() >= 0.33f ?
                            Ansi.Attribute.INTENSITY_BOLD :
                            Ansi.Attribute.INTENSITY_FAINT)
                    .a(tv.pri() > 0.5f ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF)
                    .fg(Priority.budgetSummaryColor(tv))
                    .a(
                            tv.toString(this, true)
                    )
                    .reset()
                    .toString();
        }

        out.append(v.toString()).append('\n');
    }

    /**
     * creates a new loop which runs at max speed
     */
    @NotNull
    public NARLoop start() {
        return startPeriodMS(0);
    }

    @NotNull
    public final NARLoop startFPS(float initialFPS) {
        if (initialFPS < 0)
            return startPeriodMS((int) -1); //pause

        if (initialFPS == 0)
            return startPeriodMS((int) 0); //infinite

        float millisecPerFrame = 1000.0f / initialFPS;
        return startPeriodMS((int) millisecPerFrame);
    }

    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param initialFramePeriodMS in milliseconds
     */
    @NotNull
    public NARLoop startPeriodMS(int initialFramePeriodMS) {

        synchronized (exe) {

            NARLoop ll = this.loop;
            if (ll != null && !ll.isStopped()) {
                if (initialFramePeriodMS < 0) {
                    ll.stop();
                    this.loop = null;
                } else {
                    ll.setPeriodMS(initialFramePeriodMS);
                }
            } else if ((ll == null) || (ll.isStopped())) {
                this.loop = ll = (initialFramePeriodMS >= 0) ? new NARLoop(this, initialFramePeriodMS) : null;
            }

            return this.loop;
        }

    }


    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(@NotNull Runnable t) {
        exe.runLater(t);
    }

    /**
     * run a procedure for each item in chunked stripes
     */
    public final <X> void runLater(@NotNull List<X> items, @NotNull Consumer<X> each, int maxChunkSize) {

        int conc = exe.concurrency();
        if (conc == 1) {
            //special single-thread case: just execute all
            items.forEach(each);
        } else {
            int s = items.size();
            int chunkSize = Math.max(1, Math.min(maxChunkSize, (int) Math.floor(s / conc)));
            for (int i = 0; i < s; ) {
                int start = i;
                int end = Math.min(i + chunkSize, s);
                runLater(() -> {
                    for (int j = start; j < end; j++) {
                        X x = items.get(j);
                        if (x != null)
                            each.accept(x);
                    }
                });
                i += chunkSize;
            }
        }
    }


    @NotNull
    @Override
    public String toString() {
        return self() + ":" + getClass().getSimpleName();
    }


    @NotNull
    public NAR input(@NotNull String... ss) throws NarseseException {
        for (String s : ss)
            input(s);
        return this;
    }

    public NAR inputNarsese(@NotNull InputStream inputStream) throws IOException, NarseseException {
        String x = new String(inputStream.readAllBytes());
        /*List<Task> y = */
        input(x);
        return this;
    }

    @NotNull
    public NAR inputAt(long time, @NotNull String... tt) throws NarseseException {
        //LongPredicate timeCondition = t -> t == time;

        List<Task> yy = newArrayList(tt.length);
        for (String s : tt) {
            tasks(s).forEach(x -> {
                long xs = x.start();
                long xe = x.end();
                Task y = Task.clone(x, time, xs != ETERNAL ? time : ETERNAL, xs != ETERNAL ? time + (xe - xs) : ETERNAL);
                yy.add(y);
            });
        }

//        //set the appropriate creation and occurrence times
//        for (Task y : x) {
//            TaskBuilder my = (TaskBuilder) y;
//            my.setCreationTime(time);
//            if (my.start() != ETERNAL)
//                my.occurr(time);
//        }

        inputAt(time, yy.toArray(new Task[yy.size()]));
        return this;
    }

    /**
     * TODO use a scheduling using r-tree
     */
    public void inputAt(long when, @NotNull ITask... x) {
        long now = time();
        if (when < now) {
            //past
            throw new RuntimeException("can not input at a past time");
        } else if (when == now) {
            //current cycle
            input(x);
        } else {

            //future
//            if (Param.DEBUG) {
//                for (Task t : x)
//                    ((ImmutableTask) t).log("Scheduled");
//            }


            Consumer<NAR> z = new Consumer<NAR>() {
                @Override
                public void accept(NAR m) {
                    //if (timeCondition.test(m.time())) {
                    if (m.time() == when) {
                        eventCycleStart.disable((Consumer) this);
                        m.input(x);
                    }
                }
            };
            eventCycleStart.enable(z);

        }
    }

    @NotNull
    public NAR forEachTask(@NotNull Consumer<Task> each) {
        forEachConcept(c -> c.forEachTask(each));
        return this;
    }

    @NotNull
    public NAR forEachTask(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        forEachConcept(c -> {
            c.forEachTask(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests, each);
        });
        return this;
    }

    @NotNull
    public NAR forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests,
                           boolean includeTaskLinks, int maxPerConcept,
                           @NotNull Consumer<Task> recip) {
        Consumer<? super PLink<Task>> action = t -> recip.accept(t.get());
        forEachConcept(c -> {
            if (includeConceptBeliefs) c.beliefs().forEach(maxPerConcept, recip);
            if (includeConceptQuestions) c.questions().forEach(maxPerConcept, recip);
            if (includeConceptGoals) c.goals().forEach(maxPerConcept, recip);
            if (includeConceptQuests) c.quests().forEach(maxPerConcept, recip);
            if (includeTaskLinks) {
                c.tasklinks().sample(maxPerConcept, action);
            }
        });

        return this;
    }


    /**
     * resolves a term or concept to its currrent Concept
     */
    @Nullable
    public Concept concept(@NotNull Termed termed) {
        return concept(termed, false);
    }

    /**
     * resolves a term to its Concept; if it doesnt exist, its construction will be attempted
     */
    @Nullable
    public Concept conceptualize(@NotNull Termed termed) {
        return concept(termed, true);
    }

    @Nullable
    private Concept concept(@NotNull Termed termed, boolean createIfMissing) {

        if (termed instanceof Concept) {
            Concept ct = (Concept) termed;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
        }

        Term term = conceptTerm(termed.unneg());
        return (term == null) ? null : terms.concept(term, createIfMissing);


    }

    /**
     * returns the canonical Concept term for any given Term, or null if it is unconceptualizable
     */
    @Nullable
    public Term conceptTerm(@NotNull Term term) {

        if (term instanceof Variable) {
            return null;
        }

//        Term termPre = null;
//        while (term instanceof Compound && termPre != term) {
//            //shouldnt need to check for this here
//            if (isTrueOrFalse(term))
//                throw new UnsupportedOperationException();

//            termPre = term;


        if (term instanceof Compound) {
            term = terms.atemporalize((Compound) term);
            if (term == null)
                return null;

            //atemporalizing can reset normalization state of the result instance
            //since a manual normalization isnt invoked. until here, which depends if the original input was normalized:

            term = compoundOrNull(terms.normalize((Compound) term));
            if (term == null)
                return null;

            term = compoundOrNull(term.unneg());
//                    if (nterm == null) {
//                        concepts.normalize((Compound)term);
//                        return null;
//                    }

        }


        if (term == null || (term instanceof Variable) || (isTrueOrFalse(term)))
            return null;
        return term;
    }

    @Nullable
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        focus().concepts().forEach(n -> recip.accept((Concept) n.get()));
        return this;
    }

    @NotNull
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        terms.forEach(x -> {
            if (x instanceof Concept)
                recip.accept((Concept) x);
        });
        return this;
    }


//    /**
//     * activate the concept and other features (termlinks, etc)
//     *
//     * @param link whether to activate termlinks recursively
//     */
//    @Nullable
//    public abstract Concept activate(@NotNull Termed<?> termed, @Nullable Activation activation);
//
//    @Nullable
//    final public Concept activate(@NotNull Termed<?> termed, @NotNull Budgeted b) {
//        return activate(termed, new Activation(b, 1f));
//    }

    @NotNull
    public NAR stopIf(@NotNull BooleanSupplier stopCondition) {
        onCycle(n -> {
            if (stopCondition.getAsBoolean())
                stop();
        });
        return this;
    }


    /**
     * a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame.
     */
    @NotNull
    public final On onCycle(@NotNull Consumer<NAR> each) {
        return eventCycleStart.on(each);
    }

    /**
     * avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected
     */
    @NotNull
    public final On onCycleWeak(@NotNull Consumer<NAR> each) {
        return eventCycleStart.onWeak(each);
    }

    @NotNull
    public final On onCycle(@NotNull Runnable each) {
        return onCycle((ignored) -> {
            each.run();
        });
    }

    @NotNull
    @Deprecated
    public NAR eachCycle(@NotNull Consumer<NAR> each) {
        onCycle(each);
        return this;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void input(@NotNull Iterable<? extends ITask> tasks) {
        tasks.forEach(x -> {
            if (x != null)
                input(x);
        });
    }

    public final void input(@NotNull Stream<? extends ITask> taskStream) {
        taskStream.filter(Objects::nonNull).forEach(this::input);
    }


    @Override
    public final boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    @Nullable
    public PLink<Concept> activate(Termed c, float priToAdd) {
        @Nullable Concept cc = conceptualize(c);
        return (cc != null) ? focus.activate(cc, priToAdd) : null;
    }

    /**
     * @return current priority of the named concept, or NaN if concept isnt active
     */
    public float pri(@NotNull Termed termed) {
        return focus.pri(termed);
    }

    @NotNull
    public On onTask(@NotNull Consumer<Task> o) {
        return eventTaskProcess.on(o);
    }

    public @NotNull NAR believe(@NotNull Compound c, @NotNull Tense tense) {
        return believe(c, tense, 1f);
    }

    /**
     * activate/"turn-ON"/install a concept in the index and activates it, used for setup of custom concept implementations
     * implementations should apply active concept capacity policy
     */
    @NotNull
    public final Concept on(@NotNull Concept c) {

        Concept existing = concept(c.term());
        if ((existing != null) && (existing != c))
            throw new RuntimeException("concept already indexed for term: " + c.term());

        terms.set(c);

        return c;
    }

    /**
     * registers a term rewrite functor
     */
    @NotNull
    public final Concept on(@NotNull String termAtom, @NotNull Function<TermContainer, Term> f) {
        return on(f(termAtom, f));
    }


    @Override
    public final int level() {
        return level;
    }

    /**
     * sets current maximum allowed NAL level (1..8)
     */
    public final void nal(int newLevel) {
        level = newLevel;
    }

    public final void setState(@NotNull Concept c, @NotNull ConceptState p) {

        if (c.state(p, this) != p) {
            terms.onStateChanged(c);
        }

    }

    public final long time() {
        return time.time();
    }


    public @NotNull NAR inputBinary(@NotNull File input) throws IOException {
        return inputBinary(new FileInputStream(input));
    }

    @NotNull
    public NAR outputBinary(@NotNull File f, boolean append, @NotNull Function<Task, Task> each) throws IOException {
        FileOutputStream ff = new FileOutputStream(f, append);
        outputBinary(ff, each);
        ff.close();
        return this;
    }

    /**
     * byte codec output of matching concept tasks (blocking)
     * <p>
     * the each function allows transforming each task to an optional output form.
     * if this function returns null it will not output that task (use as a filter).
     */
    @NotNull
    public NAR outputBinary(@NotNull OutputStream o, @NotNull Function<Task, Task> each) {

        //SnappyFramedOutputStream os = new SnappyFramedOutputStream(o);

        DataOutputStream oo = new DataOutputStream(o);

        MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

        forEachTask(_x -> {
            total.increment();
            Task x = post(_x);
            if (x.truth() != null && x.conf() < confMin.floatValue())
                return; //ignore task if it is below confMin

            x = each.apply(x);
            if (x != null) {
                byte[] b = IO.taskToBytes(x);
                if (b != null) {
                    try {

                        //HACK temporary until this is debugged
                        Task xx = IO.taskFromBytes(b, terms);
                        if (xx == null || !xx.equals(x)) {
                            //this can happen if a subterm is decompressed only to discover that it contradicts another part of the compound it belongs within
                            //logger.error("task serialization problem: {} != {}", _x, xx);
                        } else {

                            oo.write(b);

                            //IO.writeTask(oo, x);

                            wrote.increment();
                        }
                    } catch (Exception e) {
                        logger.error("{} can not output {}", x, e);
                        //throw new RuntimeException(e);
                        //e.printStackTrace();
                    }
                } else {
                    //warn?
                }
            }
        });

        logger.info("Saved {}/{} tasks ({} bytes)", wrote, total, oo.size());

        return this;
    }

    @NotNull
    public NAR output(@NotNull File o) throws IOException {
        return output(new FileOutputStream(o));
    }

    @NotNull
    public NAR output(@NotNull File o, Function<Task, Task> f) throws IOException {
        return outputBinary(new FileOutputStream(o), f);
    }

    @NotNull
    public NAR output(@NotNull OutputStream o) {
        return outputBinary(o, x -> x.isDeleted() ? null : x);
    }

    /**
     * byte codec input stream of tasks, to be input after decode
     * TODO use input(Stream<Task>..</Task>
     */
    @NotNull
    public NAR inputBinary(@NotNull InputStream i) throws IOException {

        //SnappyFramedInputStream i = new SnappyFramedInputStream(tasks, true);
        DataInputStream ii = new DataInputStream(i);

        int count = 0;

        while ((i.available() > 0) || (i.available() > 0) || (ii.available() > 0)) {
            Task t = IO.readTask(ii, terms);
            input(t);
            count++;
        }

        logger.info("Loaded {} tasks from {}", count, i);

        ii.close();

        return this;
    }


    /**
     * The id/name of the reasoner
     */
    public final Atom self() {
        return self;
    }

    public Focus focus() {
        return focus;
    }


}
