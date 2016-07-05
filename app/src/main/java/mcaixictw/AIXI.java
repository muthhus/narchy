package mcaixictw;

import java.io.PrintWriter;
import java.util.logging.Logger;

import mcaixictw.worldmodels.WorldModel;


/**
 * 
 * Manage interaction of the agent with the environment.
 * 
 */
public class AIXI extends AIXIModel {
	
	private static final Logger log = Logger.getLogger(AIXI.class.getName());

	/**
	 * use the given instance of WorldModel
	 * 
	 * @param env
	 * @param agentSettings
	 * @param uctSettings
	 * @param model
	 */
	public AIXI(Environment env, ControllerSettings agentSettings,
                UCTSettings uctSettings, WorldModel model) {
		super(env.numActions(), env.observationBits(),
				env.rewardBits(), model);
		this.uctSettings = uctSettings;
		this.agentSettings = agentSettings;
		this.environment = env;
		search = new UCTSearch();
	}

	public String toString() {
		String limiter = "=====================\n";
		String result = limiter;
		result += agentSettings;
		result += uctSettings;
		result += this.getModel();
		result += "cycle: " + cycle + '\n';
		result += "avgRew: " + this.averageReward() + '\n';
		result += limiter;
		return result;
	}

	private final Environment environment;
	private UCTSettings uctSettings;
	private ControllerSettings agentSettings;
	private UCTSearch search;
	private PrintWriter csv;
	private int cycle = 1;

	/**
	 * interact with the environment. if the random flag is true the agent will
	 * choose a random action on every cycle. Otherwise the agent will either
	 * use the UCT algorithm to estimate the optimal action or it will choose a
	 * random action with the probability given by the agentSettings
	 * (exploration).
	 * 
	 * @param cycles
	 * @param random
	 */
	public void play(int cycles, boolean random) {
		System.out.println("play " + cycles + " cycles");
		System.out.println(this);
		for (int k = 0; k < cycles; k++) {
			play(random);
		}
	}

	/**
	 * same as the other play function but play just one cycle.
	 * 
	 * @param random
	 */
	public void play(boolean random) {
		int obs = environment.getObservation();
		int rew = environment.getReward();
		this.modelUpdate(obs, rew);
		boolean explore = (Math.random() < agentSettings.getExploration())
				|| random;
		int action;
		if (explore) {
			action = this.genRandomActionAndUpdate();
		} else {
			action = search.search(this, uctSettings);
			this.modelUpdate(action);
		}
		environment.performAction(action);
		
		log.fine(cycle + "," + obs + ',' + rew + "," + action + ","
				+ explore + ',' + agentSettings.getExploration() + ","
				+ this.reward() + ',' + this.averageReward());
		
		if ((cycle & (cycle - 1)) == 0) {
			System.out.println(this);
		}
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
		this.modelUpdate(action);
	}

	public WorldModel getModel() {
		return this.getModel();
	}

	public Environment getEnvironment() {
		return environment;
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
