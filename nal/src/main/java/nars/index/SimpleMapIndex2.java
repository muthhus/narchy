package nars.index;

import nars.concept.ConceptBuilder;
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

    public SimpleMapIndex2(TermBuilder termBuilder, ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds, Map<TermContainer,TermContainer> subterms) {
        super(termBuilder, conceptBuilder, compounds);
        this.subterms = subterms;
    }


   @Nullable
    @Override
    public final TermContainer theSubterms(TermContainer s) {
       int ss = s.size();
       Term[] bb = new Term[ss];
       boolean changed = false;
       for (int i = 0; i < ss; i++) {
           Term a = s.term(i);

           Term b;
           if (a instanceof Compound) {
               if (a.hasTemporal())
                   return s; //dont store subterm arrays containing temporal compounds

               b = theCompound((Compound)a, true).term();
           } else {
               b = theAtom((Atomic)a, true).term();
           }
           if (a!=b) {
               changed = true;
           }
           bb[i] = b;
       }

       if (changed) {
           s = TermVector.the(bb);
       }

       TermContainer prev = subterms.putIfAbsent(s, s);
       if (prev == null)
           prev = s;
       return prev;
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
}