package mcaixictw;


import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import nars.util.data.map.UnifriedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * one node of the upper confidence bound applied to trees algorithm.
 */
public class UCTNode {

	public UCTNode(boolean isChanceNode) {
		this.isChanceNode = isChanceNode;
		visits = 0;
		mean = 0;
		//children = new UnifriedMap();
		children = new HashMap();
	}

	private final Map<BooleanArrayList, UCTNode> children; // stores the children
	private boolean isChanceNode; // true if this node is a chance node
	private double mean; // the expected reward of this node
	private int visits; // number of times the search node has been visited
	private double explorationRatio = 1.41; // Exploration-Exploitation
											// constant

	/**
	 * Returns the action with the highest expected reward.
	 * 
	 * @return
	 */
	public BooleanArrayList bestAction() {
		BooleanArrayList bestAction = null;
		double maxReward = Double.MIN_VALUE;

		for (Entry<BooleanArrayList, UCTNode> curr : children.entrySet()) {
			double expectedReward = curr.getValue().mean;
			if (expectedReward > maxReward) {
				maxReward = expectedReward;
				bestAction = curr.getKey();
			}
		}
		return bestAction;
	}

	/**
	 * determine the next action to play
	 * 
	 * @param agent
	 * @param dfr
	 * @return
	 */
	private BooleanArrayList actionSelect(AIXIModel agent, int dfr) {
		assert (agent.numActions() >= children.size());

		double maxValue = Double.MIN_VALUE;
		BooleanArrayList selectedAction = null;

		// If we haven't explored all possible actions, choose one uniformly
		// at random
		if (children.size() < agent.numActions()) {
			List<BooleanArrayList> unexplored = new ArrayList<>();
			for (int a = 0; a < agent.numActions(); a++) {
				BooleanArrayList aa = Util.encode(a, agent.getActionBits());
				if (!children.containsKey(aa)) {
					unexplored.add(aa);
				}
			}
			selectedAction = unexplored.get(Util.randRange(unexplored.size()));
		} else {
			// The general idea is to explore the most promising(with the
			// highest expected reward) actions. But also
			// explore other actions not to get stuck with wrong decisions.

			for (Entry<BooleanArrayList, UCTNode> curr : children.entrySet()) {

				UCTNode currNode = curr.getValue();

				double value = 1.0
						/ (double) (dfr * (agent.maxReward() - agent
						.minReward()))
						* curr.getValue().expectation()
						+ explorationRatio
						* Math.sqrt(Math.log((double) visits())
						/ (double) currNode.visits());

				if (value > maxValue) {
					maxValue = value;
					selectedAction = curr.getKey();
				}
			}

		}
		return selectedAction;
	}

	/**
	 * the expected reward of this node.
	 * 
	 * @return expected reward
	 */
	double expectation() {
		return mean;
	}

	/**
	 * perform a sample run through this node and it's m_children, returning the
	 * accumulated reward from this sample run.
	 * 
	 * @param agent
	 * @param m
	 *            remaining horizon
	 * @param ctUpdate
	 *            update CTW
	 * @return accumulated reward
	 */
	public double sample(AIXIModel agent, int m) {

		ModelUndo undo = new ModelUndo(agent);

		double futureTotalReward;

		if (m == 0) {
			// we have reached the horizon of the agent
			return agent.reward();
		} else if (isChanceNode) {
			BooleanArrayList p = agent.genPerceptAndUpdate();
			UCTNode cp = children.computeIfAbsent(p, (x) -> new UCTNode(false));
			futureTotalReward = cp.sample(agent, m - 1);
		} else if (visits == 0) {
			futureTotalReward = rollout(agent, m);
		} else {
			BooleanArrayList a = actionSelect(agent, m);
			UCTNode cp = children.computeIfAbsent(a, (k) -> new UCTNode(true));
			agent.modelUpdate(a);
			futureTotalReward = cp.sample(agent, m);
		}

		// Calculate the expected average reward
		double reward = futureTotalReward - undo.reward;

		// update the mean reward
		mean = 1.0 / (double) (visits + 1) * (reward + (double) visits * mean);

		visits++;

		// System.out.println("m: " + m + " visits: " + visits + " mean: " +
		// mean
		// + " sample rew: " + reward + " future tot rew: "
		// + futureTotalReward + " undo.getRew: " + undo.getReward());

		agent.modelRevert(undo);

		if (undo.age != agent.age())
			throw new RuntimeException("Undo history age mismatch");
		if (undo.historySize != agent.historySize())
			throw new RuntimeException("Undo history size mismatch");

		assert (undo.reward == agent.reward());
		assert (undo.lastUpdatePercept == agent.lastUpdatePercept());

		return futureTotalReward;
	}

	/**
	 * number of times the search node has been visited
	 */
	int visits() {
		return visits;
	}

	/**
	 * simulate a path through a hypothetical future for the agent within it's
	 * internal model of the world, returning the accumulated reward.
	 * 
	 * @param agent
	 * @param rolloutLength
	 * @param ctUpdate
	 * @return accumulated reward
	 */
	private double rollout(AIXIModel agent, int rolloutLength) {
		assert (!isChanceNode);
		assert (rolloutLength > 0);
		for (int i = 0; i < rolloutLength; i++) {
			agent.genRandomActionAndUpdate();
			agent.genPerceptAndUpdate();
		}
		return agent.reward();
	}

	/**
	 * returns the subtree rooted at [root,action,percept]. If that subtree does
	 * not exist the tree is cleared and a new search tree is returned.
	 * 
	 * @param action
	 * @param percept
	 * @return
	 */
	public UCTNode getSubtree(BooleanArrayList action, BooleanArrayList percept) {
		assert (!isChanceNode);
		//assert (children.containsKey(action));

		UCTNode chanceNode = children.get(action);

		return chanceNode.children.computeIfAbsent(percept, (k)->new UCTNode(false));

		/*
		 * 
		 * NodeSearch afterAction, afterPercept; boolean found = false;
		 * afterAction = child(action); if(afterAction != NULL) { afterPercept =
		 * afterAction->child(percept); if(afterPercept != NULL) { found = true;
		 * } }
		 * 
		 * if(found) { afterAction->m_children[percept] = 0; delete root; return
		 * afterPercept; } else { delete root; return new NodeSearch(false); }
		 */
	}
}