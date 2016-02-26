package nars;


import com.google.common.collect.Sets;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.Narsese.NarseseException;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.nal.Level;
import nars.nal.Tense;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.PatternAnswer;
import nars.nal.nal8.operator.TermFunction;
import nars.op.in.FileInput;
import nars.op.in.TextInput;
import nars.task.MutableTask;
import nars.task.Task;
import nars.task.flow.Input;
import nars.task.flow.TaskQueue;
import nars.task.flow.TaskStream;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.variable.AbstractVariable;
import nars.time.Clock;
import nars.util.event.AnswerReaction;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import net.openhft.affinity.AffinityLock;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Memory.CompoundAnonymizer;
import static nars.Symbols.*;
import static nars.nal.Tense.ETERNAL;
import static org.fusesource.jansi.Ansi.ansi;


/**
 * Non-Axiomatic Reasoner
 * <p>
 * Instances of this represent a reasoner connected to a Memory, and set of Input and Output channels.
 * <p>
 * All state is contained within Memory.  A NAR is responsible for managing I/O channels and executing
 * memory operations.  It executes a series sof cycles in two possible modes:
 * * step mode - controlled by an outside system, such as during debugging or testing
 * * thread mode - runs in a pausable closed-loop at a specific maximum framerate.
 */
public abstract class NAR implements Level, Consumer<Task> {


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
    static ThreadPoolExecutor asyncs =
            (ThreadPoolExecutor) Executors.newCachedThreadPool();
    static final Set<String> logEvents = Sets.newHashSet(
            "eventTaskProcess", "eventAnswer",
            "eventExecute", //"eventRevision", /* eventDerive */ "eventError",
            "eventSpeak"

    );
    /**
     * The memory of the reasoner
     * TODO dont expose as public
     */
    @NotNull
    public final Memory memory;
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

    //TODO use this to store all handler registrations, and decide if transient or not
    public final transient List<Object> regs = Global.newArrayList();

    private final transient Collection<Runnable> nextTasks = new CopyOnWriteArrayList(); //ConcurrentLinkedDeque();

    public NAR(@NotNull Memory m) {
        this(m, Global.DEFAULT_SELF);
    }

    public NAR(@NotNull Memory m, @NotNull Atom self) {

        memory = m;

        m.the(NAR.class, this);

        this.self = self;

        /** register some components in the dependency context, Container (which Memory subclasses from) */
        m.the("clock", m.clock);


        m.eventError.on(e -> {
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

        m.start();

    }

    private static void cycles(Memory memory, @NotNull Topic<Memory> cycleStart, int cyclesPerFrame) {
        for (; cyclesPerFrame > 0; cyclesPerFrame--) {
            cycleStart.emit(memory);
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

        NAR.asyncs.shutdown();

        memory.clear();

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
        //try {
        Task t = task(taskText);

        input(t);

        return t;
        /*} catch (Exception e) {
            return null;
        }*/
    }

    /**
     * parses and forms a Task from a string but doesnt input it
     */
    @Nullable
    public Task task(@NotNull String taskText) {
        return Narsese.the().task(taskText, memory);
    }

    @NotNull
    public List<Task> tasks(@NotNull String parse) {
        List<Task> result = Global.newArrayList(1);
        Narsese.the().tasks(parse, result, memory);
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

    @Nullable
    public <T extends Termed> T term(@NotNull String t) throws NarseseException {
        return index().the(t);
    }

    /**
     * gets a concept if it exists, or returns null if it does not
     */
    @Nullable
    public final Concept concept(@NotNull String conceptTerm) throws NarseseException {

        return concept(termOrException(conceptTerm));
    }

    /** parses a term, returning it, or throws an exception (but will not return null) */
    @NotNull public final Termed termOrException(@NotNull String conceptTerm) {
        Termed t = term(conceptTerm);
        if (t == null)
            throw new NarseseException(conceptTerm);
        return t;
    }

    /**
     * ask question
     */
    @NotNull
    public Task ask(@NotNull String termString) throws NarseseException {
        //TODO remove '?' if it is attached at end
        return ask((Compound)termOrException(termString));
    }

    /**
     * ask question
     */
    @NotNull
    public Task ask(@NotNull Compound c) {
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

    /**
     * desire goal
     */
    @Nullable
    public Task goal(@NotNull Compound goalTerm, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        return goal(
                memory.getDefaultPriority(GOAL),
                memory.getDefaultDurability(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed term, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        believe(memory.getDefaultPriority(BELIEF), term, time(tense), freq, conf);
        return this;
    }

    @Nullable
    public Task believe(float priority, @NotNull Termed term, long when, float freq, float conf) throws NarseseException {
        return believe(priority, memory.getDefaultDurability(BELIEF), term, when, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed term, float freq, float conf) throws NarseseException {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        believe(memory.getDefaultPriority(BELIEF), term(term), time(tense), freq, conf);
        return this;
    }

    public long time(@NotNull Tense tense) {
        return Tense.getRelativeOccurrence(tense, memory);
    }

    @NotNull
    public NAR believe(@NotNull String termString, float freq, float conf) throws NarseseException {
        return believe((Termed) term(termString), freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String termString) throws NarseseException {
        Termed term = termOrException(termString);
        return believe(term);
    }

    @NotNull
    public NAR believe(@NotNull Termed term) throws NarseseException {
        return believe(term, 1.0f, memory.getDefaultConfidence(BELIEF));
    }

    @Nullable
    public Task believe(float pri, float dur, @NotNull Termed term, long occurrenceTime, float freq, float conf) throws NarseseException {
        return input(pri, dur, term, BELIEF, occurrenceTime, freq, conf);
    }

    /**
     * TODO add parameter for Tense control. until then, default is Now
     */
    @Nullable
    public Task goal(float pri, float dur, @NotNull Termed goal, long occurrence, float freq, float conf) throws NarseseException {
        return input(pri, dur, goal, GOAL, occurrence, freq, conf);
    }

    @Nullable
    public Task input(float pri, float dur, @NotNull Termed term, char punc, long occurrenceTime, float freq, float conf) {

        if (term == null) {
            return null;
        }

        Task t = new MutableTask(term, punc)
                .truth(freq, conf)
                .budget(pri, dur)
                .time(time(), occurrenceTime);

        input(t);

        return t;
    }

    @NotNull
    public <T extends Compound> Task ask(@NotNull T term, char questionOrQuest) throws NarseseException {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance

        MutableTask t = new MutableTask(term);
        if (questionOrQuest == QUESTION)
            t.question();
        else if (questionOrQuest == QUEST)
            t.quest();
        else
            throw new RuntimeException("invalid punctuation");


        t.time(time(), ETERNAL);

        input(t);

        return t;

        //ex: return new Answered(this, t);

    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    public void logSummaryGT(@NotNull Appendable out, float summaryThreshold) {
        log(out, v -> {
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
    public final void input(@NotNull Task i) {

        Memory m = memory;

        Task u;
        if (i.isCommand()) {
            //direct execution
            if (execute(i, null)) {
                u = i;
            } else {
                m.remove(i, "Unmatched Command");
                u = null;
            }
        } else {
            if (!i.isDeleted()) {
                //accept input if it can be normalized
                if ((u = i.normalize(m)) != null) {
                    //ACCEPT TASK FOR INPUT
                    m.eventInput.emit(u);
                } else {
                    m.remove(i, "Unnormalizable");
                }
            } else {
                m.remove(i, "Pre-Deleted");
                u = null;
            }
        }

        if (null == u)
            throw new InvalidTaskException(i);
    }

    @Override
    public final void accept(@NotNull Task task) {
        input(task);
    }

    /**
     * Entry point for all potentially executable tasks.
     * Enters a task and determine if there is a decision to execute.
     *
     * @return number of invoked handlers
     */
    public final boolean execute(@NotNull Task inputGoal, @Nullable Concept goalConcept) {

        Term goalTerm = inputGoal.term();
        if (!Op.isOperation(goalTerm)) {
            return false;
        }

        Task goal = inputGoal;

        if (goalConcept != null) {
            //Normal Goal
            long now = time();
            Task projectedGoal = goalConcept.goals().top(now);
            float motivation = projectedGoal.motivation();

            //counteract with content from any (--, concept
            Term antiTerm = $.neg(projectedGoal.term());
            Concept antiConcept = concept(antiTerm);
            if (antiConcept!=null)
                motivation -= antiConcept.motivationElse(now, 0);

            if (motivation < memory.executionThreshold.floatValue())
                return false;

            long occ = projectedGoal.occurrence();
            if ((!((occ == ETERNAL) || (Math.abs(occ-now) < memory.duration()*2)))//right timing
                    ) { //sufficient motivation
                return false;
            }

            goal = projectedGoal;
        } else {
            //a Command to directly execute, unmodified
        }


        return goal.execute(this);

        /*else {
            System.err.println("Unexecutable: " + goal);
        }*/


        //float delta = updateSuccess(goal, successBefore, memory);

        //&& (goal.state() != Task.TaskState.Executed)) {

            /*if (delta >= Global.EXECUTION_SATISFACTION_TRESHOLD)*/

        //Truth projected = goal.projection(now, now);


//                        LongHashSet ev = this.lastevidence;
//
//                        //if all evidence of the new one is also part of the old one
//                        //then there is no need to execute
//                        //which means only execute if there is new evidence which suggests doing so1
//                        if (ev.addAll(input.getEvidence())) {

//                            //TODO more efficient size limiting
//                            //lastevidence.toSortedList()
//                            while(ev.size() > max_last_execution_evidence_len) {
//                                ev.remove( ev.min() );
//                            }
//                        }

    }

    /**
     * register a singleton
     */
    public final <X> X the(Object key, X value) {
        return memory.the(key, value);
    }

    /**
     * returns the global concept index
     */
    public final TermIndex index() {
        return memory.index;
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


    /**
     * creates a TermFunction operator from a supplied function, which can be a lambda
     */
    public On onExecTerm(@NotNull String operator, @NotNull Function<Term[], Object> func) {
        return onExec(new TermFunction(operator) {

            @Override
            public Object function(@NotNull Compound x, TermIndex i) {
                return func.apply(x.terms());
            }

        });
    }

    public On onExec(@NotNull AbstractOperator r) {
        r.init(this);
        return onExecution(r.getOperatorTerm(), r);
    }


    public On onExec(@NotNull String op, @NotNull Consumer<Term[]> each) {
        return onExecution($.operator(op), e -> {
            each.accept(Operator.argArray(e.term()));
        });
    }

    public On onExecution(@NotNull String op, @NotNull Consumer<Task> each) {
        return onExecution($.operator(op), each);
    }

    public On onExecution(@NotNull Atomic op, @NotNull Consumer<Task> each) {
        return memory.exe.computeIfAbsent(op,
                o -> new DefaultTopic<Task>())
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
        Memory memory = this.memory;

        Topic<NAR> frameStart = memory.eventFrameStart;

        Topic<Memory> cycleStart = memory.eventCycleEnd;

        Clock clock = memory.clock;

        int cpf = memory.cyclesPerFrame.intValue();
        for (; frames > 0; frames--) {
            clock.tick();
            frameStart.emit(this);
            cycles(memory, cycleStart, cpf);
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

        Topic.all(memory, (k, v) -> {

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
            previou = chan;
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
                            tv.toString(memory, true)
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
    NARLoop loop(int initialFramePeriodMS) {
//        //TODO use DescriptiveStatistics to track history of frametimes to slow down (to decrease speed rate away from desired) or speed up (to reach desired framerate).  current method is too nervous, it should use a rolling average

        return new NARLoop(this, initialFramePeriodMS);
    }

    /**
     * sets current maximum allowed NAL level (1..8)
     */
    @NotNull
    public NAR nal(int level) {
        memory.nal(level);
        return this;
    }

    /**
     * returns the current level
     */
    @Override
    public int nal() {
        return memory.nal();
    }

    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public void runLater(@NotNull Runnable t) {
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
    private void runNextTasks() {
        Collection<Runnable> n = this.nextTasks;
        if (!n.isEmpty()) {
            n.forEach(Runnable::run);
            n.clear();
        }
    }

    /**
     * signals an error through one or more event notification systems
     */
    protected void error(@NotNull Throwable ex) {
        memory.eventError.emit(ex);
    }

    /**
     * queues a task to (hopefully) be executed at an unknown time in the future,
     * in its own thread in a thread pool
     */
    public boolean runAsync(@NotNull Runnable t) {
        return runAsync(t, null);
    }

    public boolean runAsync(@NotNull Runnable t, @Nullable Consumer<RejectedExecutionException> onError) {
        try {
            memory.eventSpeak.emit("execAsync " + t);
            memory.eventSpeak.emit("pool: " + NAR.asyncs.getActiveCount() + " running, " + NAR.asyncs.getTaskCount() + " pending");

            NAR.asyncs.execute(t);

            return true;
        } catch (RejectedExecutionException e) {
            if (onError != null)
                onError.accept(e);
            return false;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + memory.toString() + ']';
    }

    /**
     * Get the current time from the clock
     *
     * @return The current time
     */
    public final long time() {
        return memory.time();
    }

    @NotNull
    public NAR onAnswer(@NotNull String question, @NotNull Consumer<Task> recvSolution) {
        //question punctuation optional
        if (!(question.length() > 0 && question.charAt(question.length() - 1) == '?')) question = question + '?';
        Task qt = task(question);
        return onAnswer(qt, recvSolution);
    }

    /**
     * inputs the question and observes answer events for a solution
     */
    @NotNull
    public NAR onAnswer(@NotNull Task questionOrQuest, @NotNull Consumer<Task> c) {
        new AnswerReaction(this, questionOrQuest) {

            @Override
            public void onSolution(Task belief) {
                c.accept(belief);
            }

        };
        return this;
    }

    @NotNull
    public NAR input(@NotNull String... ss) {
        for (String s : ss) input(s);
        return this;
    }

    @NotNull
    public NAR inputAt(long time, @NotNull String... tt) {
        //LongPredicate timeCondition = t -> t == time;

        onFrame(m -> {
            //if (timeCondition.test(m.time())) {
            if (m.time() == time) {
                m.input(tt);
            }
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
    public abstract NAR forEachConcept(@NotNull Consumer<Concept> recip);

    public abstract Concept conceptualize(@NotNull Termed termed, Budget activation, float scale, float toTermLinks);

    final public Concept conceptualize(@NotNull Termed termed, Budget activation) {
        return conceptualize(termed, activation, 1f, 0);
    }

    @NotNull
    public NAR stopIf(@NotNull BooleanSupplier stopCondition) {
        onFrame(n -> {
            if (stopCondition.getAsBoolean()) stop();
        });
        return this;
    }

    /** reasoning cycles occurr zero or more times per frame */
    @NotNull public NAR onCycle(Consumer<Memory> receiver) {
        regs.add(memory.eventCycleEnd.on(receiver));
        return this;
    }

    /** a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame. */
    @NotNull public NAR onFrame(Consumer<NAR> receiver) {
        regs.add(memory.eventFrameStart.on(receiver));
        return this;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void input(@NotNull Stream<Task> taskStream) {
        input(new TaskStream(taskStream));
    }

    protected final void process(Task[] input) {
        for (Task t : input) {
            if (t == null)
                break;
            process(t);
        }
    }

    /**
     * execute a Task as a TaskProcess (synchronous)
     * <p>
     * TODO make private
     */
    @Nullable
    protected final Concept process(@NotNull Task input) {

        if (input.isDeleted()) {
            //throw new RuntimeException(
            /*if (Global.DEBUG) {
                System.err.println(
                        input + " " + input.log() + " Deleted:\n" + input.explanation());
            }*/
            return null;
        }

        final Memory memory = this.memory;

        float activation = memory.activationRate.floatValue();
        Concept c = conceptualize(input.concept(), input.budget(), activation, 0f);
        if (c == null) {
            //throw new RuntimeException("Inconceivable: " + input);
            memory.remove(input, "Inconceivable");
            return null;
        }

        memory.emotion.busy(input);

        Task t = c.process(input, this);
        if (t == null) {
            //dont delete 'input' because it may be a duplicate which was already in the table and remains there
            return null;
        }

        //complete the activation process
        c.link(t, activation, 0f, this);

        memory.eventTaskProcess.emit(t);

        if (t.isGoal())
            execute(t, c);

        return c;
    }




    boolean validConceptTerm(Term term) {
        return !(term instanceof AbstractVariable);
    }

    @NotNull
    abstract protected Concept newConcept(Term t);


    @Nullable
    public Concept concept(Termed t) {
        Term tt = t.term();

        if (tt instanceof Concept)
            return ((Concept)tt); //assume this is the instance already indexed; may not be a safe assumption

        if (!validConceptTerm(tt))
            return null;

        TermIndex index = memory.index;

        if (!tt.isNormalized()) {
            t = index.normalized(tt);
            if (t instanceof Concept)
                return ((Concept)t);
            else if (t == null)
                return null;
            else
                tt = t.term();
        }

        //TODO ? put the unnormalized term for cached future normalizations?

        if (tt.isCompound() ) {

            Term at = tt;

            if (at!=(tt = tt.anonymous())) {
                //complete anonymization process
                if (null == (tt = index.transform((Compound) tt, CompoundAnonymizer)))
                    throw new UnbuildableTerm((Compound) at);
            }

        }



        Termed uu = index.the(tt);

        if (uu instanceof Concept) {
            return (Concept) uu;
        } else {
            throw new RuntimeException("unconceptualizable: " + uu + " " + uu.getClass());
//            if (uu == null) uu = t;
//
//            Concept n = newConcept(uu.term());
//            if (n!=null) {
//                index.put(n);
//                return n;
//            } else {
//                return null; //no concept could be made
//            }
        }

    }




    public On onQuestion(@NotNull PatternAnswer p) {
        return memory.eventTaskProcess.on(question -> {
            if (question.punc() == Symbols.QUESTION) {
                runLater(() -> {
                    List<Task> l = p.apply(question);
                    if (l != null) {
                        l.forEach(answer -> memory.eventAnswer.emit(Tuples.twin(question, answer)));
                        input(l);
                    }
                });
            }
        });
    }

    @Override
    public boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    /**
     * gets a measure of the current priority of the concept
     */
    abstract public float conceptPriority(Termed termed, float priIfNonExistent);

    public Termed[] terms(String... terms) {
        return Stream.of(terms).map(this::term).toArray(Termed[]::new);
    }

    public synchronized void dumpConcepts(@NotNull String path) throws FileNotFoundException {
        PrintStream pw = new PrintStream(new FileOutputStream(new File(path)));
        index().forEach(t -> {
            if (t instanceof Concept) {
                Concept cc = (Concept)t;
                cc.print(pw);
            } else {
                pw.append(t.toString());
            }
        });
        pw.close();
    }

    public final <X extends NAR> X with(Object... values) {
        memory.with(values);
        return (X)this;
    }

    public static final class InvalidTaskException extends RuntimeException {

        public final Task task;

        public InvalidTaskException(Task t) {
            super();
            this.task = t;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "Invalid Task: " + task.explanation();
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

}
