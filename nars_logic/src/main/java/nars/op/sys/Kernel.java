package nars.op.sys;

import nars.$;
import nars.Memory;
import nars.NAR;
import nars.Symbols;
import nars.bag.BLink;
import nars.bag.impl.CurveBag;
import nars.budget.BudgetMerge;
import nars.budget.Forget;
import nars.budget.UnitBudget;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
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
    public final MutableFloat updateRate = new MutableFloat(0.1f);

    /** master power control of how much influence this has on the NAR's reasoner budget*/
    final float strength = 0.1f;
    private final Forget.@NotNull ForgetAndDetectDeletion schForget;

    public Kernel(NAR n, int capacity) {
        this.nar = n;
        this.schedule = new CurveBag(capacity, nar.random);
        schedule.merge(BudgetMerge.avgDQBlend);
        this.schForget = new Forget.ExpForget(nar, rememberTime, new MutableFloat(1) /* TODO use immutablefloat*/)
            .withDeletedItemFiltering();
        nar.eventInput.on(t->{
           if (t.isInput() && (t.isGoal() || t.isCommand()))
               onInput(t);
        });
        nar.eventFrameStart.on(x->{
           update();
        });
    }

    private void onInput(Task t) {
        schedule.put(t, t.budget());
    }

    protected void update() {

        schedule.filter(schForget);

        schedule.commit();

        NAR n = this.nar;
        schedule.sample(updateRate.floatValue(), cl -> {
            Task t = cl.get();
            if (t.isGoal())
                n.conceptualize(t, UnitBudget.One, strength(cl));
            else /* t.isCommand */
                n.input(t); //re-input
        });

        //schedule.printAll();
    }

    private float strength(BLink<Task> cl) {
        return strength * cl.summary();
    }


    public Task run(float priority, String operator, Runnable r, Term... args) {
        Atomic op = $.operator(operator);

//        //HACK for command tasks which do not get an eventInput emission, tracked here
//        nar.onExecution(op, (Task tt) -> {
//            onInput(tt);
//        });

        MutableTask m = new MutableTask($.exec(op, args), Symbols.COMMAND) {

            /** isnt called for Command tasks currently; they will be executed right away anyway */
            @Override protected void onInput(Memory m) {
                super.onInput(m);
                logger.info("start: {}", this);
            }



            @Override
            public boolean execute(NAR m) {
                //super.onInput(m);
                //logger.info("exec: {} {}", toBudgetString(), this);
                r.run();
                return true;
            }
        }.budget(0f, 1f, priority);

        nar.input(m); //it will be received when processed

        //HACK for command tasks which do not get an eventInput emission, explicitly do this here
        onInput(m);

        return m;
    }


}
