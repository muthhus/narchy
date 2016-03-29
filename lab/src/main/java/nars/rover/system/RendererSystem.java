package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.utils.Bag;
import com.gs.collections.api.block.procedure.Procedure;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import nars.rover.Sim;
import nars.rover.obj.DrawAbove;
import nars.rover.physics.gl.Box2DJoglPanel;
import nars.rover.physics.j2d.LayerDraw;
import nars.util.data.list.FasterList;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
public class RendererSystem extends EntitySystem {

    private Sim sim;
    private FasterList<LayerDraw> toDraw = new FasterList();

    /**
     * Creates a new EntityProcessingSystem.
     *
     * @param aspect the aspect to match entities
     */
    public RendererSystem(Sim sim) {
        super(Aspect.all(DrawAbove.class));
        this.sim = sim;

        new Box2DJoglPanel(sim) {

            final Procedure<LayerDraw> drawProc = (LayerDraw l) -> l.drawSky(draw, world);

            @Override
            protected void draw(GL2 gl, float dt) {

                super.draw(gl, dt);

                //Draw "above" layer
                toDraw.forEach(drawProc);

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

        toDraw.clear();
        Bag<Entity> ee = getEntities();
        if (ee!=null) {
            ee.forEach(e -> {
                toDraw.add(e.getComponent(DrawAbove.class).drawer);
            });
        }

    }
}
