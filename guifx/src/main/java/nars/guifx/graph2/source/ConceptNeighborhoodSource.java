package nars.guifx.graph2.source;

import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.graph2.ConceptsSource;
import nars.link.BLink;
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
    final int termLinkNeighbors = 16;

    public ConceptNeighborhoodSource(NAR nar, Termed... c) {
        super(nar);
        this.roots = Lists.newArrayList(c);
    }

    final Set<Termed> conceptsSet = $.newHashSet(1);

    final Consumer<BLink<? extends Termed>> onLink = n -> {
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
