package nars.derive.match;

import nars.Op;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.Unify;
import nars.term.transform.CompoundTransform;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Predicate;

/**
 * Holds results of an ellipsis match and
*/
public class EllipsisMatch extends GenericCompound  {

    //    public static ArrayEllipsisMatch matchedSubterms(Compound Y, IntObjectPredicate<Term> filter) {
//        Function<IntObjectPredicate,Term[]> arrayGen =
//                !(Y instanceof Sequence) ?
//                        Y::terms :
//                        ((Sequence)Y)::toArrayWithIntervals;
//
//        return new ArrayEllipsisMatch(arrayGen.apply( filter ));
//    }


    public final static EllipsisMatch empty = new EllipsisMatch(Term.EmptyArray);


    protected EllipsisMatch(Term[] t) {
        super(Op.PROD, TermVector.the(t));
    }

    @Override
    public Term evalSafe(TermContext context, int remain) {
        //dont eval until it's unwrapped
        return this;
    }

    @Override
    public @Nullable Term transform(CompoundTransform t) {
        //dont eval until it's unwrapped
        return this;
    }

    @Override
    public void init(int[] meta) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term neg() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term unneg() {
        throw new UnsupportedOperationException();
    }

    @Override
    public /*@NotNull*/ Term conceptual() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Term temporalize(Retemporalize r) {
        throw new UnsupportedOperationException();
    }


    public static Term match(Term... matched) {
        switch (matched.length) {
            case 0: return empty;
            case 1: return matched[0]; //if length==1 it should not be an ellipsismatch, just the raw term
            default: return new EllipsisMatch(matched);
        }
    }


    public static Term match(/*@NotNull*/ TermContainer y, int from, int to) {


        if (from == to) {
            return EllipsisMatch.empty;
        }

        return match( y.toArraySubRange(from, to));

//        } else {
//            assert(to == y.size());
//            return ImageMatch.getRemaining(y, from);
//        }

    }

    public static Term match(SortedSet<Term> term) {
        int num = term.size();
        switch (num) {
            case 0: return empty;
            case 1: return term.first();
            default: return new EllipsisMatch(term.toArray(new Term[num]));
        }
    }

    public final boolean forEachWhile(Predicate<? super Term> c) {
        int s = subs();
        for (int i = 0; i < s; i++) {
            if (!c.test(sub(i)))
                return false;
        }
        return true;
    }

    public boolean linearMatch(TermContainer y, int from, /*@NotNull*/ Unify subst) {
        int s = subs();

        if (s + from > y.subs())
            return false; //size mismatch: would extend beyond y's size

        for (int i = 0; i < s; i++) {
            if (!y.sub(from+i).unify(sub(i), subst)) //term mismatch
                return false;
        }
        return true;
    }

//    /** HACK */
//    /*@NotNull*/
//    static Term[] expand(Term raw) {
//        return raw instanceof EllipsisMatch ?
//                ((EllipsisMatch)raw).terms :
//                new Term[] { raw };
//    }

//    public EllipsisMatch(/*@NotNull*/ Collection<Term> term, Term except) {
//        this(term.stream().filter(t -> ((t!=except) )).collect(toList()));
//    }

//    @Deprecated public EllipsisMatch(/*@NotNull*/ Collection<Term> term, Term except, Term except2) {
//        this(term.stream().filter(t -> ((t!=except) && (t!=except2) )).collect(toList()));
//    }



    //abstract public boolean addContained(Compound Y, Set<Term> target);

    @Override
    public boolean isCommutative() {
        //throw new UnsupportedOperationException("it depends");
        return false; //to be careful
    }


    public boolean rematch(/*@NotNull*/ TermContainer y, /*@NotNull*/ Collection<Term> yFree) {
        /*@NotNull*/ TermContainer x = subterms();
        int xs = x.subs();
        for (int i = 0; i < xs; i++) {
            Term e = x.sub(i);
            //if something in this ellipsis was not present in the matchable subterms
            //or if something else has matched it
            if (!y.contains(e) || !yFree.remove(e))
                return false;
        }
        return true;
    }


}
