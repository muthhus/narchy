package nars.index.term;

import nars.term.Term;
import org.jetbrains.annotations.NotNull; /** marker interface for special terms that should not be interned */
public interface NonInternable {

    static boolean internable(Term... x) {
        for (Term y : x)
            if (!(y instanceof NonInternable)) //"must not intern non-internable" + y + "(" +y.getClass() + ")";
                return false;
        return true;
    }
}
