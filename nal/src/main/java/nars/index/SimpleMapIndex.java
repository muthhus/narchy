package nars.index;

import nars.concept.Concept;
import nars.term.Compound;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** implements AbstractMapIndex with one ordinary map implementation. does not cache subterm vectors */
@Deprecated abstract public class SimpleMapIndex extends MaplikeIndex {

    public final Map<Termed,Termed> data;

    public SimpleMapIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds) {
        super(termBuilder, conceptBuilder);
        this.data = compounds;
    }


    @Override
    public Termed remove(Termed entry) {
        return data.remove(entry);
    }


    @Override public final Termed get(@NotNull Termed x) {
        return data.get(x);
    }


    @Nullable
    @Override
    public final Termed set(@NotNull Termed src, Termed target) {
        data.put(src, target);
        return target;

        //return data.putIfAbsent(src, target);
//        /*if ((existing !=null) && (existing!=target))
//            throw new RuntimeException("pre-existing value");*/
//        return target;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        data.forEach((k,v)-> c.accept(v));
    }

    @Override
    public int size() {
        return data.size() /* + atoms.size? */;
    }



    @Override
    public int subtermsCount() {
        return -1; //unsupported
    }

    @NotNull
    @Override
    public String summary() {
        return data.size() + " concepts, " + ((HashSymbolMap)atoms).map.size() + " atoms";
    }
}