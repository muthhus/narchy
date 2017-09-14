package nars;


import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import jcog.Util;
import jcog.data.MutableInteger;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.util.IterableThreadLocal;
import nars.Narsese.NarseseException;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.state.ConceptState;
import nars.control.*;
import nars.derive.PrediTerm;
import nars.exe.Exec;
import nars.index.term.TermContext;
import nars.index.term.TermIndex;
import nars.op.Operation;
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
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.Cycles;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.ShortCountsHistogram;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.$.$;
import static nars.Op.*;
import static nars.term.Functor.f;
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
    static final Set<String> logEvents = Sets.newHashSet("eventTask");
    static final String VERSION = "NARchy v?.?";

    @NotNull
    public final Exec exe;
    @NotNull
    protected final Random random;

    public final transient Topic<NAR> eventClear = new ListTopic<>();

    public final transient Topic<NAR> eventCycle = new ListTopic<>();

    /**
     * a task has been processed or re-processed (priority changed)
     */
    public final transient Topic<Task> eventTask = new ListTopic<>();

    /**
     * scoped to this NAR so it can be reset by it
     */
    final IterableThreadLocal<Derivation> derivation =
            new IterableThreadLocal<>(() -> new Derivation(this));
    //ThreadLocal.withInitial(()->new Derivation(this));


    @NotNull
    public Emotion emotion;

    @NotNull
    public final Time time;

    @NotNull
    public final TermIndex terms;


    @NotNull
    private final AtomicReference<Term> self;


    /**
     * maximum NAL level currently supported by this memory, for restricting it to activity below NAL8
     */
    int nal;

    protected final NARLoop loop = new NARLoop(this);

    private final PrediTerm<Derivation> deriver;


    //private final PrediTerm<Derivation> deriver;


    /**
     * creates a snapshot statistics object
     * TODO extract a Method Object holding the snapshot stats with the instances created below as its fields
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

            if (c instanceof BaseConcept) {
                BaseConcept tc = (BaseConcept) c;
                beliefs.accept(tc.beliefs().size());
                goals.accept(tc.goals().size());
                questions.accept(tc.questions().size());
                quests.accept(tc.quests().size());
            }

        });

        SortedMap<String, Object> x = new TreeMap();

        //x.put("time real", new Date());

        x.put("time", time());

        emotion.stat(x);

        //x.put("term index", terms.summary());

        x.put("concept count", terms.size());

        x.put("belief count", ((double) beliefs.getSum()));
        x.put("goal count", ((double) goals.getSum()));

        Util.decode(tasklinkCount, "tasklink count", 4, x::put);
        //x.put("tasklink usage", ((double) tasklinkCount.getTotalCount()) / tasklinksCap.getSum());
        x.put("tasklink count", ((double) tasklinkCount.getTotalCount()));
        Util.decode(termlinkCount, "termlink count", 4, x::put);
        //x.put("termlink usage", ((double) termlinkCount.getTotalCount()) / termlinksCap.getSum());
        x.put("termlink count", ((double) termlinkCount.getTotalCount()));

//        DoubleSummaryStatistics pos = new DoubleSummaryStatistics();
//        DoubleSummaryStatistics neg = new DoubleSummaryStatistics();
//        causes.forEach(c -> pos.accept(c.pos()));
//        causes.forEach(c -> neg.accept(c.neg()));
//        x.put("value count", pos.getCount());
//        x.put("value pos mean", pos.getAverage());
//        x.put("value pos min", pos.getMin());
//        x.put("value pos max", pos.getMax());
//        x.put("value neg mean", neg.getAverage());
//        x.put("value neg min", neg.getMin());
//        x.put("value neg max", neg.getMax());


        //x.put("volume mean", volume.);
//
//        x.put("termLinksCapacity", termlinksCap);
//        x.put("taskLinksUsed", tasklinksUsed);
//        x.put("taskLinksCapacity", tasklinksCap);

        //Util.toMap(policy, "concept state", x::put);

        //Util.toMap(rootOp, "concept op", x::put);

        Util.decode(volume, "concept volume", 4, x::put);

        //Util.toMap( clazz, "concept class", x::put);

        return x;
    }


    public NAR(@NotNull TermIndex terms, @NotNull Exec exe, @NotNull Time time, @NotNull Random rng, @NotNull ConceptBuilder conceptBuilder, Function<NAR, PrediTerm<Derivation>> deriver) {
        super(exe);

        this.random = rng;

        this.terms = terms;

        this.exe = exe;

        this.time = time;
        time.clear();

        this.deriver = deriver.apply(this);

        this.nal = 8;

        self = new AtomicReference<>(null);
        setSelf(Param.randomSelf());

        for (int i = 0; i < valueSummary.length; i++)
            valueSummary[i] = new RecycledSummaryStatistics();

        value[Cause.Purpose.Input.ordinal()] = -0.05f;
        value[Cause.Purpose.Process.ordinal()] = +0.05f;

        value[Cause.Purpose.Accurate.ordinal()] = +1f;
        value[Cause.Purpose.Inaccurate.ordinal()] = -1.0f;

        value[Cause.Purpose.Answer.ordinal()] = +1f;
        value[Cause.Purpose.Action.ordinal()] = +2f;


        //Cause.updates(this);

        this.emotion = new Emotion(this);

        if (terms.nar == null) { //dont reinitialize if already initialized, for sharing
            terms.start(this);
            Builtin.load(this);
        }

        exe.start(this);
    }


    public void setEmotion(Emotion emotion) {
        synchronized (self) { //lol
            this.emotion = emotion;
        }
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

            restart();

        }

    }

    /**
     * the clear event is a signal indicating that any active memory or processes
     * which would interfere with attention should be stopped and emptied.
     * <p>
     * this does not indicate the NAR has stopped or reset itself.
     */
    public void clear() {
        eventClear.emit(this);
    }

    /**
     * initialization and post-reset procedure
     */
    protected void restart() {


        time.clear();

        exe.start(this);

    }


    public void setSelf(String self) {
        setSelf(Atomic.the(self));
    }

    public void setSelf(Term self) {
        this.self.set(self);
    }

    @NotNull
    public Task inputAndGet(@NotNull String taskText) throws Narsese.NarseseException {
        return inputAndGet(Narsese.parse().task(taskText, this));
    }


    @NotNull
    public List<Task> input(@NotNull String text) throws NarseseException, InvalidTaskException {
        List<Task> l = Narsese.parse().tasks(text, this);
        input(l);
        return l;
    }

    @Override
    @NotNull
    public <T extends Term> T term(@NotNull String t) throws NarseseException {
        return $.$(t);
    }

    @NotNull
    public <T extends Term> T term(@NotNull byte[] code) {
        return (T) IO.termFromBytes(code);
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
    public Task question(@NotNull Term c) {
        return que(c, QUESTION);
    }

    public Task quest(@NotNull Term c) {
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
    public Task goal(@NotNull Term goalTerm, @NotNull Tense tense, float freq, float conf) {
        return goal(
                priDefault(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Term term, @NotNull Tense tense, float freq, float conf) {
        return believe(term, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Term term, @NotNull long when, float freq, float conf) {
        believe(priDefault(BELIEF), term, when, freq, conf);
        return this;
    }

    @NotNull
    public NAR believe(@NotNull Term term, @NotNull Tense tense, float freq) {
        return believe(term, tense, freq, confDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Term term, long when, float freq) {
        return believe(term, when, freq, confDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Term term, float freq, float conf) {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public Task goal(@NotNull Term term, float freq, float conf) {
        return goal(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf) {
        try {
            believe(priDefault(BELIEF), term(term), time(tense), freq, conf);
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
    public NAR believe(@NotNull String... tt) throws NarseseException {

        for (String b : tt)
            believe(b, true);

        return this;
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
    public NAR believe(@NotNull Term term) {
        return believe(term, true);
    }

    @NotNull
    public NAR believe(@NotNull Term term, boolean trueOrFalse) {
        return believe(term, trueOrFalse, confDefault(BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Term term) {
        return goal(term, true);
    }

    @NotNull
    public Task goal(@NotNull Term term, boolean trueOrFalse) {
        return goal(term, trueOrFalse, confDefault(BELIEF));
    }

    @NotNull
    public NAR believe(@NotNull Term term, boolean trueOrFalse, float conf) {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @NotNull
    public Task goal(@NotNull Term term, boolean trueOrFalse, float conf) {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri, @NotNull Term term, long occurrenceTime, float freq, float conf) throws InvalidTaskException {
        return input(pri, term, BELIEF, occurrenceTime, freq, conf);
    }


    @Nullable
    public Task goal(float pri, @NotNull Term goal, long when, float freq, float conf) throws InvalidTaskException {
        return input(pri, goal, GOAL, when, when, freq, conf);
    }

    @Nullable
    public Task goal(float pri, @NotNull Term goal, long start, long end, float freq, float conf) throws InvalidTaskException {
        return input(pri, goal, GOAL, start, end, freq, conf);
    }

    @Nullable
    public Task input(float pri, @NotNull Term term, byte punc, long occurrenceTime, float freq, float conf) throws InvalidTaskException {
        return input(pri, term, punc, occurrenceTime, occurrenceTime, freq, conf);
    }

    @Nullable
    public Task input(float pri, @NotNull Term term, byte punc, long start, long end, float freq, float conf) throws InvalidTaskException {


        ObjectBooleanPair<Term> b = Task.tryContent(term, punc, false);
        term = b.getOne();
        if (b.getTwo())
            freq = 1f - freq;

        DiscreteTruth tr = new DiscreteTruth(freq, conf, confMin.floatValue());

        Task y = new NALTask(term, punc, tr, time(), start, end, new long[]{time.nextStamp()});
        y.setPri(pri);

        input(y);

        return y;
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Term term, byte questionOrQuest) {
        return que(term, questionOrQuest, ETERNAL);
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Term term, byte punc, long when) {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        assert ((punc == QUESTION) || (punc == QUEST)); //throw new RuntimeException("invalid punctuation");

        return inputAndGet(
                new NALTask(term.unneg(), punc, null,
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
    @Override
    public final void input(@NotNull ITask... t) {
        for (ITask x : t)
            input(x);
    }

    public final void input(ITask x) {
        if (x == null) return;

        ITask y = emotion.onInput(x);
        if (y != null)
            exe.add(y);
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


//    protected void processDuplicate(@NotNull Task input, Task existing) {
//        if (existing != input) {
//
//            //different instance
//
//            float reactivation;
//            Budget e = existing.budget();
//            if (!existing.isDeleted()) {
//                float ep = e.priElseZero();
//                reactivation = Util.unitize((input.priElseZero() - ep) / ep);
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


    /**
     * asynchronously adds the service
     */
    public void on(@NotNull NARService s) {
        runLater(() -> add(s.term(), s));
    }

    @Deprecated
    public final void on(@NotNull String atom, @NotNull Operator o) {
        on((Atom) Atomic.the(atom), o);
    }

    @Deprecated
    public final void on(@NotNull Atom a, @NotNull Operator o) {

        on(new Operation(a, this) {

            @Override
            public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
                Term c = t.term();
                Atomic op = (Atomic) (c.sub(1));
                @NotNull Term[] args = ((Compound) (t.term().sub(0))).toArray();
                nar.runLater(() -> {
                    o.run(op, args, nar);
                });
                return t;
            }

        });
    }

    public final void on(@NotNull Operation c) {
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

    @Nullable
    public Truth truth(@Nullable Termed concept, byte punc, long when) {
        return truth(concept, punc, when, when);
    }

    /**
     * returns concept belief/goal truth evaluated at a given time
     */
    @Nullable
    public Truth truth(@Nullable Termed concept, byte punc, long start, long end) {
        if (concept != null) {

            assert (concept.op().conceptualizable) : "asking for truth of unconceptualizable: " + concept; //filter NEG etc

            @Nullable Concept c = concept(concept);
            if (c instanceof BaseConcept) {
                BaseConcept tc = (BaseConcept) c;
                BeliefTable table;
                switch (punc) {
                    case BELIEF:
                        table = tc.beliefs();
                        break;
                    case GOAL:
                        table = tc.goals();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                return table.truth(start, end, this);
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
    public Truth beliefTruth(Termed concept, long start, long end) {
        return truth(concept, BELIEF, start, end);
    }

    @Nullable
    public Truth goalTruth(Termed concept, long when) {
        return truth(concept, GOAL, when);
    }

    @Nullable
    public Truth goalTruth(Termed concept, long start, long end) {
        return truth(concept, GOAL, start, end);
    }


    /**
     * Exits an iteration loop if running
     */
    @Override
    public NAR stop() {

        loop.stop();

        //clear();
        super.stop();

        derivation.forEach(Derivation::reset);
        //derivation.forEach(c -> c.transformsCache.invalidateAll());

        exe.stop();

        return this;
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public final void cycle() {

        time.cycle(this);

        eventCycle.emit(this); //synchronous only

        Cause.update(causes, value, valueSummary);

        emotion.cycle();
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
                    .fg(Prioritized.budgetSummaryColor(tv))
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
        time.at(time(), t);
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

    public NAR inputNarsese(@NotNull URL url) throws IOException, NarseseException {
        return inputNarsese(url.openStream());
    }

    public NAR inputNarsese(@NotNull InputStream inputStream) throws IOException, NarseseException {
        String x = new String(inputStream.readAllBytes());
        input(x);
        return this;
    }

    /**
     * TODO this needs refactoring to use a central scheduler
     */
    @NotNull
    public NAR inputAt(long time, @NotNull String... tt) {

        assert (tt.length > 0);

        at(time, () -> {
            List<Task> yy = $.newArrayList(tt.length);
            for (String s : tt) {
                try {
                    yy.addAll(Narsese.parse().tasks(s, this));
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


    /**
     * schedule a task to be executed no sooner than a given NAR time
     */
    public final void at(long whenOrAfter, Runnable then) {

        if (whenOrAfter <= time())
            runLater(then);
        else
            time.at(whenOrAfter, then);
    }

    /**
     * tasks in concepts
     */
    @NotNull
    public Stream<Task> tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        return concepts().flatMap(c -> c.tasks(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests));
    }

    @NotNull
    public Stream<Task> tasks() {
        return tasks(true, true, true, true);
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
    public Concept concept(/*@NotNull */Termed x, boolean createIfMissing) {

        Term xt;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
            xt = ct.term();
        } else {

            if (x instanceof Variable /* fast test */ || !(xt = x.unneg()).op().conceptualizable)
                return null;
        }

        Term y = xt.conceptual();

        if (y instanceof Bool)
            return null; //throw new RuntimeException("failed to find conceptual root of " + x);


        return terms.concept(y, createIfMissing);
    }


    @Deprecated
    public NAR forEachProtoTask(@NotNull Consumer<ITask> recip) {
        exe.forEach(recip);
        return this;
    }

    public Stream<Activate> conceptActive() {
        return exe.stream().map(x -> x instanceof Activate ? (Activate) x : null).filter(Objects::nonNull);
    }

    @Deprecated
    public NAR forEachConceptActive(@NotNull Consumer<Activate> recip) {
        return forEachProtoTask(t -> {
            if (t instanceof Activate) {
                recip.accept(((Activate) t));
            }
        });
    }

    public Stream<Concept> concepts() {
        return terms.stream().filter(t -> t instanceof Concept).map(t -> (Concept) t);
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
    @Override
    @NotNull
    public final On onCycle(@NotNull Consumer<NAR> each) {
        return eventCycle.on(each);
    }

    /**
     * avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected
     */
    @NotNull
    public final On onCycleWeak(@NotNull Consumer<NAR> each) {
        return eventCycle.onWeak(each);
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
        return eventTask.on(o);
    }

    public @NotNull NAR believe(@NotNull Term c, @NotNull Tense tense) {
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

        //c.state(terms.conceptBuilder.awake());
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
    public final int nal() {
        return nal;
    }

    /**
     * sets current maximum allowed NAL level (1..8)
     */
    public final NAR nal(int newLevel) {
        nal = newLevel;
        return this;
    }


    public final long time() {
        return time.now();
    }


    public @NotNull NAR inputBinary(@NotNull File input) throws IOException {
        return inputBinary(new BufferedInputStream(new FileInputStream(input), 64 * 1024));
    }

    @NotNull
    public NAR outputBinary(@NotNull File f, boolean append) throws IOException {
        return outputBinary(f, append, t -> t);
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

        tasks().forEach(_x -> {

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
                        Task xx = IO.taskFromBytes(b);
                        if (xx == null || !xx.equals(x)) {
                            //this can happen if a subterm is decompressed only to discover that it contradicts another part of the compound it belongs within
                            logger.error("task serialization problem: {} != {}", _x, xx);
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

        logger.info("output {}/{} tasks ({} bytes)", wrote, total, oo.size());

        return this;
    }

    @NotNull
    public NAR output(@NotNull File o) throws FileNotFoundException {
        return output(new FileOutputStream(o));
    }

    @NotNull
    public NAR output(@NotNull File o, Function<Task, Task> f) throws FileNotFoundException {
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

        while (i.available() > 0 /*|| (i.available() > 0) || (ii.available() > 0)*/) {
            Task t = IO.readTask(ii);
            input(t);
            count++;
        }

        logger.info("input {} tasks from {}", count, i);

        ii.close();

        return this;
    }


    /**
     * The id/name of the reasoner
     */
    public final Term self() {
        return self.get();
    }

    @Override
    public final Termed get(Term x, boolean createIfAbsent) {
        return terms.get(x, createIfAbsent);
    }

    /**
     * strongest matching belief for the target time
     */
    public Task belief(Term c, long start, long end) {
        return answer(c, BELIEF, start, end);
    }

    public Task belief(Term c, long when) {
        return belief(c, when, when);
    }

    /**
     * strongest matching goal for the target time
     */
    public final Task goal(Term c, long start, long end) {
        return answer(c, GOAL, start, end);
    }

    public final Task goal(Term c, long when) {
        return goal(c, when, when);
    }

    public Task answer(Term c, byte punc, long when) {
        return answer(c, punc, when, when);
    }

    public Task answer(Term c, byte punc, long start, long end) {
        assert (punc == BELIEF || punc == GOAL);
        Concept concept = concept(c);
        if (!(concept instanceof BaseConcept))
            return null;

        //return ((BeliefTable) ((BaseConcept) concept).table(punc)).match(when,  c, false, this);
        return ((BeliefTable) ((BaseConcept) concept).table(punc)).answer(start, end, c, this);
    }


    public SortedMap<String, Object> stats(Appendable out) {

        SortedMap<String, Object> stat = stats();
        stat.forEach((k, v) -> {
            try {
                out.append(k.replace(" ", "/")).append('\t').append(v.toString()).append('\n');
            } catch (IOException e) {
            }
        });
        try {
            out.append('\n');
        } catch (IOException e) {
        }

        return stat;
    }

    /**
     * table of values influencing reasoner heuristics
     */
    public final FasterList<Cause> causes = new FasterList(256);
    public final float[] value = new float[Cause.Purpose.values().length];
    public final RecycledSummaryStatistics[] valueSummary = new RecycledSummaryStatistics[value.length];

    /**
     * default deriver
     */
    public Derivation derivation() {
        return derivation(deriver);
    }

    /**
     * another deriver
     */
    public Derivation derivation(PrediTerm<Derivation> deriver) {
        return derivation.get().cycle(deriver);
    }


    /**
     * deletes any task with a stamp containing the component
     */
    public void retract(long stampComponent) {
        tasks().filter(x -> Longs.contains(x.stamp(), stampComponent)).forEach(Task::delete);
    }

    /**
     * bins a range of values into N equal levels
     */
    public static class ChannelRange<X extends Priority> {
        public final float min, max;
        public final Cause[] levels;
        transient private final float range; //cache

        public ChannelRange(String id, int levels, Function<Object, CauseChannel<X>> src, float min, float max) {
            this.min = min;
            this.max = max;
            assert (max > min);
            this.range = max - min;
            assert (range > Prioritized.EPSILON);
            this.levels = Util.map(0, levels, (l) -> src.apply(id + l), Cause[]::new);
        }

        public Cause get(float value) {
            return levels[Util.bin(Util.unitize((value - min) / range), levels.length)];
        }
    }

//    public final ImplicitTaskCauses taskCauses = new ImplicitTaskCauses(this);
//
//    static class ImplicitTaskCauses {
//
//        public final Cause causeBelief, causeGoal, causeQuestion, causeQuest;
//        public final Cause causePast, causePresent, causeFuture, causeEternal;
//        //public final ChannelRange causeConf, causeFreq;
//        public final NAR nar;
//
//        ImplicitTaskCauses(NAR nar) {
//            this.nar = nar;
//            causeBelief = nar.newChannel(String.valueOf((char) BELIEF));
//            causeGoal = nar.newChannel(String.valueOf((char) GOAL));
//            causeQuestion = nar.newChannel(String.valueOf((char) QUESTION));
//            causeQuest = nar.newChannel(String.valueOf((char) QUEST));
//            causeEternal = nar.newChannel("Eternal");
//            causePast = nar.newChannel("Past");
//            causePresent = nar.newChannel("Present");
//            causeFuture = nar.newChannel("Future");
////            causeConf = new ChannelRange("Conf", 7 /* odd */, nar::newChannel, 0f, 1f);
////            causeFreq = new ChannelRange("Freq", 7 /* odd */, nar::newChannel, 0f, 1f);
//        }
//
//        /**
//         * classifies the implicit / self-evident causes a task
//         */
//        public short[] get(Task x) {
//            //short[] ii = ArrayPool.shorts().getExact(8);
//
//            short time;
//            if (x.isEternal())
//                time = causeEternal.id;
//            else {
//                long now = nar.time();
//                long then = x.nearestTimeTo(now);
//                if (Math.abs(now - then) <= nar.dur())
//                    time = causePresent.id;
//                else if (then > now)
//                    time = causeFuture.id;
//                else
//                    time = causePast.id;
//            }
//
//            short punc;
//            switch (x.punc()) {
//                case BELIEF:
//                    punc = causeBelief.id;
//                    break;
//                case GOAL:
//                    punc = causeGoal.id;
//                    break;
//                case QUESTION:
//                    punc = causeQuestion.id;
//                    break;
//                case QUEST:
//                    punc = causeQuest.id;
//                    break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
////            if (x.isBeliefOrGoal()) {
////                short freq = causeFreq.get(x.freq()).id;
////                short conf = causeConf.get(x.conf()).id;
////                return new short[]{time, punc, freq, conf};
////            } else {
//            return new short[]{time, punc};
////            }
//        }
//
//    }


    /**
     * estimate the value of a cause trace
     */
    protected float evaluate(Task x, short[] causes) {

        int numCauses = causes.length;
        if (numCauses == 0) return 0;

        float boost = 0;
        for (short c : causes) {
            Cause cause = this.causes.getSafe(c);
            if (cause == null) {
                logger.error("cause id={} missing", c);
                continue;
            }

            boost += cause.value();
        }


        return boost / numCauses;
    }

    public Cause newCause(Object x) {
        synchronized (causes) {
            short next = (short) (causes.size());
            Cause c = new Cause(next, x);
            causes.add(c);
            return c;
        }
    }

    public CauseChannel<Task> newChannel(Object x) {
        return newChannel(x, this::input);
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<Task> newCauseChannel(Object id) {

        synchronized (causes) {

            final short ci = (short) (causes.size());
            CauseChannel c = new CauseChannel<ITask>(ci, id, (x) -> {
                if (x instanceof NALTask) {
                    NALTask t = (NALTask) x;
                    int tcl = t.cause.length;
                    if (t.cause == null || tcl == 0) {
                        t.cause = new short[]{ci};
                    } else {
                        //concat
                        t.cause = Arrays.copyOf(t.cause, tcl + 1);
                        t.cause[tcl] = ci;
                    }
                }
                input(x);
            });
            causes.add(c);
            return c;
        }
    }

    public CauseChannel<Task> newChannel(Object x, Consumer<ITask> target) {
        synchronized (causes) {
            short next = (short) (causes.size());
            CauseChannel c = new CauseChannel(next, x, target);
            causes.add(c);
            return c;
        }
    }


}
