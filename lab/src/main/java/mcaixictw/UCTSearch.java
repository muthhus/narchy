package mcaixictw;


import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;

/**
 * provides ability to perform monte carlo UCT searches.
 */
public class UCTSearch {

	private UCTNode uctRoot;

	/**
	 * determine the best action by searching ahead using MCTS
	 * 
	 * @param agent
	 * @return
	 */
	public BooleanArrayList search(AIXIModel agent, UCTSettings settings) {

		if (uctRoot == null) {
			uctRoot = new UCTNode(false);
		} else {
			uctRoot = uctRoot.getSubtree(agent.getLastAction(), agent.getLastPercept());
		}

		int ss = settings.getMcSimulations();
		for (int i = 0; i < ss; i++) {
			// Sample a path trough the possible future
			uctRoot.sample(agent, settings.getHorizon());
		}

		// The currently best action according to the agent's model of the
		// future
		BooleanArrayList bestAction = uctRoot.bestAction();

		// If we do not want to recycle the tree just throw it away
		if (!settings.isRecycleUCT()) {
			uctRoot = null;
		}
		return bestAction;
	}
}
