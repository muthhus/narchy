package nars.concept.util;

import nars.term.Termed;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 9/13/16.
 */
public final class InvalidConceptException extends SoftException {

    @NotNull
    public final Termed term;
    @NotNull
    public final String reason;

    public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
        this.term = term;
        this.reason = reason;
    }

    @NotNull
    @Override
    public String getMessage() {
        return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
    }

}
