package jcog.learn.ql;

import jcog.learn.Autoencoder;
import jcog.math.FloatAveraged;
import jcog.math.FloatSupplier;
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
    float perceptionAlpha;
    float perceptionNoise = 0f;
    float perceptionCorruption = 0.1f;
    float perceptionForget = 0.0f;
    public FloatSupplier perceptionError;
    public float lastPerceptionError = 0;

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
        //logger.info("start {} -> {} -> {}", inputs, states, outputs);
        this.perceptionAlpha = 0.5f/(inputs);
        this.perceptionError = FloatAveraged.averaged(()->lastPerceptionError, inputs/2);
        ae = perception(inputs, states);
        super.start(states, outputs);
    }

    protected Autoencoder perception(int inputs, int states) {
        return new Autoencoder(inputs, states, rng);
    }


    @Override
    protected int nextAction(int state) {
        //alpha is applied elsewhere, so here directly choose

        return choose(state);
    }

    @Override
    protected int perceive(float[] input) {
        lastPerceptionError = ae.put(input, perceptionAlpha, perceptionNoise, perceptionCorruption, false, true, false);
        int w = ae.decide(decideState);
        if (perceptionForget > 0)
            ae.forget(perceptionForget);
        return w;
    }
    @Override
    public int act(float reward, float[] input) {

        //learn more slowly while the perception is not settled
        float pErr = perceptionError.asFloat();
        return act(reward, input, pErr);
    }

    protected int act(float reward, float[] input, float pErr) {
        float learningRate = 1f / (1f + pErr);
        int a = learn(perceive(input), reward, learningRate, true);

        return a;
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
