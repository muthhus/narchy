package mcaixictw.worldmodels;

import java.util.*;

import com.gs.collections.api.list.primitive.BooleanList;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import com.gs.collections.impl.stack.mutable.primitive.BooleanArrayStack;
import mcaixictw.Bits;
import mcaixictw.Util;

/**
 * 
 * A bit context tree. Learns the next bit based on context which is of the same
 * length as the tree.
 * 
 */
public class ContextTree extends WorldModel {

	public static final double LOG_OF_HALF = Math.log(0.5);
	private final int maxHistoryLength = 16384;

	// create a context tree of specified maximum depth
	protected ContextTree(String name, int depth) {
		super(name);
		history = new BitSet(maxHistoryLength);

		root = new CTNode();
		this.depth = depth;
	}

	protected BitSet history; // the agents history
	int historyPtr = 0;

	protected CTNode root; // the root node of the context tree
	protected int depth; // the maximum depth of the context tree

	protected void push(boolean b) {
		if (historyPtr == maxHistoryLength)
			throw new RuntimeException("history overflow");
		history.set(historyPtr++, b);
	}

	protected boolean pop() {
		if (historyPtr == 0)
			throw new RuntimeException("history underflow");
		return history.get(--historyPtr);
	}
	protected void pop(int n) {
		historyPtr -= n;
		if (historyPtr < 0)
			throw new RuntimeException("history underflow");
	}


	/**
	 * recursively traverses the tree and returns a string in human readable
	 * format (for debug purposes)
	 */
	public String toString() {
		String result = "";
		result += "ContextTree" + '\n';
		result += "name: " + name + '\n';
		result += "depth: " + depth + '\n';
		result += "history size: " + historySize() + '\n';
		return result;
	}

	public String prettyPrint() {
		String result = "";
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				boolean b1 = i != 0;
				boolean b2 = j != 0;

				List<Boolean> symbols = new ArrayList<>();
				symbols.add(b1);
				symbols.add(b2);

				result += "P(" + b2 + b1 + "|h) = " + predict(Util.asBitSet(symbols)) + '\n';
			}
		}
		if (root != null) {
			int nodesPerLevel = 1;
			int nodesPerNextLevel = 0;
			int nodesProcessed = 0;
			LinkedList<CTNode> bfsQueue = new LinkedList<>();
			bfsQueue.add(root);
			while (!bfsQueue.isEmpty()) {
				CTNode currNode = bfsQueue.removeFirst();
				result += currNode + " ";
				nodesProcessed++;
				if (currNode.child(false) != null) {
					bfsQueue.addLast(currNode.child(false));
					nodesPerNextLevel++;
				}
				if (currNode.child(true) != null) {
					bfsQueue.addLast(currNode.child(true));
					nodesPerNextLevel++;
				}
				if (nodesProcessed == nodesPerLevel) {
					result += "\n\n";
					nodesProcessed = 0;
					nodesPerLevel = nodesPerNextLevel;
					nodesPerNextLevel = 0;
				}
			}
		}
		return result;
	}

	protected void add(boolean sym, BitSet history, int historyPtr) {
		CTNode currNode = root;

		// Update the root
		double logKTMul = currNode.logKTMul(sym);
		currNode.logPest += logKTMul;
		assert (!Double.isNaN(currNode.logPest));

		currNode.incrCount(sym);

		// Update all nodes with that context
		int d = Math.max(historyPtr - depth, 0);

		for (int i = historyPtr - 1; i >= d; i--) {

			boolean currSymbol = history.get(i);
			// may have to create a new node.
			if (currNode.child(currSymbol) == null) {
				currNode.setChild(currSymbol, new CTNode());
			}

			currNode = currNode.child(currSymbol);

			// Update the node
			currNode.logPest += currNode.logKTMul(sym);
			assert (!Double.isNaN(currNode.logPest));
			currNode.incrCount(sym);
		}
		updateProbabilities(currNode);
	}

	protected void remove(boolean sym, BitSet history, int historyPtr) {

		CTNode currNode = root;

		assert (currNode.count(sym) > 0);
		currNode.decrCount(sym);
		// improve numerical stability
		currNode.logPest = currNode.count(false) + currNode.count(true) == 0 ? 0.0
				: currNode.logPest - currNode.logKTMul(sym);
		assert (!Double.isNaN(currNode.logPest));

		// Update all nodes with that context

		int d = Math.max(historyPtr - depth, 0);

		for (int i = historyPtr - 1; i >= d; i--) {

			boolean currSymbol = history.get(i);

			currNode = currNode.child(currSymbol);
			if (currNode == null)
				throw new NullPointerException(currSymbol + " does not exist for " + currNode + " (history=" + historyPtr + ")");


			// Update the node
			assert (currNode.count(sym) > 0);
			currNode.decrCount(sym);
			if ((currNode.count(false) + currNode.count(true)) == 0) {
				// Delete the current node, can't be the root node which was
				// processed above.
				currNode = currNode.parent;
				currNode.setChild(currSymbol, null);
				break;
			} else {
				currNode.logPest -= currNode.logKTMul(sym);
				assert (!Double.isNaN(currNode.logPest));
			}
		}
		updateProbabilities(currNode);
	}

	protected static void updateProbabilities(CTNode currNode) {

		// Update the probabilities
		// using the weighted probabilities from section 5.7

		// TODO make sure this is correct.
		if (currNode.child(false) == null && currNode.child(true) == null) {
			currNode.logPweight = currNode.logPest;
			currNode = currNode.parent;
		}

		double[] pChildW = new double[2];

		while (currNode != null) {

			CTNode falseNode = currNode.child(false);
			pChildW[0] = falseNode != null ? falseNode.logPweight : 0.0;
			CTNode trueNode = currNode.child(true);
			pChildW[1] = trueNode != null ? trueNode.logPweight : 0.0;

			// We want to calculate
			// log(Pw) = log(0.5(P_est + P0*P1))
			// Doing a bit of calculus (and using Google "sum of log
			// probabilities) we can rewrite this:
			// log(Pw) = log(0.5(P_est + P0*P1)) = log(0.5) + log(P_est + P0*P1)
			// = log(0.5) + log(1+ (P0*P1)/P_est) + logP_est
			// = log(0.5) + log(1+ exp(log((P0*P1)/P_est))) + logP_est =
			// log(0.5) + log(1+ exp( logP0+logP1-logP_est ) + logP_est
			// and so we only need one exp

			// double p_x = pChildW[0] + pChildW[1] - currNode.logPest;

			// if (p_x < 80) {
			// p_x = Math.log(1.0 + Math.exp(p_x));
			// }
			// if (p_x < -80) {
			// p_x = 1;
			// }

			double log_one_plus_exp = pChildW[0] + pChildW[1]
					- currNode.logPest;

			if (log_one_plus_exp < 80.0) {
				log_one_plus_exp = Math.log(1.0 + Math.exp(log_one_plus_exp));
			}

			currNode.logPweight = LOG_OF_HALF + currNode.logPest + log_one_plus_exp;

			// inefficient
			// double p_est = Math.exp(currNode.logPest);
			// double child0 = Math.exp(pChildW[0]);
			// double child1 = Math.exp(pChildW[1]);
			// double p_weight = 0.5 * (p_est + child0 * child1);
			//
			// currNode.logPweight = Math.log(p_weight);
			//
			// if (Double.isInfinite(currNode.logPweight)) {
			// currNode.logPweight = Double.MIN_VALUE;
			// }

			// if (Double.isInfinite(currNode.logPweight)
			// || Double.isNaN(currNode.logPweight)) {
			// System.out.println(p_est + " " + child0 + " " + child1);
			// System.out.println("is invalid: " + p_weight + " "
			// + currNode.logPweight);
			// }

			assert (!(Double.isNaN(currNode.logPweight) || Double
					.isInfinite(currNode.logPweight)));

			// continue with parent node
			currNode = currNode.parent;
		}
	}

	/**
	 * either add or remove a symbol from the tree.
	 * 
	 * 
	 * @param sym
	 * @param addSymbol
	 */
	@Deprecated
	protected void change(boolean sym, boolean addSymbol,
			Stack<Boolean> underlyingHistory) {

		CTNode currNode = root;

		// Update the root
		if (addSymbol) {
			currNode.logPest += currNode.logKTMul(sym);
			currNode.incrCount(sym);
			// m_added_symbol_count++;
		} else {
			assert (currNode.count(sym) > 0);
			currNode.decrCount(sym);
			// improve numerical stability:
			if ((currNode.count(false) + currNode.count(true)) == 0) {
				currNode.logPest = 0.0;
			} else {
				currNode.logPest -= currNode.logKTMul(sym);
			}
			// m_added_symbol_count--;
		}

		// Update all nodes with that context

		int d = Math.max(historyPtr - depth, 0);

		for (int i = historyPtr - 1; i >= d; i--) {

			boolean currSymbol = underlyingHistory.get(i);
			// may have to create a new node.
			assert (addSymbol == true);

			CTNode symbolChild = currNode.child(currSymbol);
			if (symbolChild == null) {
				currNode.setChild(currSymbol, symbolChild = new CTNode());
				// currNode.child(currSymbol).parent = currNode;
			}
			currNode = symbolChild;

			// Update the node
			if (addSymbol) {
				currNode.logPest += currNode.logKTMul(sym);
				currNode.incrCount(sym);
			} else {
				assert (currNode.count(sym) > 0);
				currNode.decrCount(sym);
				// Delete the current node
				// the current node cannot be the root node here, which was
				// processed above already
				if ((currNode.count(false) + currNode.count(true)) == 0) {
					currNode = currNode.parent;
					currNode.setChild(currSymbol, null);
					break;
				} else {
					currNode.logPest -= currNode.logKTMul(sym);
				}
			}

		}

		// Update the probabilities
		// using the weighted probabilities from section 5.7
		if (currNode.isEmpty()) {
			currNode.logPweight = currNode.logPest;
			currNode = currNode.parent;
		}

		double[] pChild_w = new double[2];

		while (currNode != null) {
			for (int c = 0; c < 2; c++) {
				boolean cBool = c == 1;
				pChild_w[c] = (currNode.child(cBool) != null) ? currNode
						.child(cBool).logPweight : 0;
			}

			// We want to calculate
			// log(Pw) = log(0.5(P_est + P0*P1))
			// Doing a bit of calculus (and using Google "sum of log
			// probabilities) we can rewrite this:
			// log(Pw) = log(0.5(P_est + P0*P1)) = log(0.5) + log(P_est + P0*P1)
			// = log(0.5) + log(1+ (P0*P1)/P_est) + logP_est
			// = log(0.5) + log(1+ exp(log((P0*P1)/P_est))) + logP_est =
			// log(0.5) + log(1+ exp( logP0+logP1-logP_est ) + logP_est
			// and so we only need one exp
			double p_x = pChild_w[0] + pChild_w[1] - currNode.logPweight;

			// if (p_x < 80) {
			// p_x = Math.log(1.0 + Math.exp(p_x));
			// }
			// if (p_x < -80) {
			// p_x = 1;
			// }

			double p_w = p_x + LOG_OF_HALF + currNode.logPweight;

			if (Util.DebugOutput) {
				System.out
						.println("currNode.log_prob_weighted: "
								+ currNode.logPweight + " p_x: " + p_x
								+ " p_w: " + p_w);
			}
			// Make sure that the probabilities are updated correctly
			assert (!addSymbol || currNode.logPweight >= p_w);
			assert (addSymbol || currNode.logPweight <= p_w);
			currNode.logPweight = p_w;

			// continue with parent node
			currNode = currNode.parent;
		}
	}

	/**
	 * update the model with a symbol.
	 * 
	 * @param sym
	 */
	protected void update(boolean sym) {
		add(sym, history, historyPtr);
		push(sym); // The symbol is now history...
	}

	/**
	 * most recent symbol has the highest index.
	 * 
	 * @param symlist
	 */
	public void update(BooleanList symlist) {
		// Update the symbol list
		int len = symlist.size();
		for (int i = 0; i < len; i++) {
			update(symlist.get(i));
		}
	}

	/**
	 * updates the history statistics, without touching the context tree
	 * 
	 * most recent symbol has the highest index.
	 * 
	 * @param symlist
	*/
	public void updateHistory(BooleanArrayList b) {
		// System.out.println("update history: " + Util.toString(symlist));
		int bs = b.size();
		for (int i = 0; i < bs; i++)
			push(b.get(i));
	}

	/**
	 * removes the most recently observed symbol from the context tree
	 */
	public void revert() {

		// we need to access the history otherwise we can not tell which the
		// last inserted symbol was.
		boolean sym = pop();
		remove(sym, history, historyPtr);
	}

	public void revert(int numSymbols) {
		for (int i = 0; i < numSymbols; i++)
			revert();
	}

	/**
	 * shrinks the history down to a former size
	 * 
	 * @param newsize
	 */
	public void revertHistory(int newsize) {

		assert (newsize <= historyPtr);

		int toRemove = historyPtr - newsize;
		pop(toRemove);
	}

	/**
	 * generate a specified number of random symbols distributed according to
	 * the context tree statistics
	 * 
	 * @param bits
	 * @return
	 */
	public BooleanArrayList genRandomSymbols(int bits) {
		BooleanArrayList result = genRandomSymbolsAndUpdate(bits);
		// restore the context tree to it's original state
		for (int i = 0; i < bits; i++) {
			revert();
		}
		return result;
	}

	double eps = 1E-8;

	/**
	 * generate a specified number of random symbols distributed according to
	 * the context tree statistics and update the context tree with the newly
	 * generated bits. last predicted symbol has the highest index.
	 */
	public BooleanArrayList genRandomSymbolsAndUpdate(int bits) {
		BooleanArrayList result = new BooleanArrayList(bits);
		for (int i = 0; i < bits; i++) {
			boolean sampledSymbol = false;
			if (Math.random() <= predict(Bits.one)) {
				sampledSymbol = true;
			}
			result.add(sampledSymbol);
			update(sampledSymbol);
		}
		return result;
	}

	/**
	 * the estimated probability of observing a particular sequence
	 * 
	 * @param symbols
	 * @return
	 */
	public double predict(BooleanList symbols) {

		double logProbBefore = logBlockProbability();

		// We calculate the probability for a '1' by first taking the ratio
		// between the probability
		// before we update the tree with '1' and afterwards. This then
		// yields the probability for
		// the symbol '1'

		// add all the symbols to the tree
		for (int i = 0 ; i < symbols.size(); i++)
			update(symbols.get(i));

		double logProbAfter = logBlockProbability();
		assert (logProbAfter <= logProbBefore);

		// Make sure calculation is correct.
		// assert (probDebug <= 1.0 && probDebug >= 0.0);
		// assert (logProbAfter <= logProbBefore);

		for (int i = 0; i < symbols.size(); i++) {
			revert();
		}

		// equation (8) in the MC-AIXI-CTW paper: conditional probability for
		// the 'symbols' substring
		double logProbSym = logProbAfter - logProbBefore;

		double result = Math.exp(logProbSym);
		assert (result >= 0.0 && result <= 1.0);

		return result;
	}

	/**
	 * the logarithm of the block probability of the whole sequence
	 * 
	 * @return
	 */
	public double logBlockProbability() {
		double logProb = root.logPweight;
		assert (!Double.isNaN(logProb) && !Double.isInfinite(logProb));
		assert (Math.exp(logProb) >= 0.0 && Math.exp(logProb) <= 1.0);
		return logProb;
	}

	/**
	 * get the n'th most recent history symbol, n == 0 returns the most recent
	 * symbol.
	 * 
	 * @param n
	 * @return
	 */
	public boolean nthHistorySymbol(int n) {
		assert (n >= 0);
		int h = historySize();
		assert (n < h);
		return history.get(h - (n + 1));
	}

	public final int historySize() {
		return historyPtr;
	}

	public int size() {
		return root.size();
	}

	public int depth() {
		return this.depth;
	}


	public CTNode getRoot() {
		return root;
	}

	public void setRoot(CTNode root) {
		this.root = root;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * returns the node corresponding to the given context. returns null if the
	 * node does not exist.
	 * 
	 * @param symbols
	 * @return
	 */
	public final CTNode getNode(BooleanArrayList symbols) {
		CTNode currNode = root;
		int s = symbols.size();
		for (int i = 0; i < s; i++) {
			currNode = currNode.child(symbols.get(i));
			if (currNode == null) {
				return null;
			}
		}
		return currNode;
	}
}
