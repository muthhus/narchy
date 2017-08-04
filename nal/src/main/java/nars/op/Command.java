package nars.op;

import nars.*;
import nars.concept.BaseConcept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

import static nars.time.Tense.ETERNAL;

/**
 * Operator interface specifically for Command ';' punctuation
 */

abstract public class Command extends BaseConcept implements PermanentConcept {

    public static String LOG_FUNCTOR = String.valueOf(Character.valueOf((char) 8594)); //RIGHT ARROW

    protected Command(@NotNull Atom atom, NAR n) {
        super(atom, n);
    }

    @Deprecated protected void run(@NotNull Atomic op, @NotNull Term[] args, @NotNull NAR nar) {

    }

    public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
        Term c = t.term();
        try {
            run((Atomic)c.sub(1), ((Compound) (t.sub(0))).toArray(), nar);
            return t;
        } catch (Throwable error) {
            if (Param.DEBUG)
                throw error;
            else
                return error(error, nar.time());
        }
    }

    static Task error(Throwable error, long when ) {
        StringWriter ss = new StringWriter();
        ExceptionUtils.printRootCauseStackTrace(error, new PrintWriter(ss));
        return Command.task("error", when, $.quote(ss.toString()));
    }


    static Task task(Term content, long when) {
        return new NALTask(content, Op.COMMAND, null, when, when, when, ArrayUtils.EMPTY_LONG_ARRAY);
    }

    public static Task logTask(@NotNull Term content) {
        return logTask(ETERNAL, content);
    }

    static Task logTask(long when, @NotNull Term... content) {
        return Command.task(LOG_FUNCTOR, when, content );
    }

    static Task task(String func, long now, @NotNull Term... args) {
        return Command.task($.func(func, args), now);
    }

    public static void log(NAR nar, @NotNull String... msg) {
        nar.input( Command.logTask(nar.time(), $.the(msg)) );
    }
    public static void log(NAR nar, @NotNull Object x) {
        nar.input( Command.logTask(nar.time(), $.the(x.toString())) );
    }


}
