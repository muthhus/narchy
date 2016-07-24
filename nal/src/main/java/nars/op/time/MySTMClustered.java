package nars.op.time;

import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.budget.BudgetFunctions;
import nars.nal.Stamp;
import nars.task.GeneratedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.truth.TruthFunctions;
import nars.util.data.MutableInteger;
import nars.util.event.DefaultTopic;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Created by me on 5/22/16.
 */
public class MySTMClustered extends STMClustered {

	public final Topic<Task> generate = new DefaultTopic<>();

	private final int maxConjunctionSize;

	float timeCoherenceThresh = 0.5f; //for sequence pairs phase
	float freqCoherenceThresh = 0.9f;

	float confMin;

	public MySTMClustered(@NotNull NAR nar, int size, char punc, int maxConjunctionSize) {
        super(nar, new MutableInteger(size), punc, maxConjunctionSize);
		this.maxConjunctionSize = maxConjunctionSize;

		//this.logger = LoggerFactory.getLogger(toString());

		allowNonInput = true;

		net.setAlpha(0.05f);
		net.setBeta(0.05f);
		net.setWinnerUpdateRate(0.03f, 0.01f);
    }

    @Override
	protected void iterate() {
		super.iterate();

		confMin = nar.confMin.floatValue();

		//LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

		int maxConjunctionSize = this.maxConjunctionSize;
		float timeCoherenceThresh = this.timeCoherenceThresh;
		float freqCoherenceThresh = this.freqCoherenceThresh;

		//clusters where all terms occurr simultaneously at precisely the same time
		//cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);

		//clusters where dt is allowed, but these must be of length 2
		cluster(2, timeCoherenceThresh, freqCoherenceThresh);


	}

	private void cluster(int maxConjunctionSize, float timeCoherenceThresh, float freqCoherenceThresh) {
		net.nodeStream()
			//.parallel()
				//.sorted((a, b) -> Float.compare(a.priSum(), b.priSum()))
			.filter(n -> {
				if (n.size() < 2)
					return false;

				double[] tc = n.coherence(0);
				if (tc==null)
					return false;


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
				node.termSet(maxConjunctionSize,  nar.compoundVolumeMax.intValue()-1).forEach(tt -> {

					Task[] uu = Stream.of(tt).filter(t -> t!=null).toArray(Task[]::new);


					//float confMin = (float) Stream.of(uu).mapToDouble(Task::conf).min().getAsDouble();
					float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
					if (conf < confMin)
						return;

					long[] evidence = Stamp.zip(Stream.of(uu), uu.length, Param.STAMP_MAX_EVIDENCE);

					@Nullable Term conj = conj(negated, uu);

					if (!(conj instanceof Compound))
						return;


					@Nullable double[] nc = node.coherence(0);
					if (nc == null)
						return;

					long t = Math.round(nc[0]);

					if ((conj = Task.normalizeTaskTerm(conj, punc, nar, true))!=null) {

						Task m = new GeneratedTask(conj, punc,
								new DefaultTruth(finalFreq, conf)) //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
								.time(now, t)
								.evidence(evidence)
								.budget(BudgetFunctions.fund(uu, 1f / uu.length))
								.log("STMCluster CoOccurr");


						//logger.debug("{}", m);
						generate.emit(m);

						//System.err.println(m + " " + Arrays.toString(m.evidence()));
						nar.inputLater(m);

						node.remove(uu);
					}


			});


		});
	}

	private Term conj(boolean negated, Task[] uu) {

		if (uu.length == 2) {
			//find the dt and construct a sequence
			Task early, late;
			if (uu[0].occurrence() <= uu[1].occurrence()) {
				early = uu[0]; late = uu[1];
			} else {
				early = uu[1]; late = uu[0];
			}
			int dt = (int)(late.occurrence() - early.occurrence());

			return $.conj(
					$.negIf(early.term(), negated),
					dt,
					$.negIf(late.term(), negated)
			);

		} else {
			Term[] s = Stream.of(uu).map(Task::term).toArray(Term[]::new);

			if (negated)
				$.neg(s);

			//just assume they occurr simultaneously
			return $.parallel(s);
		}
	}
}
