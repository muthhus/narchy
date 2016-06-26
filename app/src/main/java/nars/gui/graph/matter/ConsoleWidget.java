package nars.gui.graph.matter;


import bulletphys.dynamics.RigidBody;
import bulletphys.ui.GLConsole;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.gui.graph.Atomatter;
import nars.gui.graph.FixedAtomatterList;
import nars.gui.graph.GraphSpace;

public class ConsoleWidget extends Atomatter<VirtualTerminal> {

    private final GLConsole console;

    public ConsoleWidget(VirtualTerminal k) {
        super(k, 0);
        this.console = new GLConsole(k, 0.05f);
        scale(1f,1f,1f);
        pri = 1f;

    }

    @Override
    protected void renderRelative(GL2 gl, RigidBody body) {

        super.renderRelative(gl, body);

        gl.glPushMatrix();
        gl.glTranslatef(0, 0, radius);
        console.render(gl);
        gl.glPopMatrix();
    }

    public static void main(String[] args) {
        new GraphSpace<VirtualTerminal>(
                vt -> {
                    return new ConsoleWidget(vt);
                }, new FixedAtomatterList<>(
                    new DefaultVirtualTerminal(80,25),
                    new DefaultVirtualTerminal(40,16)
                )
        ).show(800,800);
    }
}
