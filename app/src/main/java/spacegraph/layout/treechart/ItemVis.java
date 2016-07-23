package spacegraph.layout.treechart;

import com.jogamp.opengl.GL2;
import spacegraph.render.Draw;

/**
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class ItemVis<X> {

    public final String label;
    public final X item;
    public double left;
    public double top;
    public double width;
    public double height;
    public double area;
    private float r;
    private float g;
    private float b;

    public ItemVis(X item, String label) {
        this.item = item;
        this.label = label;
    }

    public void update(float weight) {
        this.area = weight;
        this.r = -1;
    }

//    public void update(X item, String label, float weight) {
//        this.item = item;
//        this.label = label;
//        this.area= weight;
//        this.r = -1; //auto
//    }

    public void update(float weight, float r, float g, float b) {
        this.area= weight;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public String toString() {
        return "TreemapDtoElement{" +
                "label='" + label + '\'' +
                ", area=" + area +
                ", top=" + top +
                ", left=" + left +
                '}';
    }

    void setArea(double area) {
        this.area = area;
    }

//    boolean isContainer() {
//        return item.isContainer();
//    }

    @Override
    public boolean equals(Object o) {
        return this == o;

        //if (o == null || getClass() != o.getClass()) return false;

//        ItemVis that = (ItemVis) o;
//
//        if (item != null ? !item.equals(that.item) : that.item != null) return false;
//        return !(label != null ? !label.equals(that.label) : that.label != null);

    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
//        int result = label != null ? label.hashCode() : 0;
//        result = 31 * result + (item != null ? item.hashCode() : 0);
//        return result;
    }

    public void paint(GL2 gl, double percent) {
        float i = 0.25f + 0.75f * (float)percent;

        if (r < 0) {
            r = i;
            g = 0.1f;
            b = 0.1f;
        }

        gl.glColor3f(r, g, b);

        Draw.rect(gl,
            (float)left, (float)top,
            (float)width, (float)height
        );

        gl.glColor3f(1,1,1);
        float labelSize = (float) (height / 4f * Math.min(0.005f,percent));
        Draw.renderLabel(gl,
                labelSize, labelSize, //label size
                label, (float)(left+width/2f), (float)(top+height/2f), 0.5f);

    }
}
