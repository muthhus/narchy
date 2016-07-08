package mcaixictw;

/**
 * Used to restore a previous state of the agent.
 */
final public class ModelUndo {

	public ModelUndo(AIXIModel agent) {
		age = agent.age();
		reward = agent.reward();
		historySize = agent.historySize();
		lastUpdatePercept = agent.lastUpdatePercept();
	}

	public final int age;
	public final int reward;
	public final int historySize;
	public final boolean lastUpdatePercept;

}
