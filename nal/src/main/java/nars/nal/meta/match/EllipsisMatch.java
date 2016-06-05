package nars.nal.meta.match;

import nars.Op;
import nars.term.*;
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
public final class EllipsisMatch extends TermVector implements Term {

    //    public static ArrayEllipsisMatch matchedSubterms(Compound Y, IntObjectPredicate<Term> filter) {
//        Function<IntObjectPredicate,Term[]> arrayGen =
//                !(Y instanceof Sequence) ?
//                        Y::terms :
//                        ((Sequence)Y)::toArrayWithIntervals;
//
//        return new ArrayEllipsisMatch(arrayGen.apply( filter ));
//    }


    final static EllipsisMatch empty = new EllipsisMatch(Terms.empty);

    protected EllipsisMatch(Term[] t) {
        super(t);
    }

    public static Term match(@NotNull Term[] matched) {
        switch (matched.length) {
            case 0: return empty;
            case 1: return matched[0]; //if length==1 it should not be an ellipsismatch, just the raw term
            default: return new EllipsisMatch(matched);
        }
    }



    public static Term match(@NotNull Compound y, int from, int to) {
        return match(Terms.subRange(y, from, to));
    }

    public static Term match(@NotNull Collection<Term> term) {
        switch (term.size()) {
            case 0: return empty;
            case 1: return term.iterator().next();
            default: return new EllipsisMatch(term.toArray(new Term[term.size()]));
        }
    }

    /** HACK */
    @NotNull
    static Term[] expand(Term raw) {
        return raw instanceof EllipsisMatch ?
                ((EllipsisMatch)raw).term :
                new Term[] { raw };
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
    public void recurseTerms(@NotNull SubtermVisitorX v, Compound parent) {
        forEach(x -> x.recurseTerms(v, null /*this*/));
    }
    @Override
    public void recurseTerms(@NotNull SubtermVisitor v) {
        forEach(x -> x.recurseTerms(v));
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
