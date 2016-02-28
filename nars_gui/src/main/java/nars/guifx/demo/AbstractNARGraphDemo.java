package nars.guifx.demo;

import nars.guifx.graph2.ConceptsSource;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.impl.HalfHalfIsoTriangleCanvasEdgeRenderer;
import nars.guifx.graph2.impl.HalfHalfLineCanvasEdgeRenderer;
import nars.guifx.graph2.impl.HexButtonVis;
import nars.guifx.graph2.impl.SubButtonVis;
import nars.guifx.graph2.layout.Grid;
import nars.guifx.graph2.layout.Spiral;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.guifx.graph2.source.DefaultGrapher;
import nars.guifx.graph2.source.SpaceGrapher;
import nars.guifx.util.TabX;
import nars.nar.Default;

import java.util.function.Consumer;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 8/15/15.
 */
public abstract class AbstractNARGraphDemo {


    public static SpaceGrapher newGraph(Default n) {


//        new BagForgettingEnhancer(n.memory, n.core.concepts(),
//                0f, 0.8f, 0.8f);


//        n.memory.conceptForgetDurations.setValue(8);
//        n.memory.termLinkForgetDurations.setValue(12);
//        n.memory.taskLinkForgetDurations.setValue(12);

//        //n.input(new File("/tmp/h.nal"));
//        n.input("<hydochloric --> acid>.");
//        n.input("<#x-->base>. %0.65%");
//        n.input("<neutralization --> (acid,base)>. %0.75;0.90%");
//        n.input("<(&&, <#x --> hydochloric>, eat:#x) --> nice>. %0.75;0.90%");
//        n.input("<(&&,a,b,ca)-->#x>?");




        DefaultGrapher g = new DefaultGrapher(
                n,

                new ConceptsSource(n),

                /*
                new ConceptNeighborhoodSource(n,
                        n.concept("<a --> b>"),
                        n.concept("<b --> c>")
                ),
                */

                /*new JGraphSource(n, GraphPaneTest.newExampleTermLinkGraph() ) {

                    @Override
                    public Termed getTargetVertex(Termed edge) {
                        //System.out.println("? target vertex of " + edge + " " + edge.getClass());
                        return edge;
                    }
                },*/

                //new SubButtonVis(n),
                new HexButtonVis(n),
                //new DefaultNodeVis(),

                (A, B) -> {
                    return new TermEdge(A, B) {
                        @Override
                        public double getWeight() {
                            //return ((Concept)A.term).getPriority();
                            return pri;
                        }
                    };
                    //return $.pro(A.getTerm(), B.getTerm());
                },

                //new HalfHalfLineCanvasEdgeRenderer()
                new HalfHalfIsoTriangleCanvasEdgeRenderer()
                //new BlurCanvasEdgeRenderer()
        );

        //initial settings

//        runLater(()-> {
//            g.setLayout(new Grid());
//        });

        //g.setLayout(HyperassociativeMap2D.class);
        //g.pan(2000,2000);

        //g.start(100);

        return g;
    }


    public static void graphIDE(Default n) {
        graphIDE(n, null);
    }
    public static void graphIDE(Default n, Consumer<NARide> onReady) {
        NARide.show(n.loop(), ide -> {

            //ide.addView(new IOPane(n));
            ide.content.getTabs().setAll(
                    new TabX("Graph", newGraph(n)));


            ide.setSpeed(150);
            //n.frame(5);

            if (onReady!=null)
                onReady.accept(ide);

        });
    }
}
