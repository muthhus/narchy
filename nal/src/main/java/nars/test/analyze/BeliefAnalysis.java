package nars.test.analyze;

import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.time.Tense;
import nars.truth.TruthWave;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** utility class for analyzing the belief/goal state of a concept */
public class BeliefAnalysis extends EnergyAnalysis {

	public final Compound term;

	public BeliefAnalysis(NAR n, Compound term) {
		super(n);
		this.term = term;
	}

	public BeliefAnalysis(@NotNull NAR n, @NotNull String term) {
		this(n, (Compound) n.term(term));
	}

	@NotNull
	public BeliefAnalysis goal(float freq, float conf) {
		nar.goal(term, freq, conf);
		return this;
	}

	@NotNull
	public BeliefAnalysis believe(float freq, float conf) {
		nar.believe(term, freq, conf);
		return this;
	}

	@NotNull
	public BeliefAnalysis believe(float freq, float conf, @NotNull Tense present) {
		nar.believe(term, present, freq, conf);
		return this;
	}
	@NotNull
	public BeliefAnalysis believe(float pri, float freq, float conf, long when) {
		nar.believe(pri, term, when, freq, conf);
		return this;
	}

	@Nullable
	public Concept concept() {
		return nar.concept(term);
	}

	@Nullable
	public BeliefTable beliefs() {
		Concept c = concept();
		if (c == null)
			return BeliefTable.EMPTY;
		return c.beliefs();
	}
	@Nullable
	public BeliefTable goals() {
		Concept c = concept();
		if (c == null)
			return BeliefTable.EMPTY;
		return c.goals();
	}

	@NotNull
	public TruthWave wave() {
		return new TruthWave(beliefs(), nar);
	}

	@NotNull
	public BeliefAnalysis run(int frames) {
		nar.run(frames);
		return this;
	}

	public void print() {
		print(true);
	}
	public void print(boolean beliefOrGoal) {
		BeliefTable table = table(beliefOrGoal);
		System.out.println((beliefOrGoal ? "Beliefs" : "Goals") + "[@" + nar.time() + "] " + table.size()
				+ '/' + table.capacity());
		table.print(System.out);
		//System.out.println();
	}

	public int size(boolean beliefOrGoal) {
		return table(beliefOrGoal).size();
	}

	@Nullable
	public BeliefTable table(boolean beliefOrGoal) {
		return beliefOrGoal ? beliefs() : goals();
	}

	public int size() {
		return size(true);
	}


	/** sum of priorities of the belief table */
	public float priSum() {
		return beliefs().priSum();
	}

	@NotNull
	public BeliefAnalysis input(boolean beliefOrGoal, float v, float v1) {
		if (beliefOrGoal)
			believe(1.0f, 0.9f);
		else
			goal(1.0f, 0.9f);
		return this;
	}

	@NotNull
    public Bag<Task> tasklinks() {
		return concept().tasklinks();
	}

	public long time() {
		return nar.time();
	}
}
