package spacegraph.tool;

import com.google.common.primitives.Doubles;
import com.jogamp.opengl.GL2;
import jcog.data.FloatParam;
import jcog.learn.gng.NeuralGasMap;
import jcog.net.attn.MeshMap;
import org.apache.commons.math3.util.MathArrays;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.phys.Collidable;
import spacegraph.render.Draw;

import java.util.List;

import static spacegraph.SpaceGraph.window;

/**
 * a tool for realtime multidimensional data visualization
 * input modes:
 * UDPeer net
 * -each data model has its own network
 * <p>
 * stdio CSV (TODO)
 * <p>
 * visualization models:
 * 3D gasnet pointcloud
 */
public class Dimensionaleyez extends SimpleSpatial {

    private final MeshMap<Integer, List<Float>> m;
    final NeuralGasMap n = new NeuralGasMap(5, 16, 3);

    public Dimensionaleyez(String id) {
        super(id);

        m = MeshMap.get(id, (k, v) -> {
            //System.out.println(k + " " + v);
            accept(k, v);
        });


    }

    private void accept(Integer k, List v) {
        double[] da = Doubles.toArray(v);
        da = MathArrays.normalizeArray(da, 1f); //HACK this should be a learned normalization to not distort the axes
        n.learn(da);
    }


    final FloatParam scale = new FloatParam(100, 0.5f, 200f);

    @Override
    public void renderRelative(GL2 gl, Collidable body) {

        float s = scale.floatValue();
        n.forEachVertex((NeuralGasMap.AENode n) -> {
            float[] d = n.center();
            float d0 = d[0];
            if (d0 != d0) {
                System.out.println("NaN" + n);
                return;
            }
            float x = d0 * s;
            float y = d[1] * s;
            float z = d[2] * s;

            System.out.println(x + " " + y + " " + z + " <- " + n);

            gl.glPushMatrix();
            gl.glTranslatef(x, y, z);
            Draw.colorHash(gl, n.hashCode(), (float) (1f/(1f+n.getLocalDistance())));
            SpaceGraph.glut.glutSolidCube(1);
            gl.glPopMatrix();
        });
    }

    public static void main(String[] s) {
        window(new Dimensionaleyez("d1"), 800, 600);


    }
}
