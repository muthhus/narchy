package nars.control;

import jcog.Util;
import nars.NAR;
import nars.Param;
import nars.derive.DefaultDeriver;
import nars.premise.Premise;
import nars.task.ITask;
import nars.task.UnaryTask;

public class DerivePremise extends UnaryTask<Premise> {

    static final ThreadLocal<BufferedDerivation> derivation =
            ThreadLocal.withInitial(BufferedDerivation::new);

    public DerivePremise(Premise premise) {
        super(premise, premise.pri());
    }

    public Premise premise() { return value; }

    @Override
    public ITask[] run(NAR n) {

        BufferedDerivation d = derivation.get();

        assert(d.buffer.isEmpty());

        d.restartA(n);
        d.restartB(value.task);
        d.restartC(value, Util.lerp(pri, Param.UnificationTTLMax, Param.UnificationTTLMin));

        DefaultDeriver.the.test(d);

        return d.flush();



//                    assert (start >= ttlRemain);
//
//                    ttl -= (start - ttlRemain);
//                    if (ttl <= 0) break;

//                    int nextDerivedTasks = d.buffer.size();
//                    int numDerived = nextDerivedTasks - derivedTasks;
//                    ttl -= numDerived * derivedTaskCost;
//                    derivedTasks = nextDerivedTasks;


    }
}
