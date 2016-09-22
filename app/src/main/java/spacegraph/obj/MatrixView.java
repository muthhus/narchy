package spacegraph.obj;

import com.jogamp.opengl.GL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.Surface;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class MatrixView extends Surface {

    private static final Logger logger = LoggerFactory.getLogger(MatrixView.class);

    private final int w;
    private final int h;
    private final ViewFunc view;

    public static ViewFunc arrayRenderer(float[][] ww) {
        return (x, y, gl) -> {
            float v = ww[x][y];
            float r, g, b;
            if (v < 0) {
                r = -v / 2f;
                g = 0f;
                b = -v;
            } else {
                r = v;
                g = v / 2;
                b = 0f;
            }
            gl.glColor3f(r, g, b);
            return 0;
        };
    }

    public interface ViewFunc {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        float update(int x, int y, GL2 gl);
    }

    protected MatrixView(int w, int h) {
        this.w = w;
        this.h = h;
        this.view = (ViewFunc)this;
    }

    public MatrixView(int w, int h, ViewFunc view) {
        this.w = w;
        this.h = h;
        this.view = view;
    }

    @Override
    protected void paint(GL2 gl) {

        float h = this.h;
        float w = this.w;

        if ((w == 0) || (h == 0))
            return;

        float dw = 1f / w;
        float dh = 1f / h;


        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                try {
                    float dz = view.update(x, y, gl);
                    Draw.rect(gl, x * dw, 1f - y * dh, dw, dh, dz);
                } catch (Exception e) {
                    logger.error("{}",e);
                    return;
                }

            }
        }

//            //border
//            gl.glColor4f(1f, 1f, 1f, 1f);
//            Draw.strokeRect(gl, 0, 0, tw + dw, th + dh);
    }


}
