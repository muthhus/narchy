package nars.index;

import nars.concept.Concept;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** implements AbstractMapIndex with one ordinary map implementation. does not cache subterm vectors */
abstract public class SimpleMapIndex extends MaplikeIndex {

    protected final Map<Termed,Termed> concepts;

    public SimpleMapIndex(Concept.ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds) {
        super(conceptBuilder);
        this.concepts = compounds;
    }

    @NotNull
    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return concepts.computeIfAbsent(x, this::buildConcept);
    }

    @Override
    public Termed remove(Termed entry) {
        return concepts.remove(entry);
    }


    @Override public final Termed get(@NotNull Termed x) {
        return concepts.get(x);
    }


    @Nullable
    @Override
    public final void set(@NotNull Termed src, Termed target) {
        concepts.put(src, target);


        //return data.putIfAbsent(src, target);
//        /*if ((existing !=null) && (existing!=target))
//            throw new RuntimeException("pre-existing value");*/
//        return target;
    }

    @Override
    public void clear() {
        concepts.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return concepts.size() /* + atoms.size? */;
    }



    @Override
    public int subtermsCount() {
        return -1; //unsupported
    }

    @NotNull
    @Override
    public String summary() {
        return concepts.size() + " concepts ";
    }
}