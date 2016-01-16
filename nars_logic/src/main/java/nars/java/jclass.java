package nars.java;

import nars.$;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.compile.TermBuilder;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * resolve a java class from String to its usage knowledge
 */
public class jclass extends TermFunction {

    final Termizer termizer = new DefaultTermizer();

    @Nullable
    @Override public Object function(@NotNull Compound x, TermBuilder i) {

        try {
            Term t = x.term(0);
            if (t instanceof Atom) {
                String tt = ((Atom)t).toStringUnquoted();
                Class c = Class.forName(tt);
                return $.p(termizer.term(c), termizer.term(c.getMethods()));
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
