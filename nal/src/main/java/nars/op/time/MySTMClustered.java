package nars.op.time;

import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.budget.BudgetFunctions;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.truth.Stamp;
import nars.truth.TruthFunctions;
import nars.truth.Truthed;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Created by me on 5/22/16.
 */
public class MySTMClustered extends STMClustered {

	private final int maxConjunctionSize;

	public MySTMClustered(@NotNull NAR nar, int size, char punc, int maxConjunctionSize) {
        super(nar, new MutableInteger(size), punc);
		this.maxConjunctionSize = maxConjunctionSize;
    }

    @Override
	protected void iterate() {
		super.iterate();

		//LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

		net.nodeStream().parallel()
			//.sorted((a, b) -> Float.compare(a.priSum(), b.priSum()))
			.filter(n -> {
				double[] tc = n.coherence(0);

				float timeCoherenceThresh = 0.98f;
				float freqCoherenceThresh = 0.75f;

				if (tc[1] >= timeCoherenceThresh) {
					double[] fc = n.coherence(1);
					if (fc[1] >= freqCoherenceThresh) {
						return true;
					}
				}
				return false;
			})
			.map(n -> PrimitiveTuples.pair(n, n.coherence(1)[0]))
			.forEach(nodeFreq -> {
				TasksNode node = nodeFreq.getOne();
				float freq = (float)nodeFreq.getTwo();

				boolean negated;
				if (freq < 0.5f) {
					freq = 1f - freq;
					negated = true;
				} else {
					negated = false;
				}

				float finalFreq = freq;
				node.termSet(maxConjunctionSize).forEach(tt -> {

					Term[] s = Stream.of(tt).map(Task::term).toArray(Term[]::new);

					//float confMin = (float) Stream.of(tt).mapToDouble(Task::conf).min().getAsDouble();
					float conf = TruthFunctions.and((Truthed[]) tt); //used for emulation of 'intersection' truth function

					long[] evidence = Stamp.zip(Stream.of(tt), tt.length, Global.STAMP_MAX_EVIDENCE);

					if (negated)
						$.neg(s);

					@Nullable Term conj = $.parallel(s);
					if (!(conj instanceof Compound))
						return;


					long t = Math.round(node.coherence(0)[0]);

					Task m = new MutableTask(conj, punc,
							new DefaultTruth(finalFreq, conf)) //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
							.time(now, t)
							.evidence(evidence)
							.budget(BudgetFunctions.taxCollection(tt, 1f / s.length))
							.log("STMCluster CoOccurr");

					//System.err.println(m + " " + Arrays.toString(m.evidence()));
					nar.input(m);
					node.clear();

			});


		});

		//TODO create temporally inducted relations between centroids of different time indices

	}
}
