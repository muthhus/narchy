package nars.index;

import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Map;

/** additionally caches subterm vectors */
public class SimpleMapIndex2 extends SimpleMapIndex {

    private final Map<TermContainer, TermContainer> subterms;

    public SimpleMapIndex2(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds, Map<TermContainer,TermContainer> subterms) {
        super(termBuilder, conceptBuilder, compounds);
        this.subterms = subterms;
    }

    @Override
    public int subtermsCount() {
        return subterms.size(); //unsupported
    }

    @NotNull
    @Override
    public String summary() {
        return
                data.size() + " concepts, " +
                subterms.size() + " subterms, " +
                ((HashSymbolMap)atoms).map.size() + " atoms";
    }

    @Override
    public void print(@NotNull PrintStream out) {

        super.print(out);

        //subterms.forEach((k,v) -> System.out.println(k + "\t" + v));
        //data.forEach((k,v) -> System.out.println(k + "\t" + v));

        data.keySet().forEach(System.out::println);

    }

    @Override
    protected TermContainer putIfAbsent(TermContainer x, TermContainer y) {
        return subterms.putIfAbsent(x, y);
    }
}