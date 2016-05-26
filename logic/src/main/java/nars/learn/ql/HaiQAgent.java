package nars.learn.ql;

import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.Autoencoder;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/22/16.
 */
public class HaiQAgent extends HaiQ {

    //Hsom...
    final static float perceptionAlpha = 0.02f;
    @NotNull Autoencoder ae;

    public HaiQAgent() {
        super();
    }

    @Override
    public void start(int inputs, int outputs) {
        int states = (int) (2 + Math.sqrt(inputs*outputs));
        ae = new Autoencoder(inputs, states, new XorShift128PlusRandom(1));
        super.start(states, outputs);
    }

    @Override
    protected int nextAction(int state) {
        //alpha is applied elsewhere, so here directly choose
        return choose(state);
    }

    @Override
    protected int perceive(float[] input) {
        ae.train(input, perceptionAlpha, 0.01f, 0.01f, true);
        int w = ae.max();
        return w;
    }


//		@Override
//		protected int lastAction() {
//			//evaluate the actual desire values of the action concepts to get a sense of what the NAR actually wants/wanted to do
//			//and what it ideally would have done if it could execute them all in parallel in proportional amounts
//
//			//Termed best = null;
//			int best = -1;
//			float bestE = Float.NEGATIVE_INFINITY;
//
//			int s = actions;
//
//			final float epsi = Epsilon;
//
//
//			for (int j = 0; j < s; j++) {
//
//				//float e = output.get(j).ran();
//				float e = 0;
//
//				//add noise
//				if (epsi != 0) {
//					e += epsi * (rng.nextFloat() - 0.5f) * 2f;
//				}
//
//				//System.out.println(outs.get(j) + " " + e);
//
//				//System.out.println("last action: " + j + " "  + e);
//				if (e >= bestE) {
//					//best = c;
//					best = j;
//					bestE = e;
//				}
//			}
//
//			return best;
//		}

}
