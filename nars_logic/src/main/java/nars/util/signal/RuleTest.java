package nars.util.signal;

import nars.NAR;
import nars.Narsese;
import nars.nal.meta.PremiseRule;
import nars.nar.Default;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * test an invididual premise
 */
public interface RuleTest {

//	@NotNull
//	public static RuleTest from(@NotNull PremiseRule r) {
//		// TODO eventually make this handle all of r's postconditions and
//		// modifiers, etc
//		String task = r.task().toString();
//		task = task.replace("%", "p"); // TODO do proper term replacement
//
//		String belief = r.belief().toString();
//		belief = belief.replace("%", "p"); // TODO do proper term replacement
//
//		String conc = r.getConclusion().term(0).toString();
//		conc = conc.replace("%", "p");
//		char concPunc = '.';
//
//		char beliefPunc = '.';
//		char taskPunc = '.';
//		return RuleTest(new TestNAR(new Default()), task + taskPunc, belief + beliefPunc, conc
//				+ concPunc);
//	}

//	public RuleTest(@NotNull String task, @NotNull String belief, String result) {
//		this(task, belief, result, 0, 1, 0, 1);
//	}
	public static TestNAR get(TestNAR test, @NotNull String task, @NotNull String belief, String result, float minFreq,
					float maxFreq, float minConf, float maxConf) {
		RuleTest(
				// new SingleStepNAR(),
				test, task, belief, result, minFreq, maxFreq, minConf,
				maxConf);
		return test;
	}

	//private static final Narsese p = Narsese.the();

	public static void RuleTest(@NotNull TestNAR test, @NotNull String task, @NotNull String belief, String result,
					float minFreq, float maxFreq, float minConf, float maxConf) {
		RuleTest(test, test.nar.task(task), test.nar.task(belief), result, minFreq, maxFreq,
				minConf, maxConf);

	}
	public static void RuleTest(@NotNull TestNAR test, @NotNull Task task, @NotNull Task belief, String result,
					float minFreq, float maxFreq, float minConf, float maxConf) {

		test.nar.input(task);
		test.nar.input(belief);

		test.mustBelieve(25, result, minFreq, maxFreq, minConf, maxConf);

	}


}
