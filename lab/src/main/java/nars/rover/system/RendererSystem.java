package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.utils.Bag;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import nars.rover.Sim;
import nars.rover.obj.DrawAbove;
import nars.rover.physics.gl.Box2DJoglPanel;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
public class RendererSystem extends EntitySystem {

    private Sim sim;
    private Bag<Entity> toDraw;

    /**
     * Creates a new EntityProcessingSystem.
     *
     * @param aspect the aspect to match entities
     */
    public RendererSystem(Sim sim) {
        super(Aspect.all(DrawAbove.class));
        this.sim = sim;

        new Box2DJoglPanel(sim) {
            @Override
            protected void draw(GL2 gl, float dt) {

                super.draw(gl, dt);

                World2D ww = this.world;

                //Draw "above" layer
                if (toDraw != null) {
                    for (Entity e : toDraw) {
                        DrawAbove a = e.getComponent(DrawAbove.class);
                        a.drawSky(draw, ww);
                    }
                }
            }

            @Override
            public void windowDestroyed(WindowEvent windowEvent) {
                super.windowDestroyed(windowEvent);
                System.exit(0);
            }
        };
    }

    @Override
    protected void processSystem() {
        toDraw = getEntities();

    }
}
