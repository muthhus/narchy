package nars.rover.physics.gl;

import automenta.spacegraph.Space2D;
import automenta.spacegraph.demo.spacegraph.DemoTextButton;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import nars.rover.physics.PhysicsController;
import nars.rover.physics.TestbedState;
import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.dynamics.World;

/* generic box2d physics-executing panel */
public class Box2DJoglPanel extends AbstractJoglPanel {
	public final PhysicsController controller;

	final Space2D sg = new DemoTextButton().getSpace();
	public final JoglDraw draw;

	public Box2DJoglPanel(final World world, final TestbedState model,
                          final PhysicsController controller, GLCapabilitiesImmutable config) {
		super(world, controller, model, config);
		this.controller = controller;
		this.draw = new JoglDraw(this);

	}

	@Override
	protected void draw(GL2 gl, float dt) {
		if (world!=null) {
			//ex: layer #1
			draw.draw(world, dt);
		}

        //ex. layer #2
		gl.glPushMatrix();
		gl.glScalef(8f,8f,8f); //HACK scale shouldnt be applied, keep them in flat scalespace
		sg.draw(gl);
		gl.glPopMatrix();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		super.reshape(drawable, x, y, width, height);

		if (controller != null) {
			controller.updateExtents(width / 2f, height / 2f);
		}
	}

}
