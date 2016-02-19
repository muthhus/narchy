package nars.op.sys;

import nars.Memory;
import nars.NAR;
import nars.Symbols;
import nars.bag.BLink;
import nars.bag.impl.CurveBag;
import nars.budget.Forget;
import nars.budget.UnitBudget;
import nars.task.MutableTask;
import nars.task.Task;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of system tasks and adjusts parameters to attempt to ensure they run within
 * specified quality and cost limits.  To do this it uses a bag which holds concepts
 * which are formed from input goals (and commands).
 *
 */
public class Kernel {

    private static final Logger logger = LoggerFactory.getLogger(Kernel.class);

    private final NAR nar;
    private final CurveBag<Task> schedule;

    /** forgetting period */
    public final MutableFloat rememberTime = new MutableFloat(1f);


    /** what fraction of the schedule will be serviced per frame update */
    public final MutableFloat updateRate = new MutableFloat(0.5f);

    /** master power control of how much influence this has on the NAR's reasoner budget*/
    final float strength = 0.1f;
    private final Forget.ForgetAndDetectDeletion schForget;

    public Kernel(NAR n, int capacity) {
        this.nar = n;
        this.schedule = new CurveBag(capacity, n.memory.random);
        this.schForget = new Forget.ExpForget(nar, rememberTime, new MutableFloat(0) /* TODO use immutablefloat*/)
                .withDeletedItemFiltering();
        n.memory.eventInput.on(t->{
           if (t.isInput() && (t.isGoal() || t.isCommand()))
               onInput(t);
        });
        n.memory.eventFrameStart.on(x->{
           update();
        });
    }

    private void onInput(Task t) {
        schedule.put(t);
    }

    protected void update() {

        schedule.filter(schForget);

        schedule.sample(updateRate.floatValue(), cl -> {
            nar.conceptualize(cl.get(), UnitBudget.One, strength(cl) );
        });
    }

    private float strength(BLink<Task> cl) {
        return strength * cl.summary();
    }


    public Task run(float priority, String id, Runnable r) {
        MutableTask m = new MutableTask(nar.term(id), Symbols.COMMAND) {

            @Override
            protected void onInput(Memory m) {
                super.onInput(m);
                logger.info("start: {}", this);
            }



            @Override
            public boolean execute(NAR m) {
                //super.onInput(m);
                logger.trace("exec: {}", this);
                r.run();
                return true;
            }
        }.budget(priority, 0.5f, 0.5f);

        nar.input(m); //it will be received when processed

        return m;
    }


}
