package nars.nal.meta.match;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

/**
 * Holds results of an ellipsis match and
 * implements a pre-filter before forming the
 * subterm collection, and post-filter before
 * forming a resulting substituted term.
 */
public final class EllipsisMatch extends TermVector<Term> implements Term {

    //    public static ArrayEllipsisMatch matchedSubterms(Compound Y, IntObjectPredicate<Term> filter) {
//        Function<IntObjectPredicate,Term[]> arrayGen =
//                !(Y instanceof Sequence) ?
//                        Y::terms :
//                        ((Sequence)Y)::toArrayWithIntervals;
//
//        return new ArrayEllipsisMatch(arrayGen.apply( filter ));
//    }


    public EllipsisMatch(Term[] t) {
        super(t);
    }

    public EllipsisMatch(@NotNull Compound y, int from, int to) {
        this(Terms.subRange(y, from, to));
    }

    public EllipsisMatch(@NotNull Collection<Term> term) {
        this(term.toArray(new Term[term.size()]));
    }

//    public EllipsisMatch(@NotNull Collection<Term> term, Term except) {
//        this(term.stream().filter(t -> ((t!=except) )).collect(toList()));
//    }

//    @Deprecated public EllipsisMatch(@NotNull Collection<Term> term, Term except, Term except2) {
//        this(term.stream().filter(t -> ((t!=except) && (t!=except2) )).collect(toList()));
//    }



    //abstract public boolean addContained(Compound Y, Set<Term> target);

    @NotNull
    @Override
    public Op op() {
        return Op.NONE;
    }

    @Override
    public void recurseTerms(@NotNull SubtermVisitor v, Compound parent) {
        forEach(x -> v.accept(x, parent));
    }

    @Override
    public boolean isCommutative() {
        return false;
    }

    @Override
    public void append(@NotNull Appendable w) throws IOException {
        w.append(toString());
    }


    public boolean addWhileMatching(@NotNull Compound y, @NotNull Collection<Term> target, int min) {
        for (Term e : term) {
            if (!y.containsTerm(e)) return false;
            if (!target.add(e))
                return false;
        }
        return (target.size() >= min);
    }


}
