package nars.util.meter;

import nars.NAR;
import nars.concept.Concept;
import nars.concept.util.BeliefTable;
import nars.nal.Tense;
import nars.term.Compound;
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

	@NotNull
	public TruthWave wave() {
		return beliefs().getWave();
	}

	@NotNull
	public BeliefAnalysis run(int frames) {
		nar.run(frames);
		return this;
	}

	public void print() {
		System.out.println("Beliefs[@" + nar.time() + "] " + beliefs().size()
				+ '/' + beliefs().getCapacity());
		beliefs().print(System.out);
		System.out.println();
	}

	public int size() {
		return beliefs().size();
	}

	public void printWave() {
		wave().print();
	}
}
