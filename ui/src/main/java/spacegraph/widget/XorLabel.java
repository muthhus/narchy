package spacegraph.widget;

import com.jogamp.opengl.GL2;

import static com.jogamp.opengl.GL.GL_COLOR_LOGIC_OP;
import static com.jogamp.opengl.GL.GL_INVERT;

/** label which paints in XOR so it contrasts with what is underneath */
public class XorLabel extends Label {
    @Override
    public void paint(GL2 gl) {
        //https://www.opengl.org/discussion_boards/showthread.php/179116-drawing-using-xor
        gl.glEnable(GL_COLOR_LOGIC_OP);
        gl.glLogicOp(GL_INVERT);

        super.paint(gl);
        gl.glDisable(GL_COLOR_LOGIC_OP);
    }
}
