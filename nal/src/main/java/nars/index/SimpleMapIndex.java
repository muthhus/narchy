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
public class SimpleMapIndex extends AbstractMapIndex {

    public final Map<Termed,Termed> data;

    public SimpleMapIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds) {
        super(termBuilder, conceptBuilder);
        this.data = compounds;
    }


    @Nullable
    @Override
    protected final Termed theCompound(@NotNull Compound x, boolean create) {
        //??
//            Termed existing = data.get(x);
//            if (existing!=null)
//                return existing;
//
//            Termed c = internCompound(internSubterms(x.subterms(), x.op(), x.relation(), x.dt()));
//            data.put(c, c);
//            return c;
        return create ?
                theCompoundCreated(x) :
                data.get(x);
    }

    private final Termed theCompoundCreated(@NotNull Compound x) {

//        if (x.hasTemporal()) {
//            x = theTemporalCompound(x);
//            return x;
//        }

        Termed y = data.get(x);
        if (y == null) {
            y = internCompound(x.subterms(), x.op(), x.relation(), x.dt());
            if (!(y.term() instanceof Compound && y.term().hasTemporal())) {
                y = internCompound(y);
                data.put(y, y);
            }
        }
        return y;

        //doesnt work due to recursive concurrent modification exception:
//        return data.computeIfAbsent(x, (X) -> {
//            Compound XX = (Compound) X; //??
//            return internCompound(internCompound(XX.subterms(), XX.op(), XX.relation(), XX.dt()));
//        });
    }

    @Nullable
    @Override
    public final Termed set(@NotNull Termed t) {
        Termed existing = data.putIfAbsent(t, t);
        if ((existing !=null) && (existing!=t))
            throw new RuntimeException("pre-existing value");
        return t;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void forEach(@NotNull Consumer c) {
        data.forEach((k,v)-> c.accept(v));
    }

    @Override
    public int size() {
        return data.size() /* + atoms.size? */;
    }


    @Nullable
    @Override
    public TermContainer theSubterms(TermContainer s) {
        return s;
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