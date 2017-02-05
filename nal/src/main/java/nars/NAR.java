package nars;


import com.google.common.collect.Sets;
import jcog.Util;
import jcog.data.MutableInteger;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import nars.Narsese.NarseseException;
import nars.attention.Activation;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.index.task.MapTaskIndex;
import nars.index.task.TaskIndex;
import nars.index.term.TermIndex;
import nars.link.BLink;
import nars.op.Operator;
import nars.table.BeliefTable;
import nars.task.MutableTask;
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
import static nars.concept.CompoundConcept.DuplicateMerge;
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
    final Map<PermanentAtomConcept,Operator> operators = new ConcurrentHashMap();
    @NotNull public final transient Emotion emotion;
    @NotNull public final Time time;
    /**
     * holds known Term's and Concept's
     */
    @NotNull
    public final TermIndex concepts;

    @NotNull
    public final TaskIndex tasks;

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


    @Nullable
    public final Term normalize(@NotNull Compound t) {
        return concepts.normalize(t);
    }

    public NAR(@NotNull Time time, @NotNull TermIndex concepts, @NotNull Random rng, @NotNull Executioner exe) {


        this.random = rng;

        this.exe = exe;

        this.level = 8;

        this.time = time;

        this.concepts = concepts;

        this.tasks = newTaskIndex();

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

    protected TaskIndex newTaskIndex() {
        return new MapTaskIndex(exe.concurrent());
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

            tasks.clear();

            restart();

        }

    }

    public void clear() {
        runLater( () -> eventReset.emit(this));
    }

    /** initialization and post-reset procedure */
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



    public void setSelf(Atom self) {
        this.self = self;
    }

    @Deprecated
    public static void printActiveTasks(@NotNull NAR n, boolean beliefsOrGoals) {
        printActiveTasks(n, beliefsOrGoals, (t) -> {
            System.out.println(t.proof());
        });
    }

    @Deprecated
    public static void printActiveTasks(@NotNull NAR n, boolean beliefsOrGoals, @NotNull Consumer<Task> e) {
        TreeSet<Task> bt = new TreeSet<>((a, b) ->
                //sort by name
                //{ return a.term().toString().compareTo(b.term().toString()); }
                //sort by confidence (descending)
        {
            int i = Float.compare(b.conf(), a.conf());
            if (i == 0 && a != b) {
                return b.compareTo(a); //equal conf but different task
            }
            return i;
        }
        );
        n.forEachActiveConcept(c -> {
            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();

            if (!table.isEmpty()) {
                bt.add(table.match(n.time()));
                //System.out.println("\t" + c.beliefs().top(n.time()));
            }
        });
        bt.forEach(e);
    }


    /**
     * parses and forms a Task from a string but doesnt input it
     */
    @NotNull
    public Task task(@NotNull String taskText) throws NarseseException {
        Task task = Narsese.the().task(taskText, this);
        //task.normalize(this);
        return task;
    }

    @NotNull public List<Task> tasks(@NotNull String parse) {
        List<Task> result = newArrayList(1);
        List<NarseseException> exc = newArrayList(0);
        Narsese.the().tasks(parse, result, exc, this);

        if (!exc.isEmpty())
            exc.forEach(e -> logger.error("parse error: {}", e));

        return result;
    }


    @NotNull
    public List<Task> input(@NotNull String text) {
        List<Task> lt = tasks(text);
        lt.forEach(this::input);
        return lt;
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
    public Task ask(@NotNull String termString) throws NarseseException {
        //TODO remove '?' if it is attached at end
        return ask(term(termString));
    }

    /**
     * ask question
     */
    @NotNull
    public Task ask(@NotNull Termed<Compound> c) {
        //TODO remove '?' if it is attached at end
        return ask(c, QUESTION);
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
    public Task believe(float priority, @NotNull Termed term, @NotNull Tense tense, float freq, float conf)  {
        return believe(priority, term, time(tense), freq, conf);
    }


    @NotNull
    public NAR believe(@NotNull Termed term, float freq, float conf)  {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public Task goal(@NotNull Termed term, float freq, float conf) {
        return goal(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf)  {
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
    public Task goal(@NotNull String termString)  {
        try {
            return goal((Termed) term(termString), true);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public NAR believe(@NotNull String termString) throws NarseseException {
        return believe(termString, true);
    }

    @NotNull
    public NAR believe(@NotNull String termString, boolean isTrue)  {
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
    public NAR believe(@NotNull Termed<Compound> term)  {
        return believe(term, true);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse)  {
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
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf)  {
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
    public Task input(float pri, Termed<Compound> term, char punc, long occurrenceTime, float freq, float conf) {

        if (term == null) {
            throw new NullPointerException("null task term");
        }

        Truth tr = t(freq, conf, confMin.floatValue());
        if (tr == null) {
            throw new InvalidTaskException(term, "insufficient confidence");
        }

        Task t = new MutableTask(term, punc, tr)
                .budgetByTruth(pri)
                .time(time(), occurrenceTime);

        inputLater(t);

        return t;
    }

    @NotNull
    public Task ask(@NotNull Termed<Compound> term, char questionOrQuest) {
        return ask(term, questionOrQuest, ETERNAL);
    }

    @NotNull
    public Task ask(@NotNull Termed<Compound> term, char questionOrQuest, long when) {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        if ((questionOrQuest != QUESTION) && (questionOrQuest != QUEST))
            throw new RuntimeException("invalid punctuation");

        MutableTask t = new MutableTask(term, questionOrQuest, null);
        t.time(time(), when);
        t.setPriority(priorityDefault(questionOrQuest));

        input(t);

        return t;

        //ex: return new Answered(this, t);

    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    @NotNull
    public NAR logBudgetMin(@NotNull Appendable out, float summaryThreshold) {
        return log(out, v -> {
            Budgeted b = null;
            if (v instanceof Budgeted) {
                b = ((Budgeted) v);
            } else if (v instanceof Twin) {
                if (((Twin) v).getOne() instanceof Budgeted) {
                    b = (Budgeted) ((Twin) v).getOne();
                }
            }
            return b != null && b.pri() > summaryThreshold;
        });
    }


    /**
     * exposes the memory to an input, derived, or immediate task.
     * the memory then delegates it to its controller
     * <p>
     * returns the Concept (non-null) if the task was processed
     * if the task was a command, it will return false even if executed
     */
    @Nullable
    public final Concept input(@NotNull Task input) {

        try {
            input.normalize(this); //accept into input buffer for eventual processing
        } catch (@NotNull InvalidTaskException | InvalidTermException | Budget.BudgetException e) {

            emotion.eror();

            input.delete();

            if (input.isInput() || Param.DEBUG_EXTRA)
                logger.warn("input: {}", e.toString());

            //e.printStackTrace();
            //throw e;
            return null;
        }

        boolean isCommand = input.isCommand();
        if (isCommand) {
            eventTaskProcess.emit(input);
        }

        Compound inputTerm = input.term();
        if (inputTerm.hasAll(Operator.OPERATOR_BITS) && inputTerm.op() == INH) {
            Term func = inputTerm.term(1);
            if (func.op() == ATOM) {
                Term args = inputTerm.term(0);
                if (args.op() == PROD) {
                    Concept funcConcept = concept(func);
                    if (funcConcept!=null) {
                        Operator o = funcConcept.get(Operator.class);
                        if (o!=null) {
                            Task result = o.run(input, this);

                            if (isCommand) {
                                if (result != null && result != input)
                                    return input(result); //recurse
                            } else {
                                if (result != input) { //instance equality, not actual equality in case it wants to change this
                                    if (result == null) {
                                        return null; //finished
                                    } else {
                                        return input(result); //recurse until its stable
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        if (isCommand) {
            return null;
        }

        emotion.busy(input.priSafe(0));

        Task existing = tasks.addIfAbsent(input);

        if (existing == null) {

            if (time instanceof FrameTime) {
                //HACK for unique serial number w/ frameclock
                ((FrameTime) time).validate(input.evidence());
            }

            try {

                Concept c = input.concept(this);

                Activation a = process(input, c);
                if (a != null) {

                    eventTaskProcess.emit(input); //signal any additional processes

                    emotion.learn(input.priSafe(0));

                    concepts.commit(c);

                    return c; //SUCCESSFULLY PROCESSED

                } else {

                    return null;

                }


            } catch (Concept.InvalidConceptException | InvalidTermException | InvalidTaskException | Budget.BudgetException e) {

                tasks.remove(input);

                emotion.eror();

                //input.feedback(null, Float.NaN, Float.NaN, this);
                if (Param.DEBUG)
                    logger.warn("task process: {} {}", e, input);
            }
        } else {

            if (existing != input) {

                //different instance

                float reactivation;
                Budget e = existing.budget();
                if (!existing.isDeleted()) {
                    float ep = e.priSafe(0);
                    reactivation = Util.unitize((input.priSafe(0) - ep) / ep);
                    if (reactivation > 0) {
                        DuplicateMerge.merge(e, input, 1f);
                    }
                    input.feedback(null, Float.NaN, Float.NaN, this);
                } else {
                    //this may never get called due to the replacement above
                    //attempt to revive deleted task
                    e.set(input.budget());
                    reactivation = 1f;
                }

                input.delete("Duplicate");

                //re-activate only
                if (reactivation > 0) {
                    Concept c = existing.concept(this);
                    if (c != null) {
                        ((CompoundConcept) c).activateTask(existing, this, reactivation);
                    }
                }

            }

        }

        return null;
    }

    protected Activation process(@NotNull Task t, Concept c) {
        return c.process(t, this);
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
        inputLater(task);
    }


    public @NotNull Collection<Task> input(@NotNull Collection<Task> t) {
        //TaskQueue tq = new TaskQueue(t);
        //input((Input) tq);
        //return tq;
        t.forEach(this::input);
        return t;
    }

    @NotNull
    public void input(@NotNull Task... t) {
        for (Task x : t)
            input(x);
    }

    public final void on(@NotNull String atom, @NotNull Operator o) {
        on((Atom)$.the(atom), o);
    }

    public final void on(@NotNull Atom a, @NotNull Operator o) {
        enableOperator(a, o);
    }

    static class PermanentAtomConcept extends AtomConcept implements PermanentConcept {
        public PermanentAtomConcept(@NotNull Atomic atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
            super(atom, termLinks, taskLinks);
        }
    }

    /** resets any existing atom or operator */
    private void enableOperator(@NotNull Atom a, @NotNull Operator o) {
        DefaultConceptBuilder builder = (DefaultConceptBuilder) concepts.conceptBuilder();
        Map map = builder.newBagMap(1);
        PermanentAtomConcept c = new PermanentAtomConcept(a, builder.newBag(map), builder.newBag(map));
        c.put(Operator.class, o);
        concepts.set(c);
        operators.put(c, o);
    }


    /**
     * Exits an iteration loop if running
     */
    public void stop() {
        exe.stop();
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public final NAR next() {
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

            time.tick();

            exe.next(this);

            emotion.frame();

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
                        if (x!=null)
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

        List<Task> x = newArrayList(tt.length);
        for (String s : tt) {
            x.addAll(tasks(s));
        }

        //set the appropriate creation and occurrence times
        for (Task y : x) {
            MutableTask my = (MutableTask) y;
            my.setCreationTime(time);
            if (my.start() != ETERNAL)
                my.occurr(time);
        }

        inputAt(time, x);
        return this;
    }

    public void inputAt(long when, @NotNull Collection<Task> x) {
        long now = time();
        if (when < now) {
            //past
            throw new RuntimeException("can not input at a past time");
        } else if (when == now) {
            //current cycle
            input(x);
        } else {
            //future

            onCycle(m -> {
                //if (timeCondition.test(m.time())) {
                if (m.time() == when) {
                    m.input(x);
                    //this.off.off();
                }
            });

        }
    }

    @NotNull
    public NAR forEachConceptTask(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        forEachConcept(c -> {
            c.forEachTask(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests, each);
        });
        return this;
    }

    @NotNull
    public NAR forEachConceptTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests,
                                  boolean includeTaskLinks, int maxPerConcept,
                                  @NotNull Consumer<Task> recip) {
        forEachConcept(c -> {
            if (includeConceptBeliefs) c.beliefs().top(maxPerConcept, recip);
            if (includeConceptQuestions) c.questions().top(maxPerConcept, recip);
            if (includeConceptGoals) c.goals().top(maxPerConcept, recip);
            if (includeConceptQuests) c.quests().top(maxPerConcept, recip);
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
        conceptsActive().forEach(n->recip.accept(n.get()));
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
            if (stopCondition.getAsBoolean()) stop();
        });
        return this;
    }


    /**
     * a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame.
     */
    @NotNull
    public final On onCycle(@NotNull Consumer<NAR> each) {
        On r;
        /*on.add(*/
        r = eventCycleStart.on(each);//);
        return r;
    }

    @NotNull
    public final On onCycle(@NotNull Runnable each) {
        return onCycle((ignored) -> {
            each.run();
        });
    }

    @NotNull
    @Deprecated public NAR eachCycle(@NotNull Consumer<NAR> each) {
        onCycle(each);
        return this;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void inputLater(@NotNull List<Task> tasks) {
        inputLater(tasks, 1);
    }

    public void inputLater(@NotNull List<Task> tasks, int maxChunkSize) {
        runLater(tasks, this::input, maxChunkSize);
    }

    public void inputLater(@NotNull Stream<Task> taskStream) {
        taskStream.forEach(this::inputLater);
        //input(new TaskStream(taskStream));
    }


    @Override
    public boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    @Override public void activate(Termed term, float priToAdd) {
        control.activate(term, priToAdd);
    }

    @Override public float pri(@NotNull Termed termed) {
        return control.pri(termed);
    }

    public Iterable<BLink<Concept>> conceptsActive() {
        return control.conceptsActive();
    }



    /**
     * text output
     */
    public void outputTasks(@NotNull Predicate<Task> filter, @NotNull PrintStream out) {
        forEachConceptTask(c -> {
            if (filter.test(c))
                out.println(c.term().toString() + c.punc() + " " + c.truth()); //TODO occurence time
        }, true, true, true, true);
    }


//    public void dumpConcepts(@NotNull String path) throws FileNotFoundException {
//        PrintStream pw = new PrintStream(new FileOutputStream(new File(path)));
//        index.forEach(t -> {
//            if (t instanceof Concept) {
//                Concept cc = (Concept)t;
//                cc.print(pw);
//            } else {
//                pw.append(t.toString());
//            }
//        });
//        pw.close();
//    }

    @NotNull
    public On onTask(@NotNull Consumer<Task> o) {
        return eventTaskProcess.on(o);
    }

    public @NotNull NAR believe(@NotNull Termed<Compound> c, @NotNull Tense tense) {
        return believe(c, tense, 1f);
    }

    /**
     * installs a concept in the index and activates it, used for setup of custom concept implementations
     * implementations should apply active concept capacity policy
     */
    @NotNull
    public final Concept on(@NotNull Concept c) {

        Concept existing = concept(c.term());
        if ((existing !=null) && (existing!=c))
            throw new RuntimeException("concept already indexed for term: " + c.term());

        concepts.set(c);

        return c;
    }

    @NotNull
    public final Concept on(@NotNull String termAtom, @NotNull Function<Term[],Term> f) {
        return on(f(termAtom, f));
    }

    /**
     * processes the input before the next frame has run
     */
    public final void inputLater(@NotNull Task... t) {
        if (t.length == 0)
            throw new RuntimeException("empty task array");

        exe.run(t);

    }

    public final void inputLater(@NotNull Collection<Task> tt) {
        int s = tt.size();
        if (s > 0)
            inputLater(tt.toArray(new Task[s]));
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

        if (c.state(p, this)!=p) {
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
    public NAR output(@NotNull File f, boolean append, @NotNull Predicate<Task> each) throws IOException {
        FileOutputStream ff = new FileOutputStream(f, append);
        output(ff, each);
        ff.close();
        return this;
    }

    /**
     * byte codec output of matching concept tasks (blocking)
     */
    @NotNull
    public NAR output(@NotNull OutputStream o, @NotNull Predicate<Task> each) throws IOException {

        //SnappyFramedOutputStream os = new SnappyFramedOutputStream(o);


        DataOutputStream oo = new DataOutputStream(o);

        MutableInteger total = new MutableInteger(0), filtered = new MutableInteger(0);

        forEachConceptTask(t -> {
            total.increment();
            if (each.test(t)) {
                try {
                    IO.writeTask(oo, t);
                    filtered.increment();
                } catch (IOException e) {
                    logger.error("{} when trying to output to {}", t, e);
                    throw new RuntimeException(e);
                    //e.printStackTrace();
                }
            }
        }, true, true, true, true);

        logger.info("Saved {}/{} tasks ({} bytes)", filtered, total, oo.size());

        oo.close();

        return this;
    }


    @NotNull
    public NAR output(@NotNull OutputStream o) throws IOException {
        return output(o, x -> !x.isDeleted());
    }

    /**
     * byte codec input stream of tasks, to be input after decode
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

    public On onReset(Consumer<NAR> o) {
        return eventReset.on(o);
    }


}
