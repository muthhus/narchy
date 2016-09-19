package nars.video;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera extends ImageCamera {

    private final Container component;

    public Rectangle selection;


    public SwingCamera(Container component) {
        this.component = component;
        input(0, 0, component.getWidth(), component.getHeight());
        update();
    }

//    final AtomicBoolean ready = new AtomicBoolean(true);

    public void update() {
//        if (ready.compareAndSet(true, false)) {
            //SwingUtilities.invokeLater(() -> {
//                ready.set(true);
                out = ScreenImage.get(component, out, selection);
            //});
//        }
    }



    public void input(int x, int y, int w, int h) {
        this.selection = new Rectangle(x, y, w, h);
    }


    public int inWidth() {
        return component.getWidth();
    }

    public int inHeight() {
        return component.getHeight();
    }


    /** x and y in 0..1.0, w and h in 0..1.0 */
    public void input(float x, float y, float w, float h) {
        int px = Math.round(inWidth() * x);
        int py = Math.round(inHeight() * y);
        int pw = Math.round(inWidth() * w);
        int ph = Math.round(inWidth() * h);
        input(px, py, pw, ph);
    }
}
