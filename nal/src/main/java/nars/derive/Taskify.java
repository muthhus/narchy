package nars.derive;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.CauseChannel;
import nars.control.Derivation;
import nars.derive.rule.PremiseRule;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static nars.Param.FILTER_SIMILAR_DERIVATIONS;
import static nars.control.MetaGoal.Perceive;

public class Taskify extends AbstractPred<Derivation> {

    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);

    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final CauseChannel<Task> channel;
    private final String rule;

    protected Taskify(@NotNull PremiseRule rule, CauseChannel<Task> channel) {
        super($.func("taskify", $.the(channel.id)));
        this.channel = channel;
        this.rule = rule.toString(); //only store toString of the rule to avoid remaining attached to the RuleSet
    }

    @Override
    public boolean test(Derivation p) {
        Term x = p.derivedTerm.get();
        if (x == null)
            return false;

        long[] occ = p.derivedOcc;
        byte punc = p.concPunc;
        assert (punc != 0) : "no punctuation assigned";

        final NAR nar = p.nar;
        Task t = Task.tryTask(x, punc, p.concTruth, (C, tr) -> {

            long start = occ[0];
            long end = occ[1];
            assert (end >= start);

            short[] cause = ArrayUtils.addAll(p.parentCause, channel.id);

            long[] evi = p.single ? p.evidenceSingle() : p.evidenceDouble();

            long now = p.time;

            DerivedTask derived =
                    Param.DEBUG ?
                            new DebugDerivedTask(C, punc, tr, now, start, end, evi, cause, p.task, !p.single ? p.belief : null) :
                            new DerivedTask(C, punc, tr, now, start, end, evi, cause);


            return derived;
        });

        if (t == null) {
            return spam(p, Param.TTL_DERIVE_TASK_FAIL, x, nar, 0.5f);
        }

        if (same(t, p.task, p.truthResolution) || (p.belief != null && same(t, p.belief, p.truthResolution))) {
            //created a duplicate of the task
            return spam(p, Param.TTL_DERIVE_TASK_SAME, t, nar, 0.5f);
        }


        float priority = nar.derivePriority(t, p);
        assert (priority == priority);

        float tp = t.setPri(priority);

        if (Param.DEBUG)
            t.log(rule);

        if (p.derivations.merge(t, t, Conclusion.DUPLICATE_DERIVATION_MERGE) != t) {
            spam(p, Param.TTL_DERIVE_TASK_REPEAT, t, nar, 0.25f);
        } else {
            p.use(Param.TTL_DERIVE_TASK_SUCCESS);
        }

        return true;
    }


    private boolean spam(@NotNull Derivation p, int cost, Termed t, NAR nar, float factor) {
        p.use(cost);
        if (factor > 0) {
            channel.learn(Perceive,  t.voluplexity() * factor);
        }
        return true; //just does
    }

    protected boolean same(Task derived, Task parent, float truthResolution) {
        if (parent.isDeleted())
            return false;

        if (derived.equals(parent)) return true;

        if (FILTER_SIMILAR_DERIVATIONS) {
            //test for same punc, term, start/end, freq, but different conf
            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() && parent.start() == derived.start() && parent.end() == derived.end()) {
                if (Arrays.equals(derived.stamp(), parent.stamp())) {
                    if (parent.isQuestOrQuestion() ||
                            (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
                                    parent.evi() >= derived.evi())
                            ) {
                        if (Param.DEBUG_SIMILAR_DERIVATIONS)
                            logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, rule);

                        //((NALTask)parent).merge(derived);
                        parent.priMax(derived.priElseZero());
                        ((NALTask) parent).causeMerge(derived); //merge cause
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
