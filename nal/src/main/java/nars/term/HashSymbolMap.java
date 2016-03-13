package nars.term;

import nars.$;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.atom.Atomic;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static nars.$.*;

/**
 * HashMap-backed Symbol Map
 */
public class HashSymbolMap implements SymbolMap {

    public final Map<String,AtomConcept> map;

    public HashSymbolMap() {
        this(new HashMap(256));
    }

    public HashSymbolMap(Map<String, AtomConcept> map) {
        this.map = map;
    }


    @Override
    public AtomConcept resolve(String id) {
        return map.get(id);
    }

    @Override
    public AtomConcept resolveOrAdd(String s, Function<Term, Concept> conceptBuilder) {

        return map.computeIfAbsent(s,
                S -> (AtomConcept)conceptBuilder.apply($(S)));
    }

    @Override
    public AtomConcept resolveOrAdd(Atomic a, Function<Term, Concept> conceptBuilder) {
        return map.computeIfAbsent(a.toString(),
                S -> (AtomConcept)conceptBuilder.apply(a));
    }

    @Override
    public void print(Appendable out) {
        map.forEach((k,v)->{
            try {
                out.append(v.toString());
            } catch (IOException e) {
            }
        });

    }
}
