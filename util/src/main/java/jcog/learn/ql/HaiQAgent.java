package jcog.learn.ql;

import jcog.learn.Autoencoder;
import jcog.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created by me on 5/22/16.
 */
public class HaiQAgent extends HaiQ {

    public static final Logger logger = LoggerFactory.getLogger(HaiQAgent.class);


    public @NotNull Autoencoder ae;
    final BiFunction<Integer,Integer,Integer> numStates;
    final static float perceptionAlpha = 0.02f;
    float perceptionNoise = 0.005f;
    float perceptionCorruption = 0.01f;
    float perceptionForget = 0.01f;

    //float aeForget = 1f;

    public HaiQAgent() {
        this((inputs, outputs) ->
                (int) Math.ceil(/*Math.sqrt*/((1+inputs)*(1+outputs))));
    }

    public HaiQAgent(BiFunction<Integer,Integer,Integer> numStates) {
        super();
        this.numStates = numStates;
    }

    public HaiQAgent(int in, int hidden, int out) {
        this();
        start(in, hidden, out);
    }

    @Override
    public void start(int inputs, int outputs) {
        int states = numStates.apply(inputs, outputs);
        start(inputs, states, outputs);
    }

    protected void start(int inputs, int states, int outputs) {
        logger.info("start {} -> {} -> {}", inputs, states, outputs);
        ae = new Autoencoder(inputs, states, new XorShift128PlusRandom(1));
        super.start(states, outputs);
    }

    public Autoencoder perception() {
        return ae;
    }

    @Override
    protected int nextAction(int state) {
        //alpha is applied elsewhere, so here directly choose

        return choose(state);
    }

    @Override
    protected int perceive(float[] input) {
        ae.put(input, perceptionAlpha, perceptionNoise, perceptionCorruption, false, true, false);
        int w = ae.decide(decideState);
        if (perceptionForget > 0)
            ae.forget(perceptionForget);
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
