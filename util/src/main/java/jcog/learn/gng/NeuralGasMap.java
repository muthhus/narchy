package jcog.learn.gng;

import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.math.StreamingNormalizer;
import jcog.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;

/** dimension reduction applied to a neural gasnet */
public class NeuralGasMap extends NeuralGasNet<NeuralGasMap.AENode> {

    private final Autoencoder enc;
    private final int outs;

    public class AENode extends Node {

        public float[] center;

        public AENode(int id, int dimensions) {
            super(id, dimensions);
            randomizeUniform(-1, 1);
        }

        @Override
        public double learn(double[] x) {
            double e = super.learn(x);
            return e;
        }

        public float[] center() {
            if (center == null || center.length != outs)
                center = new float[outs];
            float[] f = Util.doubleToFloatArray(getDataRef());
            enc.encode(s.normalize(f, f), center, false, false);
            return center;
        }

    }

    final StreamingNormalizer s;

    public NeuralGasMap(int in, int maxNodes, int out) {
        super(in, maxNodes);
        this.outs = out;
        this.enc = new Autoencoder(in, out, new XorShift128PlusRandom(1));
        this.s = new StreamingNormalizer(in);
    }



    @Override
    public AENode learn(double... x) {
        float[] x1 = Util.doubleToFloatArray(x);
        s.normalize(x1, x1);
        if (x1[0]==x1[0]) //avoid NaN contaminating the matrices
            enc.learn(x1, 0.05f, 0.01f, 0f, false, false, false);
        return super.learn(x);
    }

    @NotNull
    @Override
    public AENode newNode(int i, int dims) {
        return new AENode(i, dims);
    }
}
