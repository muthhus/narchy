package nars.term.index;

import nars.concept.Concept;
import nars.nal.meta.match.Ellipsis;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 12/31/15.
 */
public abstract class AbstractMapIndex implements TermIndex {

    protected final SymbolMap atoms;
    protected final TermBuilder builder;
    protected final Function<Term, Concept> conceptBuilder;


    public AbstractMapIndex(TermBuilder termBuilder, Function<Term, Concept> conceptBuilder) {
        super();
        this.builder = termBuilder;
        this.atoms = new SymbolMap(conceptBuilder);
        this.conceptBuilder = conceptBuilder;
    }

    public final Termed get(@NotNull Termed key, boolean createIfMissing) {

        if (key instanceof Ellipsis)
            ///throw new RuntimeException("ellipsis not allowed in this index");
            return null;

//        if (!key.isNormalized()) {
//            Termed u = normalized(key.term());
//            if (u==null) {
//                return null; //this one component could not be conceptualized, this is somewhat normal depending on variable rules
//            } else {
//                key = u.term();
//            }
//        }


//        if (!isInternable(x)) {
//            //TODO intern any subterms which can be
//            return x;
//        }

//        Termed y = the(x);
//        if (y == null) {
//            if ((y = the(x)) !=null) {
//                put(y);
//                if (!y.equals(x))
//                    return x; //return original non-anonymized
//            }
//        }

        return key instanceof Compound ?
                theCompound((Compound) key, createIfMissing)
                : theAtom(key.term(), createIfMissing);
    }


//    /** get the instance that will be internalized */
//    @NotNull
//    public static Termed intern(@NotNull Op op, int relation, @NotNull TermContainer t) {
//        //return (TermMetadata.hasMetadata(t) || op.isA(TermMetadata.metadataBits)) ?
//                //newMetadataCompound(op, relation, t) :
//        return newInternCompound(op, t, relation);
//    }



//    @Nullable
//    public static Term newMetadataCompound(@NotNull Op op, int relation, @NotNull TermContainer t) {
//        //create unique
//        return $.the(op, relation, t);
//    }

//    @NotNull
//    static Termed newInternCompound(@NotNull Op op, @NotNull TermContainer subterms, int relation) {
//        return new GenericCompound(
//            op, relation, (TermVector) subterms
//        );
//    }

//    @Nullable
//    default Termed getOrAdd(@NotNull Termed t) {
//    }

    final Termed theAtom(Term t, boolean createIfMissing) {
        SymbolMap a = this.atoms;
        return (t instanceof Atomic) ?
                (createIfMissing ? a.resolveOrAdd((Atomic)t) : a.resolve((Atomic)t))
                : t;
    }

    abstract protected Termed theCompound(@NotNull Compound x, boolean create);

    @Nullable
    @Override public Termed the(@NotNull Termed t) {
        return get(t, true);
    }

    @Nullable
    @Override public Termed get(@NotNull Termed t) {
        return get(t, false);
    }


//    @Override
//    public abstract Termed getTermIfPresent(Termed t);

//    @Override
//    public abstract void clear();

//    @Override
//    public abstract int subtermsCount();

//    @Override
//    public abstract int size();


    @Override
    public void print(@NotNull PrintStream out) {

        atoms.print(System.out);
        forEach(out::println);

    }

//    @Nullable
//    abstract protected TermContainer get(TermContainer subterms);
//
//
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
