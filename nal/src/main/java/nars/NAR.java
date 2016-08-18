package nars;


import com.google.common.collect.Sets;
import nars.Narsese.NarseseException;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.concept.AbstractConcept;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.concept.table.BeliefTable;
import nars.index.TermIndex;
import nars.nal.Level;
import nars.nal.Tense;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.Execution;
import nars.nar.Executioner;
import nars.nar.SingleThreadExecutioner;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Operator;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.fusesource.jansi.Ansi;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Symbols.*;
import static nars.nal.Tense.ETERNAL;
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
 */
public abstract class NAR extends Memory implements Level, Consumer<Task> {


    /**
     * The information about the version and date of the project.
     */
    public static final String VERSION = "Open-NARS v1.7.0";
    /**
     * The project web sites.
     */
    public static final String WEBSITE =
            " Open-NARS website:  http://code.google.com/p/open-nars/ \n" +
                    "      NARS website:  http://sites.google.com/site/narswang/ \n" +
                    "    Github website:  http://github.com/opennars/ \n" +
                    "               IRC:  http://webchat.freenode.net/?channels=nars \n";


    public static final Logger logger = LoggerFactory.getLogger(NAR.class);


    static final Set<String> logEvents = Sets.newHashSet(
            "eventTaskProcess", "eventAnswer",
            "eventExecute" //"eventRevision", /* eventDerive */ "eventError",
            //"eventSpeak"
    );

    private final Executioner exe;


    /**
     * The id/name of the reasoner
     * TODO
     */
    @NotNull
    public Atom self;
    /**
     * Flag for running continuously
     */
    public volatile boolean running = false;


    private NARLoop loop;

    private final Collection<Object> on = $.newArrayList(); //registered handlers, for strong-linking them when using soft-index


    public NAR(@NotNull Clock clock, @NotNull TermIndex index, @NotNull Random rng, @NotNull Atom self) {
        this(clock, index, rng, self, new SingleThreadExecutioner());
    }

    public void printConceptStatistics() {
        Frequency complexity = new Frequency();
        Frequency volume = new Frequency();
        Frequency rootOp = new Frequency();
        AtomicInteger i = new AtomicInteger(0);

        LongSummaryStatistics termlinksCap = new LongSummaryStatistics();
        LongSummaryStatistics termlinksUsed = new LongSummaryStatistics();
        LongSummaryStatistics tasklinksCap = new LongSummaryStatistics();
        LongSummaryStatistics tasklinksUsed = new LongSummaryStatistics();

        forEachConcept(c -> {
            i.incrementAndGet();
            complexity.addValue(c.complexity());
            volume.addValue(c.volume());
            rootOp.addValue(c.op());

            termlinksCap.accept(c.termlinks().capacity());
            termlinksUsed.accept(c.termlinks().size());
            tasklinksCap.accept(c.tasklinks().capacity());
            tasklinksUsed.accept(c.tasklinks().size());
                    
        });
        System.out.println("Total Concepts:\n" + i.get());
        System.out.println("\ntermLinksUsed:\n" + termlinksUsed);
        System.out.println("\ntermLinksCapacity:\n" + termlinksCap);
        System.out.println("\ntaskLinksUsed:\n" + tasklinksUsed);
        System.out.println("\ntaskLinksCapacity:\n" + tasklinksCap);
        System.out.println("\nComplexity:\n" + complexity);
        System.out.println("\nrootOp:\n" + rootOp);
        System.out.println("\nvolume:\n" + volume);

    }


    @Nullable public final Compound normalize(Compound t) {
        return index.normalize(t);
    }

    public NAR(@NotNull Clock clock, @NotNull TermIndex index, @NotNull Random rng, @NotNull Atom self, Executioner exe) {
        super(clock, rng, index);


        this.self = self;


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



        index.conceptBuilder().start(this);

        index.loadBuiltins();
        index.start(this);

        tasks.start(this);

        (this.exe = exe).start(this);

    }


    @Deprecated
    public static void printTasks(@NotNull NAR n, boolean beliefsOrGoals) {
        printTasks(n, beliefsOrGoals, (t) -> {
            System.out.println(t.proof());
        });
    }

    @Deprecated
    public static void printTasks(@NotNull NAR n, boolean beliefsOrGoals, @NotNull Consumer<Task> e) {
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
                bt.add(table.top(n.time()));
                //System.out.println("\t" + c.beliefs().top(n.time()));
            }
        });
        bt.forEach(e);
    }

    /**
     * change the identity of this NAR
     */
    @NotNull
    public NAR setSelf(@NotNull Atom nextSelf) {
        this.self = nextSelf;
        return this;
    }

    @NotNull
    public NAR setSelf(@NotNull String nextSelf) {
        return setSelf((Atom) $.$(nextSelf));
    }

    /**
     * Reset the system with an empty memory and reset clock.  Event handlers
     * will remain attached but enabled plugins will have been deactivated and
     * reactivated, a signal for them to empty their state (if necessary).
     */
    @NotNull
    public synchronized void reset() {

        exe.synchronize();

        super.reset();

    }


    /**
     * inputs a task, only if the parsed text is valid; returns null if invalid
     */
    @Deprecated
    @NotNull
    public Task inputTask(@NotNull String taskText) {
        return inputTask(Narsese.the().task(taskText, this));
    }

    @Nullable
    public Task inputTask(@NotNull Task t) {
        input(t);
        return t;
    }

    /**
     * parses and forms a Task from a string but doesnt input it
     */
    @Nullable
    public Task task(@NotNull String taskText) throws NarseseException {
        Task task = Narsese.the().task(taskText, this);
        task.normalize(this);
        return task;
    }

    @NotNull
    public List<Task> tasks(@NotNull String parse) {
        return tasks(parse, (o) -> {
            logger.error("unparsed: {}", o);
        });
    }

    @NotNull
    public List<Task> tasks(@NotNull String parse, @NotNull Consumer<Object[]> unparsed) {
        List<Task> result = $.newArrayList(1);
        Narsese.the().tasks(parse, result, unparsed, this);
        return result;
    }


    @NotNull
    public List<Task> input(@NotNull String text) throws NarseseException {
        List<Task> lt = tasks(text);
        input(lt);
        return lt;
    }

    @NotNull
    public <T extends Term> Termed<T> term(@NotNull String t) throws NarseseException {
        Termed x = index.parse(t);
        if (x == null) {
            //if a NarseseException was not already thrown, this indicates that it parsed but the index failed to provide its output
            throw new NarseseException("Unindexed: " + t);
        }
        return (T) x;
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
        return goal((Termed) $.$(goalTermString), tense, freq, conf);
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
        believe(priorityDefault(BELIEF), term, time(tense), freq, conf);
        return this;
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq) {
        return believe(term, tense, freq, confidenceDefault(Symbols.BELIEF));
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq) {
        return goal(term, tense, freq, confidenceDefault(Symbols.GOAL));
    }


    @Nullable
    public Task believe(float priority, @NotNull Termed term, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        return believe(priority, durabilityDefault(BELIEF), term, time(tense), freq, conf);
    }

    @Nullable
    public Task believe(float priority, @NotNull Termed term, long when, float freq, float conf) throws NarseseException {
        return believe(priority, durabilityDefault(BELIEF), term, when, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed term, float freq, float conf) throws NarseseException {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public Task goal(@NotNull Termed term, float freq, float conf) {
        return goal(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        believe(priorityDefault(BELIEF), term(term), time(tense), freq, conf);
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
    public Task goal(@NotNull String termString) throws NarseseException {
        return goal((Termed) term(termString), true);
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
    public NAR believe(@NotNull Termed<Compound> term) throws NarseseException {
        return believe(term, true);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse) throws NarseseException {
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
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf) throws NarseseException {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @NotNull
    public Task goal(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf) {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri, float dur, @NotNull Termed<Compound> term, long occurrenceTime, float freq, float conf) throws NarseseException {
        return input(pri, dur, term, BELIEF, occurrenceTime, freq, conf);
    }


    @Nullable
    public Task goal(float pri, @NotNull Termed<Compound> goal, long when, float freq, float conf) {
        return input(pri, durabilityDefault(GOAL), goal, GOAL, when, freq, conf);
    }

    @Nullable
    public Task goal(float pri, @NotNull Termed<Compound> goal, @NotNull Tense tense, float freq, float conf) {
        return input(pri, durabilityDefault(GOAL), goal, GOAL, time(tense), freq, conf);
    }

    @Nullable
    public Task input(float pri, float dur, @NotNull Termed<Compound> term, char punc, long occurrenceTime, float freq, float conf) {

        if (term == null) {
            throw new RuntimeException("null term for task");
        }

        Task t = new MutableTask(term, punc, $.t(freq, conf))
                .budget(pri, dur)
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

        inputLater(t);

        return t;

        //ex: return new Answered(this, t);

    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    @NotNull
    public NAR logSummaryGT(@NotNull Appendable out, float summaryThreshold) {
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

    protected Task preprocess(Task input) {
        return input;
    }

    /**
     * exposes the memory to an input, derived, or immediate task.
     * the memory then delegates it to its controller
     * <p>
     * return true if the task was processed
     * if the task was a command, it will return false even if executed
     */
    @Nullable
    protected final Concept input(@NotNull Task input) {

        if (input.isDeleted()) {
            //throw new InvalidTaskException(input, "Deleted");
            emotion.errr();
            logger.warn("input deleted: {}", input);
            return null;
        }

        //TODO create: protected Concept NAR.process(input, c)  so it can just return or exception here
        try {
            input = preprocess(input);
            input.normalize(this); //accept into input buffer for eventual processing
        } catch (Exception e) {
            emotion.frustration(input.priIfFiniteElseZero());
            emotion.errr();

            input.delete();

            if (Param.DEBUG)
                logger.warn("input preprocess: {}", e.toString());
            //e.printStackTrace();
            //throw e;
            return null;
        }

        float conceptActivation = input.isInput() ? this.inputActivation.floatValue() : this.derivedActivation.floatValue();

        if (tasks.add(input)) {

            try {

                Concept c = input.concept(this);

                boolean entered = c.process(input, this);

                if (entered) {

                    if (clock instanceof FrameClock) {
                        //HACK for unique serial number w/ frameclock
                        ((FrameClock) clock).ensureStampSerialGreater(input.evidence());
                    }

                    /*
                    long numTasks = tasks.tasks.estimatedSize();
                    int concepts = index.size();
                    float per = numTasks/concepts;
                    System.out.println("tasks=" + numTasks + ", ~" + per + " per concept");
                    */

                    float p = input.pri();
                    if (p > 0) {
                        //propagate budget
                        //try {
                        Activation activation = new Activation(input, c, this, conceptActivation);

                        emotion.busy(p);
                        emotion.stress(activation.overflow);
                        //                } catch (Exception e) {
                        //                    emotion.errr();
                        //
                        //                    if (Param.DEBUG)
                        //                        logger.warn("activation error: {}", e.toString());
                        //
                        //                    inputted.delete();
                        //                    return c;
                        //                }
                    }


                    input.onConcept(c);

                    eventTaskProcess.emit(input); //signal any additional processes
                    //eventTaskProcess.emitAsync(inputted, concurrency, runWorker);

                    return c; //SUCCESSFULLY PROCESSED
                }


            } catch (Exception e) {

                tasks.remove(input);

                emotion.errr();

                if (Param.DEBUG)
                    logger.warn("process error: {}", e);
            }
        }


        emotion.frustration(input.priIfFiniteElseZero());
        //input.delete();

        return null;
    }

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


    public final On onExec(@NotNull AbstractOperator r) {
        r.init(this);
        return onExecution(r.operator(), r);
    }


    @NotNull
    public final On onExecution(@NotNull Operator op, @NotNull Consumer<OperationConcept> each) {
        On o = concept(op, true)
                .<Topic<OperationConcept>>meta(Execution.class,
                        (k, v) -> v != null ? v : new DefaultTopic<>())
                .on(each);
        this.on.add(o);
        return o;
    }


    /**
     * Exits an iteration loop if running
     */
    public void stop() {
//        if (!running) {
//            throw new RuntimeException("wasnt running");
//        }
        synchronized (exe) {
            exe.synchronize();
            exe.stop();
        }

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
    public final synchronized NAR run(int frames) {

        //AtomicBoolean r = this.running;
        //synchronized (r = this.running) {

        this.running = true;


        Clock clock = this.clock;

        for (; frames > 0; frames--) {


            clock.tick();
            emotion.frame();

            exe.next(this);

            exe.synchronize();

        }

        this.running = false;
        //if (!r.compareAndSet(true, false))
        //throw new RunStateException(false);
        //}

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
                    .a(tv.dur() > 0.5f ? Ansi.Attribute.UNDERLINE : Ansi.Attribute.UNDERLINE_OFF)
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
        if (initialFPS == 0)
            return loop((int) 0);
        float millisecPerFrame = 1000.0f / initialFPS;
        return loop((int) millisecPerFrame);
    }

    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param initialFramePeriodMS in milliseconds
     */
    @NotNull
    final NARLoop loop(int initialFramePeriodMS) {
//        //TODO use DescriptiveStatistics to track history of frametimes to slow down (to decrease speed rate away from desired) or speed up (to reach desired framerate).  current method is too nervous, it should use a rolling average

        if (this.loop != null) {
            throw new RuntimeException("Already running: " + this.loop);
        }

        return this.loop = new NARLoop(this, initialFramePeriodMS);
    }


    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(@NotNull Runnable t) {
        exe.execute(t);
    }

    public final void runLater(@NotNull Consumer<NAR> t) {
        //TODO lambda may be optimized slightly
        runLater(() -> t.accept(this));
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
        return getClass().getSimpleName() + '[' + toString() + ']';
    }


    @Nullable
    public Task ask(@NotNull String question, long occ, @NotNull Predicate<Task> eachAnswer) throws NarseseException {
        return ask(term(question), occ, eachAnswer);
    }

    @Nullable
    public Task ask(@NotNull Termed<Compound> term, long occ, @NotNull Predicate<Task> eachAnswer) {
        return ask(term, occ, Symbols.QUESTION, eachAnswer);
    }

    @Nullable
    public Task ask(@NotNull Termed<Compound> term, long occ, char punc /* question or quest */, @NotNull Predicate<Task> eachAnswer) {
        @NotNull MutableTask t;
        inputLater(t = new MutableTask(term, punc, null) {
            @Override
            public boolean onAnswered(Task answer) {
                return eachAnswer.test(answer);
            }
        }.occurr(occ));
        return t;
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
        for (String s : ss) input(s);
        return this;
    }


    @NotNull
    public NAR inputAt(long time, @NotNull String... tt) {
        //LongPredicate timeCondition = t -> t == time;

        List<Task> x = $.newArrayList(tt.length);
        for (String s : tt) {
            x.addAll(tasks(s));
        }

        //set the appropriate creation and occurrence times
        for (Task y : x) {
            MutableTask my = (MutableTask) y;
            my.setCreationTime(time);
            if (my.occurrence() != ETERNAL)
                my.occurr(time);
        }

        inputAt(time, x);
        return this;
    }

    public void inputAt(long time, @NotNull Collection<Task> x) {
        long now = time();
        if (time < now) {
            //past
            throw new RuntimeException("can not input at a past time");
        } else if (time == now) {
            //current cycle
            input(x);
        } else {
            //future

            onFrame(m -> {
                //if (timeCondition.test(m.time())) {
                if (m.time() == time) {
                    m.input(x);
                    //this.off.off();
                }
            });

        }
    }

    @NotNull
    public NAR forEachConceptTask(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        forEachConcept(c -> {
            c.visitTasks(each, includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests);
        });
        return this;
    }

    @NotNull
    public NAR forEachConceptTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests,
                                  boolean includeTaskLinks, int maxPerConcept,
                                  @NotNull Consumer<Task> recip) {
        forEachConcept(c -> {
            if (includeConceptBeliefs && c.hasBeliefs()) c.beliefs().top(maxPerConcept, recip);
            if (includeConceptQuestions && c.hasQuestions()) c.questions().top(maxPerConcept, recip);
            if (includeConceptGoals && c.hasBeliefs()) c.goals().top(maxPerConcept, recip);
            if (includeConceptQuests && c.hasQuests()) c.quests().top(maxPerConcept, recip);
            if (includeTaskLinks && null != c.tasklinks())
                c.tasklinks().forEach(maxPerConcept, t -> recip.accept(t.get()));
        });

        return this;
    }

    /**
     * clears active memory (but not memory indices)
     */
    abstract public void clear();

    @Nullable
    public final Concept concept(@NotNull Termed t) {
        return concept(t, false);
    }

    @Nullable
    public final Concept concept(@NotNull Termed tt, boolean createIfMissing) {
        if (tt instanceof Concept)
            return ((Concept) tt);
        return concept(tt.term(), createIfMissing);
    }

    public @Nullable Concept concept(@NotNull Term t, boolean createIfMissing) throws TermIndex.InvalidConceptException {
        return index.concept(t, createIfMissing);
    }

    @Nullable
    public abstract NAR forEachActiveConcept(@NotNull Consumer<Concept> recip);

    @NotNull
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        index.forEach(x -> {
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
        onFrame(n -> {
            if (stopCondition.getAsBoolean()) stop();
        });
        return this;
    }


    /**
     * a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame.
     */
    @NotNull
    public On onFrame(@NotNull Consumer<NAR> each) {
        On r;
        on.add(r = eventFrameStart.on(each));
        return r;
    }

    @NotNull
    public NAR eachFrame(@NotNull Consumer<NAR> each) {
        onFrame(each);
        return this;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void input(@NotNull Stream<Task> taskStream) {
        taskStream.forEach(this::input);
        //input(new TaskStream(taskStream));
    }


    @Override
    public boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    /**
     * gets a measure of the current priority of the concept
     */
    abstract public float conceptPriority(@NotNull Termed termed);

    public Termed[] terms(String... terms) {
        return Stream.of(terms).map(this::term).toArray(Termed[]::new);
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
    public final void on(@NotNull Concept c) {
        index.set(c);
        on.add(c);
    }

    /**
     * processes the input before the next frame has run
     */
    public final void inputLater(@NotNull Task... t) {
        if (t.length == 0)
            throw new RuntimeException("empty task array");

        exe.inputLater(t);

    }

//    private final class InputTasks implements Runnable {
//        public final Task[] t;
//
//        private InputTasks(Task[] t) {
//            this.t = t;
//        }
//
//        @Override
//        public void run() {
//            input(t);
//        }
//    }

    public final void inputLater(@NotNull Collection<Task> tt) {
        int s = tt.size();
        if (s > 0)
            inputLater(tt.toArray(new Task[s]));
    }

    /** will run this if the executioner is not completely saturated */
    public boolean runLaterMaybe(Runnable r) {
        return exe.executeMaybe(r);
    }


//    @Nullable
//    public Term eval(@NotNull String x) throws NarseseException {
//        return rt.eval((Termed)term(x));
//    }


//    public final void with(Object... values) {
//        with(values);
//        //return (X)this;
//    }

    public static final class InvalidTaskException extends RuntimeException {

        public final Termed task;


        public InvalidTaskException(Termed t, String message) {
            super(message);
            this.task = t;
            if (t instanceof Task)
                ((Task) t).delete(message);
        }

        @NotNull
        @Override
        public String getMessage() {
            return super.getMessage() + ": " +
                    ((task instanceof Task) ? ((Task) task).proof() : task.toString());
        }

    }

    public static class RunStateException extends RuntimeException {
        public RunStateException(boolean shouldRun) {
            super(shouldRun ? "NAR already running" : "NAR already stopped");
        }
    }

//    private abstract class StreamNARReaction extends NARReaction {
//
//        public StreamNARReaction(Class... signal) {
//            super(NAR.this, signal);
//        }
//
//    }
//

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

        SnappyFramedOutputStream os = new SnappyFramedOutputStream(o);

        DataOutputStream oo = new DataOutputStream(os);

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

        oo.flush();
        os.flush();

        return this;
    }


    @NotNull
    public NAR output(@NotNull OutputStream o) throws IOException {
        return output(o, x -> true);
    }

    /**
     * byte codec input stream of tasks, to be input after decode
     */
    @NotNull
    public NAR input(@NotNull InputStream tasks) throws IOException {

        SnappyFramedInputStream i = new SnappyFramedInputStream(tasks, true);
        DataInputStream ii = new DataInputStream(i);

        int count = 0;

        while ((tasks.available() > 0) || (i.available() > 0) || (ii.available() > 0)) {
            Task t = IO.readTask(ii, index);
            input(t);
            count++;
        }

        logger.info("Loaded {} tasks from {}", count, tasks);

        return this;
    }

//    /**
//     * activates a concept via a task. the task may already be present in the system,
//     * but it will be reinforced via peer tasklinks activation.
//     * (a normal duplicate task going through process() will not have this behavior.)
//     */
//    public final Activation activate(@NotNull Task t, float scale) /* throws InvalidConceptException.. */{
//        @Nullable Concept concept = concept(t, true);
//        if (concept == null)
//            throw new TermIndex.InvalidConceptException(t.term(), "task did not resolve to a concept");
//
//
//        Activation aa = new Activation(t, scale);
//        aa.run(this);
//        return aa;
//    }


    public static class Activation {
        public final Budgeted in;

        public Concept src;
        //final ObjectFloatHashMap<BLink<Task>> tasks = new ObjectFloatHashMap<>();
        //public final ObjectFloatHashMap<BLink<Term>> termlinks = new ObjectFloatHashMap<>();
        public final ObjectFloatHashMap<Concept> concepts = new ObjectFloatHashMap<>();
        public final MutableFloat overflow = new MutableFloat(0);

        public Activation(@NotNull Task in, @NotNull NAR n, float scale) {
            this((Budgeted) in, in.concept(n));
            link(n , scale);
        }

        public Activation(Budgeted in, Concept src) {

            this.in = in;
            this.src = src;

        }

        /** runs the task activation procedure */
        public Activation(Budgeted in, Concept c, NAR nar, float scale) {
            this(in, c);

            link(nar, scale);

        }

        protected final void link(NAR nar, float scale) {
            src.link(1f, null/* linkActivation */, Param.BUDGET_EPSILON, nar, this);

            activate(nar, scale); //values will already be scaled
        }

        public void linkTermLinks(Concept src, float scale, NAR nar) {
            src.termlinks().forEach(n -> {
                linkTerm(src, nar, scale, n.get());
            });
        }

        public void linkTerms(Concept src, Term[] tgt, float scale, float minScale, @NotNull NAR nar) {


            Concept asrc = src;
            if (asrc != null && asrc != this) {
                //link the src to this
                AbstractConcept.linkSub(src, asrc, scale, this, nar);
            } else {
                activate(src, scale); //activate self
            }

            int n = tgt.length;
            float tStrength = 1f / n;
            float subScale = scale * tStrength;

            if (subScale > minScale) { //TODO use a min bound to prevent the iteration ahead of time

                //then link this to terms
                for (int i = 0; i < n; i++) {
                    Term tt = tgt[i];

                    if (!tt.isNormalized()) {
                        continue; //HACK
                    }

                    //Link the peer termlink bidirectionally
                    linkTerm(src, nar, subScale, tt);
                }
            }


        }

        public void linkTerm(Concept src, @NotNull NAR nar, float subScale, Term tt) {
            Concept target = AbstractConcept.linkSub(src, tt, subScale, this, nar);

            if (target != null && in instanceof Task) {
                //insert recursive tasklink
                target.linkTask((Task) in, subScale);
            }
        }

        public void run(@NotNull NAR nar) {
            activate(nar, 1f);
        }

        public void activate(@NotNull NAR nar, float activation) {
            if (!concepts.isEmpty()) {
                float total = 1;
                //(float) concepts.sum();
                nar.activate(concepts, in, activation / total, overflow);
            }
        }

        public void activate(Concept targetConcept, float scale) {
            concepts.addToValue(targetConcept, scale);
        }

    }

    abstract protected void activate(ObjectFloatHashMap<Concept> concepts, Budgeted in, float activation, MutableFloat overflow);


//    public final void activate(@NotNull Task t) {
//        activate(t, 1f);
//    }

    public final Predicate<@NotNull Task> taskPast = (Task t) -> {
        long o = t.occurrence();
        return o != Tense.ETERNAL && o < time();
    };
    public final Predicate<@NotNull Task> taskFuture = (Task t) -> {
        long o = t.occurrence();
        return o != Tense.ETERNAL && o > time();
    };


}
