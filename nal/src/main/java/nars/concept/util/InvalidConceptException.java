package nars.concept.util;

import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 9/13/16.
 */
public final class InvalidConceptException extends RuntimeException {

    @NotNull
    public final Termed term;
    @NotNull
    public final String reason;

    public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
        this.term = term;
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
    }

}
