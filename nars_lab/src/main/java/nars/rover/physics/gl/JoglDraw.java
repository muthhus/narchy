package nars.rover.physics.gl;

import nars.rover.physics.PhysicsCamera;
import nars.rover.physics.PhysicsController;
import org.jbox2d.common.Vec2;
import org.jbox2d.particle.ParticleColor;

/**
 *
 */
public class JoglDraw extends JoglAbstractDraw {
    private final JoglAbstractPanel panel;

    public JoglDraw(JoglAbstractPanel panel) {
        this.panel = panel;
    }

    @Override
    protected PhysicsCamera getPhysicsCamera() {
        PhysicsCamera p = null;

        PhysicsController controller = panel.controller;
        if( controller != null ) {
            p = controller.getCamera();
        }

        return p;
    }

    @Override
    public void drawParticles(Vec2[] centers, float radius, ParticleColor[] colors, int count) {

    }

    @Override
    public void drawParticlesWireframe(Vec2[] centers, float radius, ParticleColor[] colors, int count) {

    }
}
