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
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Created by me on 5/22/16.
 */
public class MySTMClustered extends STMClustered {

    public MySTMClustered(@NotNull NAR nar, int size, char punc) {
        super(nar, new MutableInteger(size), punc);
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
				float freqCoherenceThresh = 0.9f;

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
				node.termSet(4).forEach(tt -> {

					Term[] s = Stream.of(tt).map(Task::term).toArray(Term[]::new);

					float confMin = (float) Stream.of(tt).mapToDouble(Task::conf).min().getAsDouble();

					long[] evidence = Stamp.zip(Stream.of(tt), tt.length, Global.STAMP_MAX_EVIDENCE);

					if (negated)
						$.neg(s);

					@Nullable Term conj = $.parallel(s);
					if (!(conj instanceof Compound))
						return;


					long t = Math.round(node.coherence(0)[0]);

					Task m = new MutableTask(conj, punc,
							new DefaultTruth(finalFreq, confMin)) //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
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
