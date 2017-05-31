package spacegraph.tool;

import com.google.common.primitives.Doubles;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.Node;
import jcog.net.attn.MeshMap;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.util.List;

import static jcog.Texts.n4;
import static spacegraph.SpaceGraph.window;

/** a tool for realtime multidimensional data visualization
 *  input modes:
 *      UDPeer net
 *          -each data model has its own network
 *
 *      stdio CSV (TODO)
 *
 *  visualization models:
 *      3D gasnet pointcloud
 */
public class Dimensionaleyez extends SpaceGraph {

    private final MeshMap<Integer, List<Float>> m;
    NeuralGasNet<Node> n = new NeuralGasNet(3, 16) {
        @NotNull
        @Override
        public Node newNode(int i, int dims) {
            return new Node(i, dims);
        }
    };

    public Dimensionaleyez(String id) {
        super();

        n.learn(1, 1, 2);
        n.learn(2, 3, 1);

        m = MeshMap.get(id, (k, v) -> {
            //System.out.println(k + " " + v);
            accept(k, v);
        });


    }

    private void accept(Integer k, List v) {
        n.learn(Doubles.toArray(v));
    }



    final FloatParam scale = new FloatParam(1, 0.5f, 200f);

    @Override
    protected void render() {

        float s = scale.floatValue();
        n.forEachVertex((Node n) -> {
            double[] d = n.getDataRef();
            float x = (float)d[0] * s;
            float y = (float)d[1] * s;
            float z = (float)d[2] * s;


            gl.glPushMatrix();
            gl.glTranslatef(x, y, z);
            Draw.colorHash(gl, n.hashCode(), 1f);
            glut.glutSolidCube(0.05f);
            gl.glPopMatrix();
        });
    }

    public static void main(String[] s) {
        window(new Dimensionaleyez("d1"), 800, 600);


    }
}
