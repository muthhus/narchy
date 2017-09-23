package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.control.Activate;
import nars.control.DurService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DynamicConceptSpace extends TermSpace {

    final Bagregate<Activate> bag;

    private final List next;
    final float bagUpdateRate = 0.1f;
    private final DurService on;


    private ConceptWidget.TermVis vis = ConceptWidget.visDefault;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<Activate> concepts, int maxNodes, int bufferedNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super(nar, maxEdgesPerNodeMin, maxEdgesPerNodeMax);


        if (concepts == null)
            concepts = (Iterable) this;

        bag = new Bagregate<Activate>(concepts, maxNodes + bufferedNodes, bagUpdateRate) {
            @Override
            protected boolean include(Activate x) {
                return DynamicConceptSpace.this.include(x.term());
            }

            @Override
            public void onAdd(PriReference<Activate> conceptPLink) {

            }

            @Override
            public void onRemove(@NotNull PriReference<Activate> value) {
                removeNode(value.get());
            }
        };

        next = $.newArrayList();
        on = DurService.build(nar, () -> {
            bag.update();
            next.clear();
            bag.forEach(maxNodes, (concept) -> {
                        ConceptWidget e = conceptWidgetActivation(concept);
                        if (e != null)
                            next.add(e);
                    }
                    //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
            );
            update();
        });
    }

    protected ConceptWidget conceptWidgetActivation(PriReference<Activate> clink) {
        Activate c = clink.get();
        if (c != null) {

            ConceptWidget cw = space.getOrAdd(c.id.term(), ConceptWidget.nodeBuilder);
            cw.concept = c.id;

                cw.activate();
                //cw.pri = clink.priElseZero();
                return cw;

        }
        return null;
    }


    @Override
    protected void get(Collection<TermWidget> displayNext) {

        displayNext.addAll(next);

//        //System.out.println(nar.time() + " " + displayNext.size() );
//        System.out.println("space:" + space.summary());
//        System.out.println("\twidgt:" + widgets.summary());
//        System.out.println("\tedges:" + edges.summary());
//        if (!displayNext.isEmpty())
//            System.out.println(displayNext.iterator().next());
    }


    protected void update() {
        for (int i = 0, activeSize = active.size(); i < activeSize; i++) {
            TermWidget a = active.get(i);
            if (a instanceof ConceptWidget) {
                ((ConceptWidget)a).commit(vis, this);
            }
        }
    }

}
