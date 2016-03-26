package nars.term.index;

import nars.concept.AtomConcept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.$.$;

/**
 * HashMap-backed Symbol Map
 */
public class HashSymbolMap implements SymbolMap {

    public final Map<String,Termed /*AtomConcept*/> map;

    public HashSymbolMap() {
        this(new HashMap(256));
    }

    public HashSymbolMap(Map<String, Termed> map) {
        this.map = map;
    }


    @Override
    public Termed resolve(String id) {
        return map.get(id);
    }

    @Override
    public Termed resolveOrAdd(String s, @NotNull Function<Term, ? extends Termed> conceptBuilder) {

        return map.computeIfAbsent(s,
                S -> (AtomConcept)conceptBuilder.apply($(S)));
    }

    @Override
    public Termed resolveOrAdd(@NotNull Atomic a, @NotNull Function<Term, ? extends Termed> conceptBuilder) {
        return map.computeIfAbsent(a.toString(),
                S -> conceptBuilder.apply(a));
    }

    @Override
    public void print(@NotNull Appendable out) {
        map.forEach((k,v)->{
            try {
                out.append(v.toString());
            } catch (IOException e) {
            }
        });

    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        map.forEach((k,v)->c.accept(v));
    }

}
