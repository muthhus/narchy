package spacegraph.obj;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.ShapeDrawer;

/**
 * Created by me on 6/27/16.
 */
public class CrosshairSurface extends Surface implements MouseListener {

    private final SpaceGraph space;
    int mx, my;
    boolean mouseEnabled;
    private float smx, smy;

    public CrosshairSurface(SpaceGraph s) {
        this.space = s;
    }



    @Override
    protected void paint(GL2 gl) {
        gl.glPushMatrix();

        if (!mouseEnabled) {
            //HACK
            space.window.addMouseListener(this);
            mouseEnabled = true;
        }



        float cw = 175f; //TODO proportional to ortho width (pixels)
        float ch = 175f; //TODO proportional to ortho height (pixels)

        //gl.glTranslatef(mx, my, 0);
        gl.glColor4f(0.2f, 0.8f, 0f, 0.6f);
        gl.glLineWidth(4f);
        ShapeDrawer.strokeRect(gl, smx-cw/2f, smy-ch/2f, cw, ch);

        float hl = 1.25f; //cross hair length
        ShapeDrawer.line(gl, smx, smy-ch*hl, smx, smy+ch*hl);
        ShapeDrawer.line(gl, smx-cw*hl, smy, smx+cw*hl, smy);

        gl.glPopMatrix();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        update(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        update(e);
    }

    private void update(MouseEvent e) {
        mx = e.getX();
        GLWindow win = space.window;
        smx = ((float)mx) ;
        my = e.getY();
        smy = (win.getHeight() - ((float)my)) ;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        update(e);
    }
}
