/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover;

import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;

/**
 * 
 * @author me
 */
abstract public class RoverWorld implements LayerDraw {


	public final World world;

	public RoverWorld(World w) {
		this.world = w;
	}

	public void addFood(World world, float w, float h, float minSize, float maxSize,
			float mass, Material m) {
		float x = (float) Math.random() * w - w / 2f;
		float y = (float) Math.random() * h - h / 2f;
		float bw = (float) (minSize + Math.random() * (maxSize - minSize));
		float bh = (float) (minSize + Math.random() * (maxSize - minSize));
		float a = 0;
		Body b = addBlock(world, x * 2.0f, y * 2.0f, bw, bh, a, mass);
		b.applyAngularImpulse((float) Math.random());
		b.setUserData(m);
	}

	public Body addWall(World world, float x, float y, float w, float h, float a) {
		Body b = addBlock(world, x, y, w, h, a, 0);
		b.setUserData(Material.wall);
		return b;
	}

	public Body addBlock(World world, float x, float y, float w, float h, float a, float mass) {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(w, h);
		BodyDef bd = new BodyDef();
		if (mass != 0) {
			bd.linearDamping = (0.95f);
			bd.angularDamping = (0.8f);
			bd.type = BodyType.DYNAMIC;
		} else {
			bd.type = BodyType.STATIC;
		}
		bd.position.set(x, y);
		Body body = world.createBody(bd);
		Fixture fd = body.createFixture(shape, mass);
		fd.setRestitution(1f);
		return body;
	}

	@Override
	public void drawGround(JoglAbstractDraw draw, World w) {

	}

	@Override
	public void drawSky(JoglAbstractDraw draw, World w) {

	}
}
