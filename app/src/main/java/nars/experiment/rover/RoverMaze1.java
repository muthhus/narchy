package nars.experiment.rover;

import nars.nar.Default;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.obj.Maze;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.HingeConstraint;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CylinderShape;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {


    public static void main(String[] args) {
        Rover r = new Rover(new Default()) {

            @Override protected void create(Dynamics world) {

                SimpleSpatial torso;
                add(torso = new SimpleSpatial("cylinderTorso") {
                    @Override
                    protected CollisionShape newShape() {
                        //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                        return new CylinderShape(v(0.5f, 1, 1));
                    }

                    @Override
                    public float mass() {
                        return 40f;
                    }
                });
                torso.shapeColor[0] = 1f;
                torso.shapeColor[1] = 0.1f;
                torso.shapeColor[2] = 0.5f;
                torso.shapeColor[3] = 1f;

                RetinaGrid rg = new RetinaGrid("cam1", v(), v(0, 0, 1), v(0.1f, 0, 0), v(0, 0.1f, 0), 6, 6, 4f) {
                    @Override
                    protected Dynamic create(Dynamics world) {

                        Dynamic l = super.create(world);

                        move(-3,0,0);
                        body.clearForces();

                        l.clearForces();
                        HingeConstraint p = new HingeConstraint(torso.body, body, v(2, 0, 0), v(-2, 0, 0), v(1, 0, 0), v(1, 0, 0));


//                        Point2PointConstraint p = new Point2PointConstraint(body, torso.body, v(2, 0, 0), v(-2, 0, 0));
//                        p.impulseClamp = 0.01f;
//                        //p.damping = 0.5f;
//                        p.tau = 0.01f;
                        add(p);
                        return l;
                    }


                };

                add(rg);
            }
        };



        new SpaceGraph<>(
                new Maze("x", 20, 20),
                r
        ).setGravity(v(0, 0, -5)).show(1000, 1000);


    }

}
