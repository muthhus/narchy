package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static nars.Op.ATOM;
import static nars.Op.INH;
import static nars.time.Tense.ETERNAL;

/**
 * Operator interface specifically for goal and command punctuation
 * Allows dynamic handling of argument list like a functor,
 * but at the task level
 * <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
 * <patham9_> executed
 * <patham9_> the consequences of this, to give examples, are a lot:
 * <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
 * <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
 * <patham9_> 3. the system wont execute and pursue what is already satisfied
 * <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
 * <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
 */
public class Operator extends BaseConcept implements PermanentConcept {

    public static String LOG_FUNCTOR = String.valueOf(Character.valueOf((char) 8594)); //RIGHT ARROW

    public final BiFunction<Task, NAR, Task> execute;

    public Operator(@NotNull Atom atom, BiFunction<Task, NAR, Task> execute, NAR n) {
        super(atom, n);
        this.execute = execute;
    }

    /**
     * returns the arguments of an operation (task or term)
     */
    public static @Nullable TermContainer args(@NotNull Termed operation) {
        assert (operation.op() == INH && operation.subIs(1, ATOM));
        return operation.sub(0).subterms();
    }

    public static Task error(Task x, Throwable error, long when) {
        StringWriter ss = new StringWriter();
        ExceptionUtils.printRootCauseStackTrace(error, new PrintWriter(ss));
        return Operator.command("error", when, $.quote(x.toString()), $.quote(ss.toString()));
    }


    static Task command(Term content, long when) {
        return new NALTask(content, Op.COMMAND, null, when, when, when, ArrayUtils.EMPTY_LONG_ARRAY);
    }

    public static Task log(long when, @NotNull Object content) {
        return Operator.command(LOG_FUNCTOR, when, $.the(content));
    }

    public static Task log(@NotNull Term content) {
        return log(ETERNAL, content);
    }

    static Task command(String func, long now, @NotNull Term... args) {
        return Operator.command($.func(func, args), now);
    }

//    public static void log(NAR nar, @NotNull String... msg) {
//        nar.input( Operator.log(nar, $.the(msg)) );
//    }
//
//    public static void log(NAR nar, @NotNull Object x) {
//        nar.input( Operator.log(nar, x) );
//    }

    /**
     * debounced and atomically/asynchronously executable operation
     */
    public static class AtomicExec implements BiFunction<Task, NAR, Task> {

        private final float minPeriod;
        private final float expThresh;

        /**
         * time of the current rising edge, or ETERNAL if not activated
         */
        final AtomicLong rise = new AtomicLong(ETERNAL);

        long lastActivity = ETERNAL;
        public static final Logger logger = LoggerFactory.getLogger(AtomicExec.class);

        final BiConsumer<Task, NAR> exe;

        public AtomicExec(BiConsumer<Task, NAR> exe, float expThresh) {
            this(exe, expThresh, 1);
        }

        public AtomicExec(BiConsumer<Task, NAR> exe, float expThresh, float minPeriod /* dur's */) {
            this.exe = exe;
            this.minPeriod = minPeriod;
            this.expThresh = expThresh;
        }

        @Override
        public @Nullable Task apply(@NotNull Task x, @NotNull NAR n) {

            long now = n.time();
            int dur = n.dur();
            int cycleRadiusToWatch = dur;
            if (!x.isBefore(now)) {
                long xs = x.start();
                if (xs ==now) {
                    return tryInvoke(x, n);
                } else {
                    n.at(xs, ()->tryInvoke(x, n));
                }
            }

            return x;
        }

        /**
         * executed async
         */
        protected void invoke(Task x, NAR n) {
            try {
                @Nullable Concept cc = x.concept(n, true);
                if (cc != null) {
                    long now = n.time();
                    Truth desire = cc.goals().truth(now, n);
                    if (desire != null && desire.expectation() >= expThresh) {
                        exe.accept(x, n);
                    }
                }
            } catch (Throwable t) {
                logger.info("{} {}", this, t);
            } finally {
                //end invocation
                lastActivity = n.time();
                rise.set(ETERNAL);
            }
        }

        public @Nullable Task tryInvoke(Task x, NAR n) {

            long now = n.time();
            if (lastActivity == ETERNAL || (now - lastActivity > minPeriod * n.dur()) && rise.compareAndSet(ETERNAL, now)) {


                n.runLater(() -> {
                    invoke(x, n);
                }); //async exec

                //invoke(x, n); //inline exec
            }
            return null;
        }

        public boolean isInvoked() {
            return rise.get() != ETERNAL;
        }

    }
}
