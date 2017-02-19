/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.gui.output.graph.layout;

import nars.entity.Concept;
import nars.entity.Item;
import nars.gui.util.Video;
import nars.gui.util.graph.AbstractGraphVis;
import nars.gui.util.graph.EdgeVis;
import nars.gui.util.graph.GraphDisplay;
import nars.gui.util.graph.VertexVis;

/**
 * Item Hash = theta, Priority = radius
 */
public class HashPriorityPolarLayout implements GraphDisplay<Item, Object> {

    //# of radians to cover
    float arcStart, arcStop;
    float spacing;

    public HashPriorityPolarLayout(float arcStart, float arcStop, float spacing) {
        this.arcStart = arcStart;
        this.arcStop = arcStop;
        this.spacing = spacing;
    }

    @Override
    public void vertex(AbstractGraphVis<Item, Object> g, VertexVis<Item, Object> v) {
        Item vertex = v.getVertex();

        float priority = vertex.getPriority();
        double radius = (1.0 - priority) * spacing + 8;

        Object x = vertex;
        if (vertex instanceof Concept) {
            x = ((Concept) vertex).getTerm();
        }

        float angle = ((arcStop - arcStart) * Video.hashFloat(x.hashCode()) + arcStart) * ((float) Math.PI * 2f);
        v.tx = (float) (Math.cos(angle) * radius) * spacing;
        v.ty = (float) (Math.sin(angle) * radius) * spacing;

    }

    @Override
    public void edge(AbstractGraphVis<Item, Object> g, EdgeVis<Item, Object> e) {

    }

}
