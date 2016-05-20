package nars.rover;

import com.artemis.World;
import com.artemis.WorldConfiguration;
import nars.rover.system.*;
import nars.util.data.Util;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World2D;

/**
 * NARS Rover
 *
 * @author me
 */
public class Sim {


    private final PhysicsModel physics;
    public final World game;
    public final World2D world;
    private long delayMS;
    private float fps;
    private boolean running;

    public Sim(World2D world) {
        super();



        this.physics = new PhysicsModel();

        this.world = world;
        world.setContactListener(physics);
        world.setGravity(new Vec2());
        world.setAllowSleep(false);

        this.game = new World(new WorldConfiguration()
            .setSystem(new EntityPhysics(this))
            .setSystem(new VisionRaySystem(world))
            .setSystem(new MotorSystem())
            .setSystem(new RendererSystem(this))
            .setSystem(new RunNARSystem())
            .setSystem(new EdibleUpdate())
            .setSystem(new HealthUpdate())
            //.setSystem(new WeaponSystem())
        );

    }




    public static String f(double p) {
        if (p < 0) {
            throw new RuntimeException("Invalid value for: " + p);
            //p = 0;
        }
        if (p > 0.99f) {
            p = 0.99f;
        }
        int i = (int) (p * 10f);
        return String.valueOf(i);
    }

    public final void setFPS(float f) {
        this.fps = f;

        delayMS = (long) (1000f / fps);
    }

    public final void run(float fps) {
        setFPS(fps);

        float dt = 1f/fps;

        running = true;

        while (running) {
            cycle(dt);
            Util.pause(delayMS);
        }
    }

    int velocityIterations = 3;
    int positionIterations = 8;

    public void cycle(float dt) {

        world.step(dt, velocityIterations, positionIterations);

        game.process();

    }

    public final  void stop() {
        running = false;
    }

    public World2D getWorld() {
        return world;
    }


    /*public void add(Being r) {
        r.init(this);
        robots.add(r);
    }*/

    /*public void remove(Body r) {
        getWorld().destroyBody(r);
    }*/



}
