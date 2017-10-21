package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.util.Flip;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.DurService;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;

import java.util.List;

public class DynamicConceptSpace extends TermSpace {

    public final NAR nar;
    final Bagregate<Activate> bag;

    private final Flip<List> next = new Flip(FasterList::new);
    final float bagUpdateRate = 0.25f;
    private final int maxNodes;
    private DurService on;


    public TermWidget.TermVis vis;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super();
        vis = new ConceptWidget.ConceptVis2(maxEdgesPerNodeMax);
        this.nar = nar;
        this.maxNodes = maxNodes;

        if (concepts == null)
            concepts = (Iterable) this;

        bag = new Bagregate<Activate>(concepts, maxNodes, bagUpdateRate) {
            @Override
            protected boolean include(Activate x) {
                return DynamicConceptSpace.this.include(x.id.term());
            }

            @Override
            public void onRemove(PriReference<Activate> value) {
                removeNode(value.get());
            }
        };
    }

    void removeNode(Activate concept) {
        if (space != null)
            space.remove(concept.id.term());

//        @Nullable ConceptWidget cw = widgets.getIfPresent(concept.get());
//        if (cw != null) {
//            cw.hide();
//        }
    }
////        cw.delete();
////
////        ConceptWidget cw = concept.remove(this);
////        if (cw!=null) {
////            cw.delete(space.dyn);
////        }
////        return cw;
//    }




    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        on = DurService.build(nar, () -> {
            if (bag.update()) {
                List l = next.write();
                l.clear();
                bag.forEach((concept) -> {
                            ConceptWidget e = conceptWidget(concept.get());
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


  protected ConceptWidget conceptWidget(PriReference<Concept> clink) {
        Concept c = clink.get();
        if (c != null) {

            ConceptWidget cw = (ConceptWidget) space.getOrAdd(c, ConceptWidget.nodeBuilder);

            cw.activate();

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
