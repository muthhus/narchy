package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.quote;
import static nars.Op.COMMAND;

/**
 * Operator interface specifically for Command ';' punctuation
 */
@FunctionalInterface
public interface Command extends Operator {

    @Override
    default @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
        if (t.punc() == COMMAND) {
            Compound c = t.term();
            run((Atomic) (c.term(1)), ((Compound) (t.term(0))).terms(), nar);
            return t;
        }
        return null;
    }

    void run(@NotNull Atomic op, @NotNull Term[] args, @NotNull NAR nar);

    static Task task(Compound content) {
        return new MutableTask(content, Op.COMMAND, null);
    }

    static Task log(@NotNull Term content) {
        return Command.task($.func("log", content));
    }

    static Task log(@NotNull String msg) {
        return Command.log($.quote(msg));
    }

    static void log(NAR nar, @NotNull Term content) {
        nar.inputLater( Command.log(content) );
    }

    static void log(NAR nar, @NotNull String msg) {
        nar.inputLater( Command.log(msg) );
    }

}
