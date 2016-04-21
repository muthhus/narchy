/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.world;

import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import nars.Global;
import nars.rover.Sim;
import nars.rover.obj.MaterialColor;
import nars.rover.obj.Physical;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World2D;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.lang.Math.*;

/**
 * @author me
 */
abstract public class RoverWorld implements LayerDraw {


    public final World2D world;

    public RoverWorld(World2D w) {
        this.world = w;
    }

    public EntityEdit newFood(Sim sim, float ww, float wh, float minSize, float maxSize,
                              float mass, Component... quality) {
        float x = (float) random() * ww * 1.75f - ww / 2f;
        float y = (float) random() * wh * 1.75f - wh / 2f;
        float bw = (float) (minSize + random() * (maxSize - minSize));
        float bh = (float) (minSize + random() * (maxSize - minSize));


        //PolygonShape shape = new PolygonShape().setAsBox(bw, bh);
        PolygonShape shape = newRandomConvexPolygon(bw, bh, 4, 7);

        return newPhys(sim, mass, x, y, shape, (float) random() * 3f, quality);
    }

    /**
     * http://stackoverflow.com/questions/21690008/how-to-generate-random-vertices-to-form-a-convex-polygon-in-c
     */
    public static PolygonShape newRandomConvexPolygon(float w, float h, int minPoints, int maxPoints) {



        /*
        2.if number of vertexes is known = N

set random step to be on average little less then 2PI / N
for example da=a0+(a1*Random());
a0=0.75*(2*M_PI/N) ... minimal da
a1=0.40*(2*M_PI/N) ... a0+(0.5*a1) is avg = 0.95 ... is less then 2PI/N
inside for add break if vertex count reach N
if after for the vertex count is not N then recompute all from beginning
because with random numbers you cannot take it that you always hit N vertexes this way !!!
         */


        float a, x, y;

        double twopi = 2.0 * PI;
        double minAngleInc = twopi / maxPoints;
        double maxAngleInc = twopi / minPoints;

        List<Vec2> v = Global.newArrayList(maxPoints);

        PolygonShape p = new PolygonShape();

        for (a = 0.0f; a < twopi; )         // full circle
        {
            x = w * (float) ( cos(a));
            y = h * (float) ( sin(a));
            a += minAngleInc + Math.random() * ((maxAngleInc * 0.95) - minAngleInc); //0.95 for safety epsilon to absolutely make sure below max points

            // here add your x,y point to polygon

            v.add(new Vec2(x, y));

        }
        p.set(v.toArray(new Vec2[v.size()]), v.size());

        return p;


    }

    @NotNull
    public EntityEdit newPhys(Sim sim, float mass, float x, float y, PolygonShape shape, float a, Component... quality) {
        BodyDef bd = new BodyDef();
        if (mass != 0) {
            bd.linearDamping = (0.95f);
            bd.angularDamping = (0.8f);
            bd.type = BodyType.DYNAMIC;
        } else {
            bd.type = BodyType.STATIC;
        }
        bd.position.set(x, y);

        Entity entity = sim.game.createEntity();

        bd.setAngle(a);
        bd.setUserData(entity);

        EntityEdit ee = entity.edit();

        for (Component c : quality)
            ee.add(c);

        return ee.add(new Physical(bd, shape));
    }

    public EntityEdit addWall(Sim sim, float x, float y, float w, float h, float a) {
        return newPhys(sim, 0, x, y, new PolygonShape().setAsBox(w, h), a,
                new MaterialColor(1f, 1f, 1f));
    }

//	public Body addBlock(World2D world, float x, float y, float w, float h, float a, float mass) {
//
//
//		Fixture fd = body.createFixture(shape, mass);
//		fd.setRestitution(1f);
//		return body;
//	}

    @Override
    public void drawGround(JoglAbstractDraw draw, World2D w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw draw, World2D w) {

    }
}
