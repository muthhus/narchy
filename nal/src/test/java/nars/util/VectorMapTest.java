package nars.util;

import jcog.learn.Autoencoder;
import nars.NAR;
import nars.NARS;
import nars.util.data.UniformVector;
import nars.util.data.VectorMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * @author me
 */


public class VectorMapTest {

    @NotNull
    NAR n = new NARS().get();

    @Test
    public void testUniformVector() {

        float[] d = new float[3];

        UniformVector v = new UniformVector(n, "d", d);
        v.update();

        //n.log();

        n.run(16);

        d[1] = 1.0f;
        d[2] = 0.5f;

        v.update();

        n.run(16);


        //TODO assert that NAR has > 1 concepts
    }

    @Test
    public void testAE() {


        VectorMap v = new VectorMap(n, "d", 8, 0.25f, 2, 0.75f) {

            Autoencoder d;

            @Override
            protected void map(@NotNull float[] in, @NotNull float[] out) {
                if (d == null)
                    d = new Autoencoder(in.length, out.length, n.random());

                d.put(in, 0, 0.05f, 0, true);
                d.encode(in, out, true, true);
            }
        };


        //new TextOutput(n, System.out);

        v.update();

        n.run(16);

        n.onCycle(nn -> {
            long t = n.time();
            if (t % 100 != 0) return;

            for (int i = 0; i < v.input.data.length; i++)
                v.input.data[i] = 0.5f * (float) (1.0 + Math.sin((t + i) / 20.0f));
            v.update();
        });

        v.update();

        n.log();

        n.run(256);


        //new NARSwing(n);

    }

//    public static void main(String[] args) {
//        new VectorMapTest().testAE();
//    }
}
