package nars;


import com.google.common.collect.Sets;
import com.gs.collections.impl.tuple.Tuples;
import nars.Narsese.NarseseException;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.nal.Level;
import nars.nal.Tense;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.Execution;
import nars.nal.nal8.Operator;
import nars.nal.nal8.PatternAnswer;
import nars.nal.nal8.operator.TermFunction;
import nars.task.MutableTask;
import nars.task.Task;
import nars.task.flow.Input;
import nars.task.flow.TaskQueue;
import nars.task.flow.TaskStream;
import nars.task.in.FileInput;
import nars.task.in.TextInput;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.compound.Compound;
import nars.time.Clock;
import nars.util.event.AnswerReaction;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import net.openhft.affinity.AffinityLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

import static nars.Symbols.*;
import static nars.nal.Tense.ETERNAL;


/**
 * Non-Axiomatic Reasoner
 * <p>
 * Instances of this represent a reasoner connected to a Memory, and set of Input and Output channels.
 * <p>
 * All state is contained within Memory.  A NAR is responsible for managing I/O channels and executing
 * memory operations.  It executesa series sof cycles in two possible modes:
 * * step mode - controlled by an outside system, such as during debugging or testing
 * * thread mode - runs in a pausable closed-loop at a specific maximum framerate.
 */
public abstract class NAR implements Level,Consumer<Task> {


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
    static final ThreadPoolExecutor asyncs =
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
    public final transient List<Object> regs = new ArrayList();

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
    public TextInput input(@NotNull String text) {
        TextInput i = new TextInput(this, text);
        if (i.size() == 0) {
            //TODO replace with real parser error
            error(new NarseseException("Input syntax error: " + text));
        }
        input((Input) i);
        return i;
    }

    @NotNull
    public <T extends Termed> T term(@NotNull String t) throws NarseseException {

        T x = (T) Narsese.the().term(t, index());

        if (x == null) {
            NAR.logger.error("Term syntax error: '{}'", t);
        } else {

            //this is applied automatically when a task is entered.
            //it's only necessary here where a term is requested
            //TODO apply this in index on the original copy only
//            Term xt = x.term();
//            if (xt.isCompound()) {
//                xt.setDuration(memory.duration());
//            }
        }
        return x;
    }

    /**
     * gets a concept if it exists, or returns null if it does not
     */
    @Nullable
    public Concept concept(@NotNull String conceptTerm) throws NarseseException {
        return memory.concept(term(conceptTerm));
    }

    /**
     * ask question
     */
    @NotNull
    public Task ask(@NotNull String termString) throws NarseseException {
        //TODO remove '?' if it is attached at end
        /*if (t instanceof Compound)
            return ((T)t).normalizeDestructively();*/
        return ask((Compound) Narsese.the().<Compound>term(termString));
    }

    /**
     * ask question
     */
    @NotNull
    public Task ask(@NotNull Compound c) {
        //TODO remove '?' if it is attached at end
        return ask(c, QUESTION);
    }

    /**
     * ask quest
     */
    @Nullable
    public Task askShould(@NotNull String questString) throws NarseseException {
        Term c = term(questString);
        if (c instanceof Compound)
            return askShould((Compound) c);
        return null;
    }

    /**
     * ask quest
     */
    @NotNull
    public Task askShould(@NotNull Compound quest) {
        return ask(quest, QUEST);
    }

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
        believe(memory.getDefaultPriority(JUDGMENT), term, time(tense), freq, conf);
        return this;
    }

    @Nullable
    public Task believe(float priority, @NotNull Termed term, long when, float freq, float conf) throws NarseseException {
        return believe(priority, memory.getDefaultDurability(JUDGMENT), term, when, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull Termed term, float freq, float conf) throws NarseseException {
        return believe(term, Tense.Eternal, freq, conf);
    }

    @NotNull
    public NAR believe(@NotNull String term, @NotNull Tense tense, float freq, float conf) throws NarseseException {
        believe(memory.getDefaultPriority(JUDGMENT), term(term), time(tense), freq, conf);
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
        return believe((Termed) term(termString));
    }

    @NotNull
    public NAR believe(@NotNull Termed term) throws NarseseException {
        return believe(term, 1.0f, memory.getDefaultConfidence(JUDGMENT));
    }

    @Nullable
    public Task believe(float pri, float dur, @NotNull Termed term, long occurrenceTime, float freq, float conf) throws NarseseException {
        return input(pri, dur, term, JUDGMENT, occurrenceTime, freq, conf);
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

    public static final class InvalidTaskException extends RuntimeException {

        public final Task task;

        public InvalidTaskException(Task t) {
            super();
            this.task = t;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "Invalid Task: " + task.getExplanation();
        }

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
        //if (i != null) {
            if (i.isCommand()) {
                //direct execution
                int n = execute(i);
                if (n == 0) {
                    m.remove(i, "Unmatched Command");
                    u = null;
                } else {
                    u = i;
                }
            } else {
                if (i.isDeleted()) {
                    m.remove(i, "Pre-Deleted");
                    u = null;
                } else {
                    //accept input if it can be normalized
                    u = i.normalize(m);
                    if (u == null) {
                        m.remove(i, "Unnormalizable");
                    } else {
                        m.eventInput.emit(u);
                    }
                }
            }
        /*} else {
            u = null;
        }*/

        if (null == u)
            throw new InvalidTaskException(i);
    }

    @Override public final void accept(Task task) {
        input(task);
    }

    /**
     * Entry point for all potentially executable tasks.
     * Enters a task and determine if there is a decision to execute.
     *
     * @return number of invoked handlers
     */
    public final int execute(@NotNull Task goal) {
        Term operation = goal.term();

        if (Op.isOperation(operation)) {

            if (!goal.isEternal())
                goal.setExecuted();

            Topic<Execution> tt = memory.exe.get(
                Operator.operatorTerm((Compound) operation)
            );

            if (tt != null && !tt.isEmpty()) {

                //enqueue after this frame, before next
                //beforeNextFrame(
                new Execution(this, goal, tt).run();
                //);
                return 1;
            }

        }
        /*else {
            System.err.println("Unexecutable: " + goal);
        }*/

        return 0;
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

    public On onExecTask(@NotNull String operator, @NotNull Consumer<Execution> f) {
        return onExec(operator, f);
    }



    /**
     * creates a TermFunction operator from a supplied function, which can be a lambda
     */
    public On onExecTerm(@NotNull String operator, @NotNull Function<Term[], Object> func) {
        return onExec(new TermFunction(operator) {

            @Override
            public Object function(@NotNull Compound x, TermBuilder i) {
                return func.apply(x.terms());
            }

        });
    }

    public On onExec(@NotNull AbstractOperator r) {
        return onExec(r.getOperatorTerm(), r);
    }

    public On onExec(@NotNull String op, @NotNull Consumer<Execution> each) {
        return onExec($.operator(op), each);
    }

    public On onExec(@NotNull Operator op, @NotNull Consumer<Execution> each) {
        Topic<Execution> t = memory.exe.computeIfAbsent(
                op, (Term o) -> new DefaultTopic<Execution>());
        return t.on(each);
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
        for ( ; frames > 0; frames--) {
            clock.tick();
            frameStart.emit(this);
            cycles(memory, cycleStart, cpf);
            runNextTasks();
        }
    }

    private static void cycles(Memory memory, @NotNull Topic<Memory> cycleStart, int cyclesPerFrame) {
        for (; cyclesPerFrame > 0; cyclesPerFrame--) {
            cycleStart.emit(memory);
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

            try {
                outputEvent(out, previous[0], k, v);
            } catch (IOException e) {
                error(e);
            }
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

    public void outputEvent(@NotNull Appendable out, String previou, @NotNull String k, Object v) throws IOException {
        //indent each cycle
        if (!"eventCycleStart".equals(k)) {
            out.append("  ");
        }

        String chan = k.toString();
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

        if (v instanceof Object[])
            v = Arrays.toString((Object[]) v);
        else if (v instanceof Task)
            v = ((Task) v).toString(memory, true);

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
    public NAR answer(@NotNull String question, @NotNull Consumer<Task> recvSolution) {
        //question punctuation optional
        if (!(question.length() > 0 && question.charAt(question.length() - 1) == '?')) question = question + '?';
        Task qt = task(question);
        return answer(qt, recvSolution);
    }

    /**
     * inputs the question and observes answer events for a solution
     */
    @NotNull
    public NAR answer(@NotNull Task questionOrQuest, @NotNull Consumer<Task> c) {
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

        onEachFrame(m -> {
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
            if (includeConceptBeliefs && c.hasBeliefs()) c.getBeliefs().top(maxPerConcept, recip);
            if (includeConceptQuestions && c.hasQuestions()) c.getQuestions().top(maxPerConcept, recip);
            if (includeConceptGoals && c.hasBeliefs()) c.getGoals().top(maxPerConcept, recip);
            if (includeConceptQuests && c.hasQuests()) c.getQuests().top(maxPerConcept, recip);
            if (includeTaskLinks && null != c.getTaskLinks())
                c.getTaskLinks().forEach(maxPerConcept, t->recip.accept(t.get()));
        });

        return this;
    }

    @Nullable
    public abstract NAR forEachConcept(@NotNull Consumer<Concept> recip);

    @Nullable
    public abstract Concept conceptualize(@NotNull Termed termed, Budget activation, float scale);

    @NotNull
    public NAR stopIf(@NotNull BooleanSupplier stopCondition) {
        onEachFrame(n -> {
            if (stopCondition.getAsBoolean()) stop();
        });
        return this;
    }

    @NotNull
    public NAR onEachCycle(Consumer<Memory> receiver) {
        regs.add(memory.eventCycleEnd.on(receiver));
        return this;
    }

    @NotNull
    public NAR onEachFrame(Consumer<NAR> receiver) {
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

    /**
     * execute a Task as a TaskProcess (synchronous)
     *
     * TODO make private
     */
    @Nullable
    protected final Concept process(@NotNull Task input) {

        if (input.isDeleted()) {
            //throw new RuntimeException(
            System.err.println(
                    input + " "  + input.log() + " deleted:\n" + input.getExplanation());
            return null;
        }


        float activation = memory.activationRate.floatValue();
        Concept c = conceptualize(input.concept(), input.budget(), activation);
        if (c == null) {
            throw new RuntimeException("Inconceivable: " + input);
        }

        memory.emotion.busy(input);

        Task matched = c.process(input, this);
        //if (!task.getDeleted()) {

        c.link(matched, activation, this);

//        if (input!=matched) {
//            //if (Global.DEBUG..
//            input.log(Tuples.pair("Resolved", matched));
//            logger.info(input.getExplanation());
//        }

        memory.eventTaskProcess.emit(matched);
        //}

        return c;
    }

    /**
     * convenience method shortcut for concept(t.getTerm())
     * when possible, try to provide an existing Concept instance
     * to avoid a lookup
     */
    @Nullable
    public Concept concept(Termed termed) {
        return memory.concept(termed);
    }

    public On onQuestion(@NotNull PatternAnswer p) {
        return memory.eventTaskProcess.on(question -> {
            if (question.punc() == '?') {
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
