package nars.nal.meta;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/30/15.
 */
public interface ProcTerm extends Term {

    default void appendJavaProcedure(@NotNull StringBuilder s) {
        s.append("/* TODO: ").append(this).append(" */\n");
    }

    void accept(PremiseEval c);
}
