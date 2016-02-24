package nars.guifx.graph2.source;

import javafx.beans.InvalidationListener;
import nars.NAR;
import nars.guifx.annotation.Implementation;
import nars.guifx.annotation.ImplementationProperty;
import nars.guifx.graph2.GraphSource;
import nars.guifx.graph2.NodeVis;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.impl.CanvasEdgeRenderer;
import nars.guifx.graph2.layout.*;
import nars.guifx.util.POJOPane;

import java.util.function.BiFunction;

import static javafx.application.Platform.runLater;

/**
 * provides defalut settings for a NARGraph view
 */
public class DefaultGrapher extends SpaceGrapher {

    @Implementation(FastOrganicLayout.class)
    @Implementation(HyperOrganicLayout.class)
    @Implementation(HyperassociativeMap2D.class)
    @Implementation(Spiral.class)
    @Implementation(Circle.class)
    @Implementation(Grid.class)
    @Implementation(HyperassociativeMap1D.class)
    @Implementation(Hilbert.class)
    //@Implementation(TimeGraph.class)
    public final ImplementationProperty<IterativeLayout> layoutType = new ImplementationProperty<>();

    public final NAR nar;
    public final POJOPane pojo;



//    public DefaultGrapher(int capacity, ConceptsSource source) {
//        this(
//                source, new DefaultNodeVis(), capacity, edg
//                new CanvasEdgeRenderer());
//    }

    public DefaultGrapher(NAR nar, GraphSource source,
                          NodeVis v,
                          BiFunction<TermNode, TermNode, TermEdge> edgeBuilder,
                          CanvasEdgeRenderer edgeRenderer) {

        super(nar, source, v, edgeBuilder, edgeRenderer);

        this.nar = nar;

        InvalidationListener layoutChange = e -> {
            IterativeLayout il = layoutType.getInstance();

            if (il!=null) {
                setLayout(il);
            } else {
                //layout.set(nullLayout);
            }
        };

        layoutType.addListener(layoutChange);



        pojo = new POJOPane(this);
        pojo.layout();
        pojo.autosize();
        getChildren().add(pojo);



        //layoutChange.invalidated(null); //set current value
        runLater(() -> layoutChange.invalidated(null));



    }

}
