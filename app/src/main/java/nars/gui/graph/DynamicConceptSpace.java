package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.util.Flip;
import nars.NAR;
import nars.control.Activate;
import nars.control.DurService;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;

import java.util.List;

public class DynamicConceptSpace extends TermSpace {

    final Bagregate<Activate> bag;

    private final Flip<List> next = new Flip(FasterList::new);
    final float bagUpdateRate = 0.25f;
    private final int maxNodes;
    private DurService on;


    private ConceptWidget.TermVis vis = ConceptWidget.visDefault;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super(nar, maxEdgesPerNodeMin, maxEdgesPerNodeMax);

        this.maxNodes = maxNodes;

        if (concepts == null)
            concepts = (Iterable) this;

        bag = new Bagregate<Activate>(concepts, maxNodes, bagUpdateRate) {
            @Override
            protected boolean include(Activate x) {
                return DynamicConceptSpace.this.include(x.id.term());
            }

            @Override
            public void onAdd(PriReference<Activate> conceptPLink) {

            }

            @Override
            public void onRemove(PriReference<Activate> value) {
                removeNode(value.get());
            }
        };
    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        on = DurService.build(nar, () -> {
            if (bag.update()) {
                List l = next.write();
                l.clear();
                bag.forEach((concept) -> {
                            ConceptWidget e = conceptWidgetActivation(concept);
                            if (e != null) {
                                e.commit(vis, this);
                                l.add(e);
                            }
                        }
                        //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
                );
                next.commit();
            }
        });
    }

    @Override
    public synchronized void stop() {
        on.stop();
        on = null;
        super.stop();
    }

    protected ConceptWidget conceptWidgetActivation(PriReference<Activate> clink) {
        Activate c = clink.get();
        if (c != null) {

            ConceptWidget cw = space.getOrAdd(c.id.term(), ConceptWidget.nodeBuilder);
            cw.concept = c.id;


            //cw.pri = clink.priElseZero();
            return cw;

        }
        return null;
    }

    @Override
    protected List<TermWidget> get() {
        List<TermWidget> r = next.read();
        return r;
    }


}
