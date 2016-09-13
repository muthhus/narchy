package nars.experiment.rover;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import spacegraph.SimpleSpatial;
import spacegraph.math.v3;
import spacegraph.obj.CompoundSpatial;
import spacegraph.phys.Dynamics;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CylinderShape;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/13/16.
 */
public class Rover extends CompoundSpatial {

    private final NAR nar;


    public Rover(NAR nar) {
        super(nar);
        this.nar = nar;

        SimpleSpatial torso;
        add(torso = new SimpleSpatial("cylinderTorso") {
            @Override
            protected CollisionShape newShape() {
                //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                return new CylinderShape(v(1, 1, 1));
            }
        });
        torso.shapeColor[0] = 1f;
        torso.shapeColor[1] = 0.1f;
        torso.shapeColor[2] = 0.5f;
        torso.shapeColor[3] = 1f;

    }


}
