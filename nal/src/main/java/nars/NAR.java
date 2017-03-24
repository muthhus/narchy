package nars;


import com.google.common.collect.Sets;
import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.Prioritized;
import jcog.data.MutableInteger;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import nars.Narsese.NarseseException;
import nars.attention.Activation;
import nars.attention.SpreadingActivation;
import nars.budget.BLink;
import nars.budget.Budget;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.index.term.TermIndex;
import nars.op.Operator;
import nars.task.ImmutableTask;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.transform.Functor;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import nars.time.FrameTime;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.Cycles;
import nars.util.exe.Executioner;
import nars.util.task.InvalidTaskException;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.$.$;
import static nars.$.*;
import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.term.transform.Functor.f;
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
public class NAR extends Param implements Consumer<Task>, NARIn, NAROut, Control, Cycles<NAR> {

    public static final Logger logger = LoggerFactory.getLogger(NAR.class);
    static final Set<String> logEvents = Sets.newHashSet("eventTaskProcess", "eventAnswer", "eventExecute");
    static final String VERSION = "NARchy v?.?";

    public final Executioner exe;
    @NotNull
    public final Random random;
    public final transient Topic<NAR> eventReset = new ArrayTopic<>();
    public final transient ArrayTopic<NAR> eventCycleStart = new ArrayTopic<>();
    public final transient Topic<Task> eventTaskProcess = new ArrayTopic<>();
    final Map<PermanentAtomConcept, Operator> operators = new ConcurrentHashMap();
    @NotNull
    public final transient Emotion emotion;
    @NotNull
    public final Time time;
    /**
     * holds known Term's and Concept's
     */
    @NotNull
    public final TermIndex concepts;


    private Control control = Control.NullControl;


    @NotNull
    private Atom self = Param.randomSelf();


    /**
     * maximum NAL level currently supported by this memory, for restricting it to activity below NAL8
     */
    int level;


    private NARLoop loop;


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


    public NAR(@NotNull Time time, @NotNull TermIndex concepts, @NotNull Random rng, @NotNull Executioner exe) {


        this.random = rng;

        this.exe = exe;

        this.level = 8;

        this.time = time;

        this.concepts = concepts;

        this.emotion = new Emotion();

        concepts.start(this);

//        eventError.on(e -> {
//            if (e instanceof Throwable) {
//                Throwable ex = (Throwable) e;
//
//                //TODO move this to a specific impl of error reaction:
//                ex.printStackTrace();
//
//                if (Param.DEBUG && Param.EXIT_ON_EXCEPTION) {
//                    //throw the exception to the next lower stack catcher, or cause program exit if none exists
//                    throw new RuntimeException(ex);
//                }
//            } else {
//                logger.error(e.toString());
//            }
//        });

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

            if (!exe.isRunning()) {
                logger.warn("can not reset already stopped NAR");
                return;
            }

            stop();

            clear();

            concepts.clear();


            restart();

        }

    }

    public void clear() {
        runLater(() -> eventReset.emit(this));
    }

    /**
     * initialization and post-reset procedure
     */
    protected void restart() {

        for (Concept t : Builtin.statik)
            on(t);

        Builtin.load(this);

        time.clear();

        exe.start(this);

    }


    public void setControl(Control control) {
        this.control = control;
    }


    public void setSelf(String self) {
        setSelf((Atom) $.the(self));
    }

    public void setSelf(Atom self) {
        this.self = self;
    }


    /**
     * parses and forms a Task from a string but doesnt input it
     */
    @NotNull
    public Task task(@NotNull String taskText) throws NarseseException {
        return Narsese.the().task(taskText, this);
    }

    @NotNull
    public List<Task> tasks(@NotNull String parse) {
        List<Task> result = newArrayList(1);
        List<NarseseException> exc = newArrayList(0);
        Narsese.the().tasks(parse, result, exc, this);

        if (!exc.isEmpty())
            exc.forEach(e -> logger.error("parse error: {}", e));

        return result;
    }


    @NotNull
    public List<Task> input(@NotNull String text) {
        List<Task> l = tasks(text);
        input(l);
        return l;
    }

    @NotNull
    public Termed term(@NotNull String t) throws NarseseException {
        Termed x = concepts.parse(t);
        if (x == null) {
            //if a NarseseException was not already thrown, this indicates that it parsed but the index failed to provide its output
            throw new NarseseException("Unindexed: " + t);
        }
        return x;
    }

    @Override
    public final NAR nar() {
        return this;
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
        ask(term(termString));
    }

    /**
     * ask question
     */
    @NotNull
    public void ask(@NotNull Termed<Compound> c) {
        //TODO remove '?' if it is attached at end
        ask(c, (char) QUESTION);
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
        return goal((Termed) $(goalTermString), tense, freq, conf);
    }

    /**
     * desire goal
     */
    @Nullable
    public Task goal(@NotNull Termed<Compound> goalTerm, @NotNull Tense tense, float freq, float conf) {
        return goal(
                priorityDefault(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq, float conf) {
        return believe(term, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, @NotNull long when, float freq, float conf) {
        believe(priorityDefault(BELIEF), term, when, freq, conf);
        return this;
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq) {
        return believe(term, tense, freq, confidenceDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, long when, float freq) {
        return believe(term, when, freq, confidenceDefault(BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq) {
        return goal(term, tense, freq, confidenceDefault(GOAL));
    }


    @Nullable
    public Task believe(float priority, @NotNull Termed term, @NotNull Tense tense, float freq, float conf) {
        return believe(priority, term, time(tense), freq, conf);
    }


    @NotNull
    public NAR believe(@NotNull Termed term, float freq, float conf) {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public Task goal(@NotNull Termed term, float freq, float conf) {
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
        return believe((Termed) term(termString), freq, conf);
    }

    @NotNull
    public Task goal(@NotNull String termString) {
        try {
            return goal((Termed) term(termString), true);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public NAR believe(@NotNull String termString) {
        return believe(termString, true);
    }

    @NotNull
    public NAR believe(@NotNull String termString, boolean isTrue) {
        try {
            return believe(term(termString), isTrue);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public Task goal(@NotNull String termString, boolean isTrue) throws NarseseException {
        return goal(term(termString), isTrue);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term) {
        return believe(term, true);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse) {
        return believe(term, trueOrFalse, confidenceDefault(BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term) {
        return goal(term, true);
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term, boolean trueOrFalse) {
        return goal(term, trueOrFalse, confidenceDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf) {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf) {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri, @NotNull Termed<Compound> term, long occurrenceTime, float freq, float conf) {
        return input(pri, term, BELIEF, occurrenceTime, freq, conf);
    }


    @Nullable
    public Task goal(float pri, @NotNull Termed<Compound> goal, long when, float freq, float conf) {
        return input(pri, goal, GOAL, when, freq, conf);
    }

    @Nullable
    public Task goal(float pri, @NotNull Termed<Compound> goal, @NotNull Tense tense, float freq, float conf) {
        return input(pri, goal, GOAL, time(tense), freq, conf);
    }

    @Nullable
    public Task input(float pri, Termed<Compound> term, byte punc, long occurrenceTime, float freq, float conf) {

        if (term == null) {
            throw new NullPointerException("null task term");
        }

        Truth tr = t(freq, conf, confMin.floatValue());
        if (tr == null) {
            throw new InvalidTaskException(term, "insufficient confidence");
        }

        Task y = new ImmutableTask((Compound) term, punc, tr, time(), occurrenceTime, occurrenceTime, new long[]{time.nextStamp()});
        y.budget(pri, this);

        input(y);

        return y;
    }

    @NotNull
    public void ask(@NotNull Termed<Compound> term, char questionOrQuest) {
        ask(term, questionOrQuest, ETERNAL);
    }

    public Task ask(@NotNull Termed<Compound> term, char questionOrQuest, long when) {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        if ((questionOrQuest != QUESTION) && (questionOrQuest != QUEST))
            throw new RuntimeException("invalid punctuation");

        Task t = new ImmutableTask((Compound) term, (byte) questionOrQuest, null,
                time(), when, when, new long[]{time.nextStamp()}).budget(this);

        input(t);

        return t;

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

    public final void input(@NotNull Task... t) {
        for (Task x : t)
            input(x);
    }

    /**
     * exposes the memory to an input, derived, or immediate task.
     * the memory then delegates it to its controller
     * <p>
     * returns the Concept (non-null) if the task was processed
     * if the task was a command, it will return false even if executed
     */
    @Nullable
    public final Concept input(@NotNull Task input0) {

        Task input;
        try {
            input = pre(input0);
            if (input == null)
                return null;
        } catch (InvalidTermException e) {
            emotion.eror(input0.volume());
            return null;
        }

        float q = input.qua();
        if (q != q) { //default budget if qua == NaN
            input.budget(this);
        }

        float inputPri = input.priSafe(0);
        emotion.busy(inputPri, input.volume());


        if (input.isCommand() /* || (input.isGoal() && (input.isEternal() || ((input.start() - now) >= -dur)))*/) { //eternal, present (within duration radius), or future


            Task transformed = execute(input);
            if (transformed == null)
                return null;
            else if (transformed != input)
                return input(transformed);
            //else: continue


        }


        if (time instanceof FrameTime) {
            //HACK for unique serial number w/ frameclock
            ((FrameTime) time).validate(input.stamp());
        }

        try {

            Concept c = input.concept(this);
            if (c instanceof TaskConcept) {

                Activation a = ((TaskConcept) c).process(input, this);

                if (a != null) {

                    eventTaskProcess.emit(/*post*/(input));

                    emotion.learn(inputPri, input.volume());

                    concepts.commit(c);

                    return c; //SUCCESSFULLY PROCESSED
                }

            }

        } catch (Concept.InvalidConceptException | InvalidTermException | InvalidTaskException | Budget.BudgetException e) {

            emotion.eror(input.volume());

            //input.feedback(null, Float.NaN, Float.NaN, this);
            if (Param.DEBUG)
                logger.warn("task process: {} {}", e, input);
        }

        return null;
    }


    private boolean executable(Task input) {
        if (input.isCommand())
            return true;

        Concept c = input.concept(this);
        float be;
        if (c == null)
            be = 0;
        else {
            Truth b = c.belief(time(), time.dur());
            if (b == null)
                be = 0;
            else
                be = b.expectation();
        }

        return input.expectation() - be >= Param.EXECUTION_THRESHOLD;
    }

    private @Nullable Task execute(Task cmd) {


        Compound inputTerm = cmd.term();
        if (inputTerm.hasAll(Operator.OPERATOR_BITS) && inputTerm.op() == INH) {
            Term func = inputTerm.term(1);
            if (func.op() == ATOM) {
                Term args = inputTerm.term(0);
                if (args.op() == PROD) {
                    Concept funcConcept = concept(func);
                    if (funcConcept != null) {
                        Operator o = funcConcept.get(Operator.class);
                        if (o != null) {


                            /*if (isCommand)*/
                            {
                                Task result = o.run(cmd, this);
                                if (result != null && result != cmd) {
                                    //return input(result); //recurse
                                    return result;
                                }
                            }
//                            } else {
//
//                                if (!cmd.isEternal() && cmd.start() > time() + time.dur()) {
//                                    inputAt(cmd.start(), cmd); //schedule for execution later
//                                    return null;
//                                } else {
//                                    if (executable(cmd)) {
//                                        Task result = o.run(cmd, this);
//                                        if (result != cmd) { //instance equality, not actual equality in case it wants to change this
//                                            if (result == null) {
//                                                return null; //finished
//                                            } else {
//                                                //input(result); //recurse until its stable
//                                                return result;
//                                            }
//                                        }
//                                    }
//                                }
//                            }

                        }

                    }
                }
            }
        }

        /*if (isCommand)*/ {
            eventTaskProcess.emit(cmd);
            return null;
        }

    }

    /**
     * override to perform any preprocessing of a task (applied before the normalization step)
     */
    @Nullable
    public Task pre(@NotNull Task t) {
        return t;
    }

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

    public final static ThreadLocal<ObjectFloatHashMap<Termed>> acti = ThreadLocal.withInitial(() ->
        new ObjectFloatHashMap<>()
    );

    public Activation activateTask(@NotNull Task input, @NotNull Concept c, float scale) {
        //return new DepthFirstActivation(input, this, nar, nar.priorityFactor.floatValue());

        //float s = scale * (0.5f + 0.5f * pri(c, 1));
        return new SpreadingActivation(input, c, this, scale, acti.get());
    }

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


    @NotNull
    public void processAll(@NotNull Task... t) {
        for (Task x : t)
            input(x);
    }

    public final void on(@NotNull String atom, @NotNull Operator o) {
        on((Atom) $.the(atom), o);
    }

    public final void on(@NotNull Atom a, @NotNull Operator o) {
        DefaultConceptBuilder builder = (DefaultConceptBuilder) concepts.conceptBuilder();
        PermanentAtomConcept c = builder.withBags(a, (termlink, tasklink) -> {
            return new PermanentAtomConcept(a, termlink, tasklink);
        });
        c.put(Operator.class, o);
        concepts.set(c);
        operators.put(c, o);
    }

    public void input(Function<NAR, Task>... tasks) {
        for (Function<NAR, Task> x : tasks) {

            try {

                input(x.apply(this));

            } catch (@NotNull InvalidTaskException | InvalidTermException | Budget.BudgetException e) {

                if (x instanceof TaskBuilder) {

                    TaskBuilder tb = (TaskBuilder) x;

                    emotion.eror(tb.volume());

                    if (tb.isInput() || Param.DEBUG_EXTRA)
                        logger.warn("input: {}", e.toString());

                }

            }
        }
    }

    public final int dur() {
        return time.dur();
    }

    static class PermanentAtomConcept extends AtomConcept implements PermanentConcept {
        public PermanentAtomConcept(@NotNull Atomic atom, Bag<Term, BLink<Term>> termLinks, Bag<Task, BLink<Task>> taskLinks) {
            super(atom, termLinks, taskLinks);
        }
    }


    /**
     * Exits an iteration loop if running
     */
    public final void stop() {
        exe.stop();
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public final NAR cycle() {
        return run(1);
    }


    /**
     * Runs multiple frames, unless already running (then it return -1).
     *
     * @return total time in seconds elapsed in realtime
     */
    @NotNull
    public final NAR run(int frames) {

        for (; frames > 0; frames--) {

            time.cycle();

            emotion.cycle();

            exe.cycle(this);

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
                    .a(tv.qua() > 0.5f ?
                            Ansi.Attribute.INTENSITY_BOLD :
                            Ansi.Attribute.INTENSITY_FAINT)
                    .a(tv.pri() > 0.5f ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF)
                    .fg(Budget.budgetSummaryColor(tv))
                    .a(
                            tv.toString(this, true)
                    )
                    .reset()
                    .toString();
        }

        out.append(v.toString());

        out.append('\n');
    }

    /**
     * creates a new loop which begins paused
     */
    @NotNull
    public NARLoop loop() {
        return loop(-1);
    }

    @NotNull
    public NARLoop loop(float initialFPS) {
        if (initialFPS < 0)
            return loop((int) -1); //pause

        if (initialFPS == 0)
            return loop((int) 0); //infinite

        float millisecPerFrame = 1000.0f / initialFPS;
        return loop((int) millisecPerFrame);
    }

    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param initialFramePeriodMS in milliseconds
     */
    @NotNull
    private NARLoop loop(int initialFramePeriodMS) {

        synchronized (concepts) {
            if (this.loop != null) {
                throw new RuntimeException("Already running: " + this.loop);
            }

            return this.loop = new NARLoop(this, initialFramePeriodMS);
        }
    }


    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(@NotNull Runnable t) {
        exe.run(t);
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

    public final void runLater(@NotNull Consumer<NAR> t) {
        exe.run(t);
    }


    //    @Nullable
//    public Future runAsync(@NotNull Runnable t, int maxRunsPerFrame) {
//        final Semaphore s = new Semaphore(0);
//        onFrame(nn -> {
//            int a = s.availablePermits();
//            if (a < maxRunsPerFrame)
//                s.release(1); //maxRunsPerFrame-a);
//        });
//        return runAsync(() -> {
//            while (true) {
//                try {
//                    s.acquire();
//                    t.run();
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                    //...then try again
//                }
//            }
//        });
//    }

    @NotNull
    @Override
    public String toString() {
        return self() + ":" + getClass().getSimpleName();
    }


//    /**
//     * inputs the question and observes answer events for a solution
//     */
//    @NotNull
//    public NAR onAnswer(@NotNull Task questionOrQuest, @NotNull Consumer<Task> c) {
//        new AnswerReaction(this, questionOrQuest) {
//
//            @Override
//            public void onSolution(Task belief) {
//                c.accept(belief);
//            }
//
//        };
//        return this;
//    }

    @NotNull
    public NAR input(@NotNull String... ss) {
        for (String s : ss)
            input(s);
        return this;
    }


    @NotNull
    public NAR inputAt(long time, @NotNull String... tt) {
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
    public void inputAt(long when, @NotNull Task... x) {
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
                        eventCycleStart.disable((Consumer)this);
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
        forEachConcept(c -> {
            if (includeConceptBeliefs) c.beliefs().forEach(maxPerConcept, recip);
            if (includeConceptQuestions) c.questions().forEach(maxPerConcept, recip);
            if (includeConceptGoals) c.goals().forEach(maxPerConcept, recip);
            if (includeConceptQuests) c.quests().forEach(maxPerConcept, recip);
            if (includeTaskLinks)
                c.tasklinks().forEach(maxPerConcept, t -> recip.accept(t.get()));
        });

        return this;
    }


    @Nullable
    public final Concept concept(@NotNull Termed t) {
        return concept(t, false);
    }

    @Nullable
    public final Concept concept(@NotNull Termed tt, boolean createIfMissing) {
        if (tt instanceof Concept)
            return ((Concept) tt); //assumes the callee has the same instance as the instance this might return if given only a Term
        if (tt instanceof Variable)
            return null; //fast eliminate

        return concept(tt.term(), createIfMissing);
    }

    public final @Nullable Concept concept(@NotNull Term t, boolean createIfMissing) {

        //t = post(t);

        t = concepts.conceptualizable(t);
        if (t == null)
            return null;

        Concept c = concepts.concept(t, createIfMissing);
        if (c != null && createIfMissing && c.isDeleted()) {
            //try again
            concepts.remove(c.term());
            return concepts.concept(t, createIfMissing);
        }

        return c;
    }

    @Nullable
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        conceptsActive().forEach(n -> recip.accept(n.get()));
        return this;
    }

    @NotNull
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        concepts.forEach(x -> {
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
    @NotNull public final On onCycle(@NotNull Consumer<NAR> each) {
        return eventCycleStart.on(each);
    }

    /** avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected */
    @NotNull public final On onCycleWeak(@NotNull Consumer<NAR> each) {
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

    public void input(@NotNull Iterable<Task> tasks) {
        tasks.forEach(x -> {
            if (x != null)
                input(x);
        });
    }

    public final void input(@Nullable Stream<Task> taskStream) {
        if (taskStream != null) {
            taskStream.forEach(x -> {
                if (x != null)
                    input(x);
            });
        }
    }

    public final void input(@NotNull Stream<Task> taskStream, float priNormalized) {
        if (priNormalized < Param.BUDGET_EPSILON)
            return;

        List<Task> t = $.newArrayList(256);
        float priTotal = 0;
        Iterator<Task> xx = taskStream.iterator();
        while (xx.hasNext()) {
            Task x = xx.next();
            if (x == null)
                continue;

            float p = x.pri();
            if (p != p)
                continue; //ignore deleted

            t.add(x);
            priTotal += p;
        }
        if (t.isEmpty())
            return;

        if (priTotal < Param.BUDGET_EPSILON) {
            input(t); //just input all (having 0 priority) as-is
        } else {
            float scale = priNormalized / priTotal;
            for (int i = 0, tSize = t.size(); i < tSize; i++) {
                Task x = t.get(i);
                x.budget().priMult(scale);
                input(x);
            }
        }
    }


    @Override
    public final boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }


    @Override
    public void activate(Concept c, float priToAdd) {
        if (priToAdd > Param.BUDGET_EPSILON)
            control.activate(c, priToAdd);
    }

    @Override
    public float pri(@NotNull Termed termed) {
        return control.pri(termed);
    }

    public Iterable<PLink<Concept>> conceptsActive() {
        return control.conceptsActive();
    }


    @NotNull
    public On onTask(@NotNull Consumer<Task> o) {
        return eventTaskProcess.on(o);
    }

    public @NotNull NAR believe(@NotNull Termed<Compound> c, @NotNull Tense tense) {
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

        concepts.set(c);

        return c;
    }

    /**
     * registers a term rewrite functor
     */
    @NotNull
    public final Concept onTerm(@NotNull String termAtom, @NotNull Function<Term[], Term> f) {
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
            concepts.onStateChanged(c);
        }

    }

    public final long time() {
        return time.time();
    }


    public @NotNull NAR input(@NotNull File input) throws IOException {
        return input(new FileInputStream(input));
    }

    @NotNull
    public NAR output(@NotNull File f, boolean append, @NotNull Function<Task, Task> each) throws IOException {
        FileOutputStream ff = new FileOutputStream(f, append);
        output(ff, each);
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
    public NAR output(@NotNull OutputStream o, @NotNull Function<Task, Task> each) {

        //SnappyFramedOutputStream os = new SnappyFramedOutputStream(o);

        DataOutputStream oo = new DataOutputStream(o);

        MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

        forEachTask(_x -> {
            total.increment();
            Task x = post(_x);
            x = each.apply(x);
            if (x != null) {
                byte[] b = IO.taskToBytes(x);
                if (b != null) {
                    try {

                        //HACK temporary until this is debugged
                        Task xx = IO.taskFromBytes(b, concepts);
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
        return output(new FileOutputStream(o), f);
    }

    @NotNull
    public NAR output(@NotNull OutputStream o) throws IOException {
        return output(o, x -> x.isDeleted() ? null : x);
    }

    /**
     * byte codec input stream of tasks, to be input after decode
     * TODO use input(Stream<Task>..</Task>
     */
    @NotNull
    public NAR input(@NotNull InputStream i) throws IOException {

        //SnappyFramedInputStream i = new SnappyFramedInputStream(tasks, true);
        DataInputStream ii = new DataInputStream(i);

        int count = 0;

        while ((i.available() > 0) || (i.available() > 0) || (ii.available() > 0)) {
            Task t = IO.readTask(ii, concepts);
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

    public Control getControl() {
        return control;
    }

    public final On onReset(Consumer<NAR> o) {
        return eventReset.on(o);
    }


}
