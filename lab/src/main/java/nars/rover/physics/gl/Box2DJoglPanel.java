package nars.rover.physics.gl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import nars.rover.Sim;
import org.jbox2d.dynamics.World2D;

/* generic box2d physics-executing panel */
public class Box2DJoglPanel extends AbstractJoglPanel {


	//final Space2D sg = new DemoTextButton().getSpace();
	public final JoglDraw draw;

	public Box2DJoglPanel(final Sim sim) {
		this(sim.world);
	}

	public Box2DJoglPanel(final World2D world) {
		super(world, newDefaultConfig());
		this.draw = new JoglDraw(this);
	}

	private static GLCapabilitiesImmutable newDefaultConfig() {

		GLCapabilities config = new GLCapabilities(GLProfile.getMaximum(true)); //getDefault());

		config.setHardwareAccelerated(true);
//        config.setBackgroundOpaque(false);

		config.setAlphaBits(8);
		config.setAccumAlphaBits(8);
		config.setAccumRedBits(8);
		config.setAccumGreenBits(8);
		config.setAccumBlueBits(8);
		return config;
	}

	@Override
	protected void draw(GL2 gl, float dt) {
		if (world!=null) {
			//ex: layer #1
			draw.draw(world, dt);
		}

//        //ex. layer #2
//		gl.glPushMatrix();
//		gl.glScalef(8f,8f,8f); //HACK scale shouldnt be applied, keep them in flat scalespace
//		sg.draw(gl);
//		gl.glPopMatrix();
	}

//	@Override
//	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
//		super.reshape(drawable, x, y, width, height);
//	}

}
