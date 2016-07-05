package mcaixictw.worldmodels;

import com.gs.collections.api.list.primitive.BooleanList;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;


/**
 * The agents model of the the environment. The goal of the agent is to maximize
 * its reward while it is interacting with an environment. In order to be
 * successful the agent needs to build its own model of the world while it is
 * interacting with it.
 */
public abstract class WorldModel {

	// @Deprecated
	// public Worldmodel() {
	// // JPA
	// }

	protected WorldModel(String name) {
		this.name = name;
	}

	protected String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract String toString();

	// public abstract void clear();



	public abstract void update(BooleanList symlist);

	public abstract void updateHistory(BooleanArrayList symlist);

	public abstract void revert(int numSymbols);

	public abstract void revertHistory(int newsize);

	public abstract int depth();

	public abstract BooleanArrayList genRandomSymbols(int bits);

	public abstract BooleanArrayList genRandomSymbolsAndUpdate(int bits);



	public abstract double predict(BooleanList symbols);

	public abstract boolean nthHistorySymbol(int n);

	public abstract int historySize();

	public static WorldModel build(String name,
								   WorldModelSettings settings, int historySize /* TODO */) {
		return build(name, settings);
	}
	public static WorldModel build(String name,
								   WorldModelSettings settings) {

		if (settings.isFacContextTree()) {
			return new FactorialContextTree(name, settings.getDepth());
		} else {
			return new ContextTree(name, settings.getDepth());
		}
	}

	public abstract void forget(int b);
}