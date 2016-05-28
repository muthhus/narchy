package nars;


import com.google.common.collect.Sets;
import com.gs.collections.api.tuple.Twin;
import nars.Narsese.NarseseException;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.concept.table.BeliefTable;
import nars.nal.Level;
import nars.nal.Tense;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.Execution;
import nars.op.in.FileInput;
import nars.op.in.TextInput;
import nars.task.MutableTask;
import nars.task.Task;
import nars.task.flow.Input;
import nars.task.flow.TaskQueue;
import nars.task.flow.TaskStream;
import nars.term.*;
import nars.term.atom.Atom;
import nars.time.Clock;
import nars.truth.DefaultTruth;
import nars.util.TermCodec;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import net.openhft.affinity.AffinityLock;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final ExecutorService asyncs = //shared
            (ThreadPoolExecutor) Executors.newCachedThreadPool();

    static final Set<String> logEvents = Sets.newHashSet(
            "eventTaskProcess", "eventAnswer",
            "eventExecute", //"eventRevision", /* eventDerive */ "eventError",
            "eventSpeak"

    );
    /**
     * The id/name of the reasoner
     * TODO
     */
    @NotNull
    public final Atom self;
    /**
     * Flag for running continuously
     */
    public final AtomicBoolean running = new AtomicBoolean();

    //final Narjure rt = new Narjure();

    //TODO use this to store all handler registrations, and decide if transient or not
    public final transient List<Object> regs = Global.newArrayList();

    private final transient List<Runnable> nextTasks = new CopyOnWriteArrayList(); //ConcurrentLinkedDeque();



    private NARLoop loop;

    public NAR(@NotNull Clock clock, TermIndex index, @NotNull Random rng, @NotNull Atom self) {
        super(clock, rng, index);

        the(NAR.class, this);

        this.self = self;

        /** register some components in the dependency context, Container (which Memory subclasses from) */
        the("clock", clock);


        eventError.on(e -> {
            if (e instanceof Throwable) {
                Throwable ex = (Throwable) e;

                //TODO move this to a specific impl of error reaction:
                ex.printStackTrace();

                if (Global.DEBUG && Global.EXIT_ON_EXCEPTION) {
                    //throw the exception to the next lower stack catcher, or cause program exit if none exists
                    throw new RuntimeException(ex);
                }
            } else {
                logger.error(e.toString());
            }
        });

    }

    @Deprecated public static void printTasks(NAR n, boolean beliefsOrGoals) {
        TreeSet<Task> bt = new TreeSet<>((a, b) -> { return a.term().toString().compareTo(b.term().toString()); });
        n.forEachConcept(c -> {
            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();

            if (!table.isEmpty()) {
                bt.add(table.top(n.time()));
                //System.out.println("\t" + c.beliefs().top(n.time()));
            }
        });
        bt.forEach(xt -> {
            System.out.println(xt);
        });
    }

    private void cycles(@NotNull Topic<Memory> cycleStart, int cyclesPerFrame) {
        for (; cyclesPerFrame > 0; cyclesPerFrame--) {
            cycleStart.emit(this);
        }
    }

    /**
     * Reset the system with an empty memory and reset clock.  Event handlers
     * will remain attached but enabled plugins will have been deactivated and
     * reactivated, a signal for them to empty their state (if necessary).
     */
    @NotNull
    public synchronized NAR reset() {

        nextTasks.clear();

        if (asyncs!=null)
            asyncs.shutdown();

        clear();

        return this;
    }

    @NotNull
    public FileInput input(@NotNull File input) throws IOException {
        FileInput fi = new FileInput(this, input);
        input((Input) fi);
        return fi;
    }

    /**
     * inputs a task, only if the parsed text is valid; returns null if invalid
     */
    @Nullable
    public Task inputTask(@NotNull String taskText) {
        return inputTask( task(taskText) );
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
        return Narsese.the().task(taskText, this).normalize(this);
    }

    @NotNull
    public List<Task> tasks(@NotNull String parse) throws NarseseException {
        List<Task> result = Global.newArrayList(1);
        Narsese.the().tasks(parse, result, this);
        result.forEach(c->c.normalize(this));
        return result;
    }

    @NotNull
    public TaskQueue inputs(@NotNull String parse) {
        return input(tasks(parse));
    }

    @NotNull
    public TextInput input(@NotNull String text) throws NarseseException {
        return (TextInput) input((Input)new TextInput(this, text));
    }

    @NotNull
    public <T extends Term> Termed<T> term(@NotNull String t) throws NarseseException {
        Termed x = index.the(t);
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
    public NAR goal(@NotNull String goalTermString, @NotNull Tense tense, float freq, float conf) throws NarseseException  {
        return goal((Termed)$.$(goalTermString), tense, freq, conf);
    }

    /**
     * desire goal
     */
    @Nullable
    public NAR goal(@NotNull Termed<Compound> goalTerm, @NotNull Tense tense, float freq, float conf)  {
        return goal(
                priorityDefault(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq, float conf)  {
        believe(priorityDefault(BELIEF), term, time(tense), freq, conf);
        return this;
    }
    @NotNull public NAR believe(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq)  {
        return believe(term, tense, freq, confidenceDefault(Symbols.BELIEF));
    }
    @NotNull public NAR goal(@NotNull Termed<Compound> term, @NotNull Tense tense, float freq)  {
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
    public NAR goal(@NotNull Termed term, float freq, float conf)  {
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
    public NAR goal(@NotNull String termString) throws NarseseException {
        return goal((Termed)term(termString), true);
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
    public NAR goal(@NotNull String termString, boolean isTrue) throws NarseseException {
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
    public NAR goal(@NotNull Termed<Compound> term)  {
        return goal(term, true);
    }

    @NotNull
    public NAR goal(@NotNull Termed<Compound> term, boolean trueOrFalse)  {
        return goal(term, trueOrFalse, confidenceDefault(BELIEF));
    }
    @NotNull
    public NAR believe(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf) throws NarseseException {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }
    @NotNull
    public NAR goal(@NotNull Termed<Compound> term, boolean trueOrFalse, float conf)  {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri, float dur, @NotNull Termed<Compound> term, long occurrenceTime, float freq, float conf) throws NarseseException {
        return input(pri, dur, term, BELIEF, occurrenceTime, freq, conf);
    }


    @Nullable public NAR goal(float pri, @NotNull Termed<Compound> goal, long when, float freq, float conf)  {
        input(pri, durabilityDefault(GOAL), goal, GOAL, when, freq, conf);
        return this;
    }
    @Nullable public NAR goal(float pri, @NotNull Termed<Compound> goal, @NotNull Tense tense, float freq, float conf)  {
        input(pri, durabilityDefault(GOAL), goal, GOAL, time(tense), freq, conf);
        return this;
    }

    @Nullable
    public Task input(float pri, float dur, @NotNull Termed<Compound> term, char punc, long occurrenceTime, float freq, float conf) {

        if (term == null) {
            return null;
        }

        Task t = new MutableTask(term, punc, new DefaultTruth(freq, conf))
                .budget(pri, dur)
                .time(time(), occurrenceTime);

        input(t);

        return t;
    }
    @NotNull
    public Task ask(@NotNull Termed<Compound> term, char questionOrQuest)  {
        return ask(term, questionOrQuest, ETERNAL);
    }

    @NotNull
    public Task ask(@NotNull Termed<Compound> term, char questionOrQuest, long when)  {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        if ((questionOrQuest != QUESTION) && (questionOrQuest != QUEST))
            throw new RuntimeException("invalid punctuation");

        MutableTask t = new MutableTask(term, questionOrQuest, null);
        t.time(time(), when);

        input(t);

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
            return b != null && b.summary() > summaryThreshold;
        });
    }

    /**
     * exposes the memory to an input, derived, or immediate task.
     * the memory then delegates it to its controller
     * <p>
     * return true if the task was processed
     * if the task was a command, it will return false even if executed
     */
    public final void input(@NotNull Task t) {
        if (t.isCommand()) {
            t.execute(null, this); //direct execution
        } else {
            process(t.normalize(this)); //accept into input buffer for eventual processing
        }
    }

    @Override
    public final void accept(@NotNull Task task) {
        input(task);
    }


    @NotNull
    public TaskQueue input(@NotNull Collection<Task> t) {
        TaskQueue tq = new TaskQueue(t);
        input((Input) tq);
        return tq;
    }

    @NotNull
    public TaskQueue input(@NotNull Task... t) {
        TaskQueue tq = new TaskQueue(t);
        input((Input) tq);
        return tq;
    }

//    public On onExecTask(@NotNull String operator, @NotNull Consumer<Execution> f) {
//        return onExec(operator, f);
//    }


    public final On onExec(@NotNull AbstractOperator r) {
        r.init(this);
        return onExecution(r.getOperatorTerm(), r);
    }



    public final On onExecution(@NotNull String op, @NotNull Consumer<OperationConcept> each) {
        return onExecution($.operator(op), each);
    }

    @NotNull  public final On onExecution(@NotNull Operator op, @NotNull Consumer<OperationConcept> each) {
        return concept(op,true)
                .<Topic<OperationConcept>>meta(Execution.class,
                        (k, v) -> v!=null ?  v : new DefaultTopic<>())
                .on(each);
    }

    /**
     * Adds an input channel for input from an external sense / sensor.
     * Will remain added until it closes or it is explicitly removed.
     */
    @NotNull
    public Input input(@NotNull Input i) {
        i.input(this, 1);
        return i;
    }

    /**
     * Exits an iteration loop if running
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            throw new RuntimeException("wasnt running");
        }
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @NotNull
    public NAR step() {
        return run(1);
    }

    /**
     * pins thread to a CPU core to improve performance while
     * running some frames.
     * <p>
     * there is some overhead in acquiring the lock so it
     * will not make sense to use this method unless
     * the expected runtime for the given # of frames
     * is sufficiently high (ie. dont use this in a loop;
     * instead put the loop inside an AffinityLock)
     */
    @NotNull
    public NAR runBatch(int frames) {

        AffinityLock al = AffinityLock.acquireLock();
        try {
            run(frames);
        } finally {
            al.release();
        }

        return this;
    }

    /**
     * Runs multiple frames, unless already running (then it return -1).
     *
     * @return total time in seconds elapsed in realtime
     */
    @NotNull
    public final NAR run(int frames) {

        AtomicBoolean r = this.running;
        if (!r.compareAndSet(false, true))
            throw new NAR.AlreadyRunningException();

        _frame(frames);

        r.compareAndSet(true, false);

        return this;
    }

    private final void _frame(int frames) {

        emotion.frame();

        Topic<NAR> frameStart = eventFrameStart;

        Topic<Memory> cycleStart = eventCycleEnd;

        Clock clock = this.clock;

        int cpf = cyclesPerFrame.intValue();
        for (; frames > 0; frames--) {
            clock.tick();
            frameStart.emit(this);
            cycles(cycleStart, cpf);
            runNextTasks();
        }


    }

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
                error(e);
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

        if (this.loop!=null) {
            throw new RuntimeException("Already running: " + this.loop);
        }

        return this.loop = new NARLoop(this, initialFramePeriodMS);
    }


    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(@NotNull Runnable t) {
        if (running.get()) {
            //in a frame, so schedule for after it
            nextTasks.add(t);
        } else {
            //not in a frame, can execute immediately
            t.run();
        }
    }


    /**
     * runs all the tasks in the 'Next' queue
     */
    private final void runNextTasks() {

        List<Runnable> n = this.nextTasks;
        if (!n.isEmpty()) {
            n.forEach(Runnable::run);
            n.clear();
        }
    }

    /**
     * signals an error through one or more event notification systems
     */
    protected void error(@NotNull Throwable ex) {
        eventError.emit(ex);
    }

    /**
     * queues a task to (hopefully) be executed at an unknown time in the future,
     * in its own thread in a thread pool
     */
    @Nullable
    public static Future runAsync(@NotNull Runnable t) {

        logger.trace("runAsyncs run {}", t);

        try {
            Future<?> f = asyncs.submit(t);
            return f;
        } catch (RejectedExecutionException e) {
            logger.error("execAsync error {} in {}", t, e);
            return null;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + toString() + ']';
    }



    public Task ask(@NotNull String question, long occ, @NotNull Predicate<Task> eachAnswer) throws NarseseException {
        return ask(term(question), occ, eachAnswer);
    }

    public Task ask(Termed<Compound> term, long occ, Predicate<Task> eachAnswer) {
        return ask(term, occ, Symbols.QUESTION, eachAnswer);
    }

    public Task ask(Termed<Compound> term, long occ, char punc /* question or quest */, Predicate<Task> eachAnswer) {
        return inputTask(new MutableTask(term, punc, null) {
            @Override public boolean onAnswered(Task answer) {
                return eachAnswer.test(answer);
            }
        }.occurr(occ));
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

        long now = time();
        if (time < now) {
            //past
            throw new RuntimeException("can not input at a past time");
        } else if (time == now) {
            //current cycle
            input(tt);
        } else {
            //future
            onFrame(m -> {
                //if (timeCondition.test(m.time())) {
                if (m.time() == time) {
                    m.input(tt);
                }
            });
        }
        return this;
    }

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

    @Nullable
    public final Concept concept(@NotNull Termed t) {
        return concept(t, false);
    }

    @Nullable
    public final Concept concept(@NotNull Termed t, boolean createIfMissing) {

        //optimization: assume a concept instance is the concept of this NAR

        if (t instanceof Concept) {
            //TODO check the concept hasnt been deleted, if not, then it is ok to accept the Concept as-is
            return (Concept) t;
        }

        Termed tt = t.term();
        if (tt instanceof Concept) {
            //TODO check the concept hasnt been deleted, if not, then it is ok to accept the Concept as-is
            return (Concept) tt;
        }

        tt = index.validConceptTerm(tt);
        if (tt == null)
            return null;

        Termed c = createIfMissing ? index.the(tt) : index.get(tt);
        if (!(c instanceof Concept)) {
            throw new RuntimeException("not a concept: " + c + " while resolving: " + t + " create=" + createIfMissing);
        }
        return (Concept) c;

    }

    @Nullable
    public abstract NAR forEachConcept(@NotNull Consumer<Concept> recip);

    /** activate the concept and other features (termlinks, etc)
     * @param link whether to activate termlinks recursively
     * */
    @Nullable
    public abstract Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted b, float conceptActivation, float linkActivation, @Nullable MutableFloat conceptOverflow);


    @Deprecated @Nullable
    final public Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted b) {
        return conceptualize(termed, b, 1f, 0f, null);
    }

    @NotNull
    public NAR stopIf(@NotNull BooleanSupplier stopCondition) {
        onFrame(n -> {
            if (stopCondition.getAsBoolean()) stop();
        });
        return this;
    }

    /** reasoning cycles occurr zero or more times per frame */
    @NotNull public NAR onCycle(@NotNull Consumer<Memory> receiver) {
        regs.add(eventCycleEnd.on(receiver));
        return this;
    }

    /** a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame. */
    @NotNull public On onFrame(@NotNull Consumer<NAR> receiver) {
        On r;
        regs.add(r = eventFrameStart.on(receiver));
        return r;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void input(@NotNull Stream<Task> taskStream) {
        input(new TaskStream(taskStream));
    }



    @Override
    public boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    /**
     * gets a measure of the current priority of the concept
     */
    abstract public float conceptPriority(Termed termed);

    public Termed[] terms(String... terms) {
        return Stream.of(terms).map(this::term).toArray(Termed[]::new);
    }

    public void dumpConcepts(@NotNull String path) throws FileNotFoundException {
        PrintStream pw = new PrintStream(new FileOutputStream(new File(path)));
        index.forEach(t -> {
            if (t instanceof Concept) {
                Concept cc = (Concept)t;
                cc.print(pw);
            } else {
                pw.append(t.toString());
            }
        });
        pw.close();
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

        public final Task task;


        public InvalidTaskException(Task t, String message) {
            super(message);
            this.task = t;
        }

        @NotNull
        @Override
        public String getMessage() {
            return super.getMessage();// + ": " + task.explanation();
        }

    }

    public static class AlreadyRunningException extends RuntimeException {
        public AlreadyRunningException() {
            super("already running");
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

    /** byte codec output of matching concept tasks (blocking) */
    @NotNull
    public NAR output(@NotNull OutputStream o, @NotNull Predicate<Task> each) {

        forEachConceptTask(t-> {
            if (each.test(t)) {
                try {
                    TermCodec.the.encodeToStream(o, t);
                } catch (IOException e) {
                    logger.error("{} when trying to output to {}", t, e);
                    throw new RuntimeException(e);
                    //e.printStackTrace();
                }
            }
        }, true, true, true, true);

        try {
            o.flush();
        } catch (IOException e) {
            logger.error("{}", e);
        }

        return this;
    }


    @NotNull
    public NAR output(@NotNull OutputStream o) {
        return output(o, x -> true);
    }

    /** byte codec input stream of tasks, to be input after decode */
    @NotNull
    public NAR input(@NotNull InputStream tasks) throws Exception {
        while (tasks.available() > 0) {
            Task t = (Task)TermCodec.the.decodeFromStream(tasks);
            input(t);
        }
        return this;
    }

    @Nullable
    abstract public Concept process(@NotNull Task input, float activation);

    @Nullable
    public final Concept process(@NotNull Task input) {
        return process(input, conceptActivation.floatValue());
    }

    /** accepts null-terminated array */
    public final void process(@NotNull Task... input) {
        float activation = conceptActivation.floatValue();
        for (Task t : input) {
            if (t == null) //for null-terminated arrays
                break;
            if (!t.isDeleted())
                process(t, activation);
        }
    }
}
