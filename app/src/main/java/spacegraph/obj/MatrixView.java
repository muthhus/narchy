package spacegraph.obj;

import com.jogamp.opengl.GL2;
import nars.experiment.arkanoid.Arkancide;
import spacegraph.Surface;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class MatrixView extends Surface {

    private final int w;
    private final int h;
    private final ViewFunc view;

    public interface ViewFunc {
        void update(int x, int y, GL2 gl);
    }

    public MatrixView(int w, int h, ViewFunc view) {
        this.w = w;
        this.h = h;
        this.view = view;
    }

    @Override
    protected void paint(GL2 gl) {


        if ((w == 0) || (h == 0))
            return;


        float dw = 1f / w;
        float dh = 1f / h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                view.update(x, y, gl);

//                    gl.glColor3f(r, g, bl);
                Draw.rect(gl, x * dw, 1f - y * dh, dw, dh);

            }
        }


//            //border
//            gl.glColor4f(1f, 1f, 1f, 1f);
//            Draw.strokeRect(gl, 0, 0, tw + dw, th + dh);
    }


}
