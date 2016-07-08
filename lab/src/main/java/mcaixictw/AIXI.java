package mcaixictw;

import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import mcaixictw.worldmodels.WorldModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.function.IntConsumer;

import static mcaixictw.Util.asInt;


/**
 * 
 * Manage interaction of the agent with the environment.
 * 
 */
public class AIXI extends AIXIModel {
	
	private static final Logger log = LoggerFactory.getLogger(AIXI.class.getName());

	public AIXI(Environment env, ControllerSettings agentSettings,UCTSettings uctSettings, WorldModel model) {
		this(env.numActions(), env.observationBits(),
			 env.rewardBits(), agentSettings, uctSettings, model);

	}

	/**
	 * use the given instance of WorldModel
	 * 
	 * @param env
	 * @param agentSettings
	 * @param uctSettings
	 * @param model
	 */
	public AIXI(int actions, int observationBits, int rewardBits, ControllerSettings agentSettings,
                UCTSettings uctSettings, WorldModel model) {
		super(actions, observationBits, rewardBits, model);
		this.uctSettings = uctSettings;
		this.agentSettings = agentSettings;
		search = new UCTSearch();
	}

	public String toString() {

		String result = "";
		result += "cycle: " + cycle + ' ';
		int hs = historySize();
		result += "history: " + hs + ' ';

//		result +=  "(" + (((ContextTree)getAgent().getModel()).minH-hs) + ".." +
//				(((ContextTree)getAgent().getModel()).maxH-hs) + ") ";
//		((ContextTree)getAgent().getModel()).resetHistoryCounter();

		result += agentSettings.toString() + ' ';
		result += uctSettings.toString() + ' ';
		result += "avgRew: " + this.averageReward();

		return result;
	}

	private UCTSettings uctSettings;
	private ControllerSettings agentSettings;
	private UCTSearch search;
	private PrintWriter csv;
	private int cycle = 1;

//	/**
//	 * interact with the environment. if the random flag is true the agent will
//	 * choose a random action on every cycle. Otherwise the agent will either
//	 * use the UCT algorithm to estimate the optimal action or it will choose a
//	 * random action with the probability given by the agentSettings
//	 * (exploration).
//	 *
//	 * @param cycles
//	 * @param random
//	 */
//	public void play(int obs, int rew, int cycles, boolean random) {
//		System.out.println("play " + cycles + " cycles");
//		System.out.println(this);
//		for (int k = 0; k < cycles; k++) {
//			play(obs, rew, random);
//		}
//	}

	public void run(Environment e, boolean random) {
		run(e.getObservation(), e.getReward(), random, e::performAction);
	}

	/**
	 * same as the other play function but play just one cycle.
	 * 
	 * @param random
	 */
	public void run(int obs, int rew, boolean random, IntConsumer onAction) {
		this.modelUpdate(obs, rew);
		decide(random, onAction);
	}

	public void run(float[] obs, int bitsPerInput, int rew, boolean random, IntConsumer onAction) {
		this.modelUpdate(obs, bitsPerInput, rew);
		decide(random, onAction);
	}


	void decide(boolean random, IntConsumer onAction) {
		boolean explore = random || (Math.random() < agentSettings.getExploration());

		BooleanArrayList action;
		if (explore) {
			action = this.genRandomActionAndUpdate();
		} else {
			action = search.search(this, uctSettings);
			this.modelUpdate(action);
		}

		//update memory
		int perceptSize = getAgent().getRewBits() + getAgent().getObsBits() + getAgent().getActionBits();
		if (getModel().historySize() > getModel().depth() * perceptSize) {
			getModel().forget(perceptSize);
		}

		if (action.size() != getActionBits())
			throw new RuntimeException();

		onAction.accept(asInt(action));

		/*log.fine(cycle + "," + obs + ',' + rew + "," + action + ","
				+ explore + ',' + agentSettings.getExploration() + ","
				+ this.reward() + ',' + this.averageReward());*/

		/*if ((cycle & (cycle - 1)) == 0) {
			System.out.println(this);
		}*/

		agentSettings.setExploration(agentSettings.getExploration()
				* agentSettings.getExploreDecay());
		cycle++;
	}

	/**
	 * dictate observation, reward and action. can be used for training
	 * purposes.
	 * 
	 * @param obs
	 * @param rew
	 * @param action
	 */
	public void dictate(int obs, int rew, int action) {
		this.modelUpdate(obs, rew);
		this.modelUpdate(Util.encode(action, getActionBits()));
	}




	public AIXIModel getAgent() {
		return this;
	}



	public UCTSearch getSearch() {
		return search;
	}

	public void setSearch(UCTSearch search) {
		this.search = search;
	}

}
