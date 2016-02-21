/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.spacegraph.demo.spacegraph;

import automenta.spacegraph.shape.Rect;
import automenta.spacegraph.ui.GridRect;
import automenta.spacegraph.ui.PointerLayer;
import automenta.vivisect.swing.NWindow;
import nars.util.data.Util;

/**
 *
 * @author seh
 */
public class DemoBlend extends AbstractSurfaceDemo {

    @Override
    public String getName() {
        return "2D Fractal Surface";
    }

    @Override
    public String getDescription() {
        return "";
    }

    public DemoBlend() {
        super();

        add(new GridRect(6, 6));

        /* add rectangles, testing:
        --position
        --size
        --color
        --tilt
         */
        int numRectangles = 512;
        float maxRadius = 0.1f;
        float r = 6.0f;
        float w = 1.5f;
        float h = 0.75f;
        for (int i = 0; i < numRectangles; i++) {
            float s = 1.0f + (float) Math.random() * maxRadius;
            float a = (float) i / 2.0f;
            float x = ((float) Math.random() - 0.5f) * r ;
            float y = ((float) Math.random() - 0.5f) * r ;

            float red = (float)Math.random();
            float green = (float)Math.random();
            float blue = (float)Math.random();

            float heightVariance = 5f;

            Rect r1 = new Rect().color(red, green, blue, 1f/numRectangles + 0.1f);
            r1.center(x, y, Util.sigmoid((float)Math.random()-0.5f)*heightVariance);
            r1.scale(
                0.1f + (float)Math.random() * w,
                0.1f + (float)Math.random() * h);


            add(r1);

        }

        add(new PointerLayer(this));
    }

    public static void main(String[] args) {
        new NWindow(newPanel(new DemoBlend()), 800, 800, true);
    }
}
