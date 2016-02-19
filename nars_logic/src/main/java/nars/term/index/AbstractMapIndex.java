package nars.term.index;

import nars.$;
import nars.Op;
import nars.nal.Tense;
import nars.nal.meta.match.Ellipsis;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
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


    /** get the instance that will be internalized */
    @NotNull
    public static Termed intern(@NotNull Op op, int relation, @NotNull TermContainer t) {
        //return (TermMetadata.hasMetadata(t) || op.isA(TermMetadata.metadataBits)) ?
                //newMetadataCompound(op, relation, t) :
        return newInternCompound(op, t, relation);
    }

    @Nullable
    public static Term newMetadataCompound(@NotNull Op op, int relation, @NotNull TermContainer t) {
        //create unique
        return $.the(op, relation, t);
    }

    @NotNull
    protected static Termed newInternCompound(@NotNull Op op, @NotNull TermContainer subterms, int relation) {
        return new GenericCompound(
            op, relation, (TermVector) subterms
        );
    }

    @Nullable
    @Override
    public Termed the(Term x) {

        if (x instanceof Ellipsis)
            ///throw new RuntimeException("ellipsis not allowed in this index");
            return null;

//        if (!isInternable(x)) {
//            //TODO intern any subterms which can be
//            return x;
//        }

        Termed y = getTermIfPresent(x);
        if (y == null) {
            if ((y = makeTerm(x)) !=null) {
                putTerm(y);
                if (!y.equals(x))
                    return x; //return original non-anonymized
            }
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

    @Nullable
    @Override
    public Termed make(@NotNull Op op, int relation, TermContainer t, int dt) {
        @Nullable TermContainer subs = internSub(t);
        if (subs == null)
            return null;
        Termed x = intern(op, relation, subs);
        if (dt!= Tense.ITERNAL && x!=null && x.term().isCompound()) {
            x = ((Compound)x.term()).dt(dt);
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
            TermContainer us = unifySubterms(s);
            if (us !=null) {
                putSubterms(us);
                existing = s;
            }
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
