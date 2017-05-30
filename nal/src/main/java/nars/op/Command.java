package nars.op;

import jcog.bag.Bag;
import nars.*;
import nars.concept.AtomConcept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

import static nars.time.Tense.ETERNAL;

/**
 * Operator interface specifically for Command ';' punctuation
 */

abstract public class Command extends AtomConcept implements PermanentConcept {

    public static String LOG_FUNCTOR = String.valueOf(Character.valueOf((char) 8594)); //RIGHT ARROW

    public Command(@NotNull Atom atom) {
        super(atom, Bag.EMPTY, Bag.EMPTY);
    }

    @Deprecated protected void run(@NotNull Atomic op, @NotNull Term[] args, @NotNull NAR nar) {

    }

    public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
        Compound c = t.term();
        try {
            run((Atomic) (c.sub(1)), ((Compound) (t.term(0))).toArray(), nar);
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


    static Task task(Compound content, long when) {
        return new NALTask(content, Op.COMMAND, null, when, when, when, new long[] { });
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


}
