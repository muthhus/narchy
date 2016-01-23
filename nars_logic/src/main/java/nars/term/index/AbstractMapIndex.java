package nars.term.index;

import nars.$;
import nars.Op;
import nars.nal.Tense;
import nars.term.*;
import nars.term.compound.Compound;
import nars.term.compound.GenericCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 12/31/15.
 */
public abstract class AbstractMapIndex implements TermIndex {
    public AbstractMapIndex() {
        super();
    }

    public static boolean isInternable(Term t) {
        return true; //!TermMetadata.hasMetadata(t);
    }

    /** get the instance that will be internalized */
    @NotNull
    public static Termed intern(@NotNull Op op, int relation, TermContainer t) {
        //return (TermMetadata.hasMetadata(t) || op.isA(TermMetadata.metadataBits)) ?
                //newMetadataCompound(op, relation, t) :
        return newInternCompound(op, t, relation);
    }

    @Nullable
    public static Term newMetadataCompound(@NotNull Op op, int relation, TermContainer t) {
        //create unique
        return $.the(op, relation, t);
    }

    @NotNull
    protected static Termed newInternCompound(@NotNull Op op, TermContainer subterms, int relation) {
        return new GenericCompound(
            op, relation, (TermVector) subterms
        );
    }

    @Nullable
    @Override
    public Termed the(Term x) {

        if (!isInternable(x)) {
            //TODO intern any subterms which can be
            return x;
        }

        Termed y = getTermIfPresent(x);
        if (y == null) {
            putTerm(y = makeTerm(x));
            if (!y.equals(x))
                return x; //return original non-anonymized
        }

        return y;
    }

//    @Override
//    public abstract Termed getTermIfPresent(Termed t);

//    @Override
//    public abstract void clear();

//    @Override
//    public abstract int subtermsCount();

//    @Override
//    public abstract int size();

    @NotNull
    @Override
    public Termed make(@NotNull Op op, int relation, TermContainer t, int dt) {
        Termed x = intern(op, relation, internSub(t));
        if (dt!= Tense.ITERNAL && x.term().isCompound()) {
            x = ((Compound)x.term()).t(dt);
        }
        return x;
    }

    @Override public Termed makeAtomic(Term t) {
        return t;
    }

    @Nullable
    @Override public TermContainer internSub(TermContainer s) {
        TermContainer existing = getSubtermsIfPresent(s);
        if (existing == null) {
            putSubterms(unifySubterms(s));
            return s;
        }
        return existing;
    }


    abstract protected void putSubterms(TermContainer subterms);
    @Nullable
    abstract protected TermContainer getSubtermsIfPresent(TermContainer subterms);


//    @Override
//    public void print(PrintStream out) {
//        BiConsumer itemPrinter = (k, v) -> System.out.println(v.getClass().getSimpleName() + ": " + v);
//        forEach(d -> itemPrinter);
//        System.out.println("--");
//        subterms.forEach(itemPrinter);
//    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);
}
