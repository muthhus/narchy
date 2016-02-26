package nars.term.index;

import com.google.common.cache.CacheBuilder;
import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.*;
import nars.term.atom.AtomicString;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 1/2/16.
 */
public class MapIndex2 extends AbstractMapIndex {

    private static final int SUBTERM_RELATION = Integer.MIN_VALUE;

    final Atoms atoms = new Atoms();
    final Map<TermVector, IntObjectHashMap> data;
    private final TermBuilder builder;
    int count;

    public MapIndex2(Map<TermVector, IntObjectHashMap> data) {

        this.builder = new TermBuilder();

        this.data = data;

    }

    @Override
    public final TermBuilder builder() {
        return builder;
    }

    @NotNull
    static TermVector vector(@NotNull Term t) {
        return (t.isCompound())  ?
                    (TermVector)((Compound)t).subterms() : null;
    }

    static final Function<TermVector, IntObjectHashMap> groupBuilder =
            (k) -> new IntObjectHashMap(8);

    /** returns previous value */
    private Object putItem(TermVector vv, int index, Object value) {

        IntObjectHashMap g = group(vv);
        Object res = g.put(index, value);
        if (res==null) {
            //insertion
            g.compact();
        }
        return res;
    }

    public IntObjectHashMap group(TermVector vv) {
        return data.computeIfAbsent(vv, groupBuilder);
    }


    @Nullable
    @Override
    public Termed getIfPresent(@NotNull Termed t) {
        return (Termed) getItemIfPresent(
                vector(t.term()), t.opRel());
    }


    @Nullable
    public Object getItemIfPresent(Object vv, int index) {
        IntObjectHashMap group = data.get(vv);
        if (group == null) return null;
        return group.get(index);
    }

    @Nullable
    @Override
    protected TermContainer getSubtermsIfPresent(TermContainer subterms) {
        return (TermContainer) getItemIfPresent(
                subterms, SUBTERM_RELATION);
    }

    @Override
    public void putTerm(@NotNull Termed t) {
        if (t.term() instanceof AtomicString) {
            atoms.putIfAbsent(t.toString(), ()->(AtomConcept)t);
        } else {
            Object replaced = putItem(vector(t.term()), t.opRel(), t);
            if (replaced == null)
                count++;
        }
    }

    @Override
    protected void putSubterms(TermContainer subterms) {

        putItem((TermVector)subterms, SUBTERM_RELATION, subterms);
    }


    @Override
    public void clear() {
        count = 0;
        data.clear();
        //TODO atoms.clear();
    }

    @Override
    public int subtermsCount() {
        return data.size();
    }

    @Override
    public int size() {
        /** WARNING: not accurate */
        return count;
    }



    @Override
    public void forEach(Consumer<? super Termed> c) {
        throw new RuntimeException("unimpl");
    }
}
