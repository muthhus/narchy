package nars;


import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.mix.PSink;
import nars.Narsese.NarseseException;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.control.Cause;
import nars.control.ConceptFire;
import nars.derive.Deriver;
import nars.derive.TrieDeriver;
import nars.derive.meta.op.Caused;
import nars.index.term.TermContext;
import nars.index.term.TermIndex;
import nars.nar.exe.Executioner;
import nars.op.Command;
import nars.op.Operator;
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
import nars.term.atom.Bool;
import nars.term.atom.IntAtom;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.Cycles;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.ShortCountsHistogram;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
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
public class NAR extends Param implements Consumer<ITask>, NARIn, NAROut, Cycles<NAR>, TermContext {

    public static final Logger logger = LoggerFactory.getLogger(NAR.class);
    static final Set<String> logEvents = Sets.newHashSet("eventTaskProcess", "eventAnswer", "eventExecute");
    static final String VERSION = "NARchy v?.?";

    public final Executioner exe;
    protected final @NotNull Random random;

    public final ConceptBuilder conceptBuilder;

    public final transient Topic<NAR> eventReset = new ArrayTopic<>();
    public final transient ArrayTopic<NAR> eventCycleStart = new ArrayTopic<>();
    public final transient Topic<Task> eventTaskProcess = new ArrayTopic<>();


    @NotNull
    public final transient Emotion emotion;
    @NotNull
    public final Time time;

    @NotNull
    public final TermIndex terms;


    @NotNull
    private Term self;


    /**
     * maximum NAL level currently supported by this memory, for restricting it to activity below NAL8
     */
    int level;

    protected final NARLoop loop = new NARLoop(this);

    private TrieDeriver deriver;

    /**
     * creates a snapshot statistics object
     */
    public synchronized SortedMap<String, Object> stats() {

        //Frequency complexity = new Frequency();
        Frequency clazz = new Frequency();
        Frequency policy = new Frequency();
        Frequency rootOp = new Frequency();

        ShortCountsHistogram volume = new ShortCountsHistogram(2);

        //AtomicInteger i = new AtomicInteger(0);

        LongSummaryStatistics beliefs = new LongSummaryStatistics();
        LongSummaryStatistics goals = new LongSummaryStatistics();
        LongSummaryStatistics questions = new LongSummaryStatistics();
        LongSummaryStatistics quests = new LongSummaryStatistics();

        Histogram termlinkCount = new Histogram(1);
        Histogram tasklinkCount = new Histogram(1);
//        LongSummaryStatistics termlinksCap = new LongSummaryStatistics();
//        LongSummaryStatistics tasklinksCap = new LongSummaryStatistics();


        forEachConcept(c -> {

            if ((c instanceof Functor))
                return;

            //complexity.addValue(c.complexity());
            volume.recordValue(c.volume());
            rootOp.addValue(c.op());
            clazz.addValue(c.getClass().toString());

            @Nullable ConceptState p = c.state();
            policy.addValue(p != null ? p.toString() : "null");

            //termlinksCap.accept(c.termlinks().capacity());
            termlinkCount.recordValue(c.termlinks().size());

            //tasklinksCap.accept(c.tasklinks().capacity());
            tasklinkCount.recordValue(c.tasklinks().size());

            beliefs.accept(c.beliefs().size());
            goals.accept(c.goals().size());
            questions.accept(c.questions().size());
            quests.accept(c.quests().size());

        });

        SortedMap<String, Object> x = new TreeMap();

        //x.put("time real", new Date());

        x.put("time", time());

        emotion.stat(x);

        //x.put("term index", terms.summary());

        x.put("concept count", terms.size());

        x.put("belief count", ((double) beliefs.getSum()));
        x.put("goal count", ((double) goals.getSum()));

        Util.toMap(tasklinkCount, "tasklink count", 4, x::put);
        //x.put("tasklink usage", ((double) tasklinkCount.getTotalCount()) / tasklinksCap.getSum());
        x.put("tasklink count", ((double) tasklinkCount.getTotalCount()));
        Util.toMap(termlinkCount, "termlink count", 4, x::put);
        //x.put("termlink usage", ((double) termlinkCount.getTotalCount()) / termlinksCap.getSum());
        x.put("termlink count", ((double) termlinkCount.getTotalCount()));

        DoubleSummaryStatistics values = new DoubleSummaryStatistics();
        causeValue.forEach(c -> values.accept(c.value()));
        x.put("values mean", values.getAverage());
        x.put("values min", values.getMin());
        x.put("values max", values.getMax());
        x.put("values count", values.getCount());


        //x.put("volume mean", volume.);
//
//        x.put("termLinksCapacity", termlinksCap);
//        x.put("taskLinksUsed", tasklinksUsed);
//        x.put("taskLinksCapacity", tasklinksCap);

        //Util.toMap(policy, "concept state", x::put);

        //Util.toMap(rootOp, "concept op", x::put);

        Util.toMap(volume, "concept volume", 4, x::put);

        //Util.toMap( clazz, "concept class", x::put);

        return x;
    }


    public NAR(@NotNull TermIndex terms, @NotNull Executioner exe, @NotNull Time time, @NotNull Random rng, @NotNull ConceptBuilder conceptBuilder) {

        this.random = rng;

        this.terms = terms;

        this.exe = exe;

        this.self = Param.randomSelf();

        this.time = time;

        (this.conceptBuilder = conceptBuilder).start(this);

        this.level = 8;

        time.clear();

        this.emotion = new Emotion(this);

        this.deriver = Deriver.DEFAULT;
        deriver.forEachCause((Caused x) -> {
            if (x.cause != null) // a re-used copy from rule permutes? TODO why?
                return;
            //assert(x.cause == null);
            x.cause = newCause(x);
        });

        if (terms.nar == null) //dont reinitialize if already initialized, for sharing
            terms.start(this);

        exe.start(this);

    }


    public PSink<ITask, ITask> newInputChannel(Object id) {

        Cause c = newCause(id);
        short ci = c.id;
        short[] cs = new short[]{ci};

        return new PSink<ITask, ITask>(id, this::input) {
            @Override
            public ITask apply(ITask x) {
                if (x instanceof NALTask) {
                    //assert (((NALTask) x.ref).cause.length == 0);
                    NALTask t = (NALTask) x;
                    if (t.cause == null)
                        t.cause = cs;
                    else
                        t.cause = ArrayUtils.add(t.cause, 0 /* prepend */, ci);
                }
                return x;
            }
        };
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


    public void setSelf(String self) {
        setSelf((Atom) Atomic.the(self));
    }

    public void setSelf(Term self) {
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

    @NotNull
    public <T extends Term> T term(@NotNull byte[] code) throws NarseseException {
        return (T) IO.termFromBytes(code, terms);
    }


    /**
     * gets a concept if it exists, or returns null if it does not
     */
    @Nullable
    public final Concept conceptualize(@NotNull String conceptTerm) throws NarseseException {
        return conceptualize(term(conceptTerm));
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
    public void question(@NotNull String termString) throws NarseseException {
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
    public Task input(float pri, @NotNull Compound term, byte punc, long occurrenceTime, float freq, float conf) throws InvalidTaskException {


        ObjectBooleanPair<Compound> b = Task.tryContent(term, punc, terms, false);
        term = b.getOne();
        if (b.getTwo())
            freq = 1f - freq;

        DiscreteTruth tr = new DiscreteTruth(freq, conf, confMin.floatValue());
        if (tr == null)
            throw new InvalidTaskException(term, "insufficient confidence");


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
                new NALTask((Compound) term.unneg(), punc, null,
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
    public final void input(@NotNull ITask... t) {
        for (ITask x : t)
            if (x != null) input(x);
    }

    public void input(ITask x) {
        if (x != null)
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
    public final void accept(@NotNull ITask task) {
        input(task);
    }


    @Deprecated
    public final void on(@NotNull String atom, @NotNull Operator o) {
        on((Atom) Atomic.the(atom), o);
    }

    @Deprecated
    public final void on(@NotNull Atom a, @NotNull Operator o) {

        on(new Command(a) {

            @Override
            public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
                Compound c = t.term();
                o.run((Atomic) (c.sub(1)), ((Compound) (t.term(0))).toArray(), nar);
                return t;
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
            if (c instanceof TaskConcept) {
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
                return table.truth(when, this);
            }
        }
        return null;
    }

    @Nullable
    public Truth beliefTruth(String concept, long when) throws NarseseException {
        return truth(conceptualize(concept), BELIEF, when);
    }

    @Nullable
    public Truth goalTruth(String concept, long when) throws NarseseException {
        return truth(conceptualize(concept), GOAL, when);
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
        loop.stop();
        exe.stop();
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public final void cycle() {

        time.cycle();

        valueUpdate();

        emotion.cycle();

        exe.cycle(this);

        if (!scheduled.isEmpty()) {
            LongObjectPair<Runnable> next;
            long now = time();
            while ((next = scheduled.peek()) != null) {
                if (next.getOne() <= now) {
                    scheduled.poll();
                    exe.runLater(next.getTwo());
                } else {
                    break; //wait till another time
                }
            }
        }

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
                    //.a(tv.originality() >= 0.33f ?
                    .a(tv.pri() >= 0.25f ?
                            Ansi.Attribute.INTENSITY_BOLD :
                            Ansi.Attribute.INTENSITY_FAINT)
                    .a(tv.pri() > 0.75f ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF)
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
        assert (initialFPS > 0);

        float millisecPerFrame = 1000.0f / initialFPS;
        return startPeriodMS((int) millisecPerFrame);
    }

    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param ms in milliseconds
     */
    @NotNull
    public NARLoop startPeriodMS(int ms) {
        loop.setPeriodMS(ms);
        return loop;
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

    /**
     * TODO this needs refactoring to use a central scheduler
     */
    @NotNull
    public NAR inputAt(long time, @NotNull String... tt)  {

        assert(tt.length>0);

        at(time, ()->{
            List<Task> yy = newArrayList(tt.length);
            for (String s : tt) {
                try {
                    yy.addAll(tasks(s));
                } catch (NarseseException e) {
                    logger.error("{} for: {}", e, s);
                    e.printStackTrace();
                }
            }


            int size = yy.size();
            if (size > 0)
                input(yy.toArray(new Task[size]));

        });

        return this;
    }

    /**
     * TODO use a scheduling using r-tree
     */
    public void inputAt(long when, @NotNull ITask... x) {
        long now = time();
        if (when <= now) {
            //past or current cycle
            input(x);
        } else {
            at(when, () -> input(x));
        }
    }

    final MinMaxPriorityQueue<LongObjectPair<Runnable>> scheduled =
            MinMaxPriorityQueue.orderedBy((LongObjectPair<Runnable> a, LongObjectPair<Runnable> b) -> {
                int c = Longs.compare(a.getOne(), b.getOne());
                if (c == 0)
                    return -1; //maintains uniqueness in case they occupy the same time
                else
                    return c;
            }).create();

    /**
     * schedule a task to be executed no sooner than a given NAR time
     */
    public void at(long when, Runnable then) {
        scheduled.add(PrimitiveTuples.pair(when, then));
    }

    @NotNull
    public NAR forEachConceptTask(@NotNull Consumer<Task> each) {
        return forEachConcept(c -> c.forEachTask(each));
    }

    @NotNull
    public NAR forEachConceptTask(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        return forEachConcept(c -> {
            c.forEachTask(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests, each);
        });
    }

    @NotNull
    public NAR forEachConceptTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests,
                                  boolean includeTaskLinks, int maxPerConcept,
                                  @NotNull Consumer<Task> recip) {
        Consumer<? super PriReference<Task>> action = t -> recip.accept(t.get());
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
    public final Concept conceptualize(@NotNull Termed termed) {
        return concept(termed, true);
    }

    @Nullable
    private Concept concept(@NotNull Termed x, boolean createIfMissing) {
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
        }

        Term y = conceptTerm(x.term());
        return (y == null) ? null : terms.concept(y, createIfMissing);
    }

    /**
     * returns the canonical Concept term for any given Term, or null if it is unconceptualizable
     */
    @Nullable
    public Term conceptTerm(@NotNull Term term) {

        if (term instanceof Compound) {

            //boolean wasNormalized = term.isNormalized();

            term = compoundOrNull(term.unneg());
            if (term == null) return null;

            term = compoundOrNull(terms.atemporalize((Compound) term));
            if (term == null) return null;

            //atemporalizing can reset normalization state of the result instance
            //since a manual normalization isnt invoked. until here, which depends if the original input was normalized:

            //if (wasNormalized) {
            term = compoundOrNull(terms.normalize((Compound) term));
            if (term == null) return null;
            //}

            term = compoundOrNull(term.unneg() /* once again to be sure */);
            if (term == null) return null;

        }

        if (term instanceof Variable || term instanceof IntAtom || term instanceof Bool)
            return null;

        return term;
    }


    public NAR forEachTaskActive(@NotNull Consumer<ITask> recip) {
        exe.forEach(recip);
        return this;
    }

    public NAR forEachConceptActive(@NotNull Consumer<ConceptFire> recip) {
        return forEachTaskActive(t -> {
            if (t instanceof ConceptFire) {
                recip.accept(((ConceptFire) t));
            }
        });
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

        c.state(conceptBuilder.awake());
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


    public final long time() {
        return time.now();
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

        forEachConceptTask(_x -> {

            if (_x.isDeleted()) return; //HACK forEachConcept should not return deleted tasks

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
    public final Term self() {
        return self;
    }

    @Override
    public final Termed get(Term x, boolean createIfAbsent) {
        return terms.get(x, createIfAbsent);
    }

    /**
     * strongest matching belief for the target time
     */
    public Task belief(Compound c, long when) {
        return match(c, BELIEF, when);
    }

    /**
     * strongest matching goal for the target time
     */
    public final Task goal(Compound c, long when) {
        return match(c, GOAL, when);
    }

    /**
     * punc must be either BELIEF or GOAL
     */
    public Task match(Compound c, byte punc, long when) {
        Concept concept = concept(c);
        if (concept == null)
            return null;

        return ((BeliefTable) concept.table(punc)).match(when, null, null, false, this);
    }

    public Deriver deriver() {
        return deriver;
    }

    public SortedMap<String, Object> stats(PrintStream out) {

        SortedMap<String, Object> stat = stats();
        stat.forEach((k, v) -> {
            System.out.println(k.replace(" ", "/") + "\t" + v);
        });
        System.out.println("\n");

        return stat;
    }

    /**
     * table of values influencing reasoner heuristics
     */
    public final FasterList<Cause> causeValue = new FasterList(512);

    /**
     * returns the sum of the current values (applied after adding the increment)
     *
     */
    public float value(short[] causes, float value) {

        float boost = 0;
        int xl = causes.length;

        //value *= activation; //weight the apparent value by its incoming activation?


        //normalize the sub-values to 1.0 using triangular number as a divisor
        float sum = 0.5f * xl * (xl + 1);

        for (int i = 0; i < xl; i++) {
            short c = causes[i];
            Cause cc = this.causeValue.get(c);
            if (cc == null)
                continue; //ignore, maybe some edge case where the cause hasnt been registered yet?
                    /*assert(cc!=null): c + " missing from: " + n.causes.size() + " causes";*/

            boost += cc.value();

            float vPer = (((float)(i+1))/sum) * value; //linear triangle increasing to inc, warning this does not integrate to 100% here
            if (vPer != 0) {
                cc.apply(vPer);
            }
        }

        return boost;
    }

    private Cause newCause(Object x) {
        synchronized (causeValue) {
            short next = (short) (causeValue.size());
            Cause c = new Cause(next, x);
            causeValue.add(c);
            return c;
        }
    }


    public final FloatParam valuePositiveDecay = new FloatParam(0.995f, 0, 1f);
    public final FloatParam valueNegativeDecay = new FloatParam(0.97f, 0, 1f);

    public void valueUpdate() {
        float p = valuePositiveDecay.floatValue();
        float n = valueNegativeDecay.floatValue();
        causeValue.forEach(c -> c.commit(p, n));
    }

}
