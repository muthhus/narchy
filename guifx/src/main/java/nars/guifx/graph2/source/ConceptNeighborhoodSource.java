package nars.guifx.graph2.source;

import com.google.common.collect.Lists;
import nars.Global;
import nars.NAR;
import nars.bag.ArrayBLink;
import nars.concept.Concept;
import nars.guifx.graph2.ConceptsSource;
import nars.term.Termed;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Includes the termlinked and tasklinked concepts of a set of
 * root concepts
 */
public class ConceptNeighborhoodSource extends ConceptsSource {

    private final ArrayList<Termed> roots;
    int termLinkNeighbors = 16;

    public ConceptNeighborhoodSource(NAR nar, Termed... c) {
        super(nar);
        this.roots = Lists.newArrayList(c);
    }

    final Set<Termed> conceptsSet = Global.newHashSet(1);

    final Consumer<ArrayBLink<? extends Termed>> onLink = n -> {
        Termed tn = n.get();
        conceptsSet.add(
                (tn instanceof Concept) ? tn : nar.concept(tn));
    };

    @Override
    public void commit() {

        roots.forEach(r -> {
            conceptsSet.add(r);
            if (!(r instanceof Concept)) return;

            Concept c = (Concept) r;

            c.tasklinks().forEach(termLinkNeighbors, onLink);
            c.termlinks().forEach(termLinkNeighbors, onLink);
            //concepts::add);
        });

        commit(conceptsSet);

        //System.out.println(concepts);



    }
}
