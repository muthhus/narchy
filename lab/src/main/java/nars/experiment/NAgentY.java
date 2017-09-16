package nars.experiment;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.util.IntIntToObjectFunc;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Task;
import nars.concept.ActionConcept;
import nars.concept.ScalarConcepts;
import nars.concept.SensorConcept;
import nars.control.AgentService;
import nars.term.Term;
import nars.util.signal.Signal;
import nars.video.Bitmap2D;
import nars.video.CameraSensor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

abstract public class NAgentY extends NAgentX {

    final AgentService.AgentBuilder spec;

    private AgentService agent;

    public NAgentY(NAR nar, IntIntToObjectFunc<Agent> ctl) {
        this("", nar, ctl);
    }

    public NAgentY(String id, NAR nar, IntIntToObjectFunc<Agent> ctl) {
        super(id, nar);
        spec = new AgentService.AgentBuilder(ctl, () -> rewardCurrent);
    }

    @Override
    protected void start(NAR nar) {

        spec.out(1, (ignored)->{ /* do nothing */ });

        agent = spec.get(nar);

        //agent.ons.off(); //HACK
        super.start(nar);
    }

    @Override
    protected float doAct() {
        //agent.run(nar);
        return super.doAct();
    }

    @Override
    public void addSensor(SensorConcept c) {
        super.addSensor(c);
        spec.in(c.signal::asFloat);
    }

    @Override
    protected <C extends Bitmap2D> CameraSensor<C> addCamera(CameraSensor<C> c) {
        c.pixels.forEach((CameraSensor<C>.PixelConcept p) -> {
            spec.in(() -> p.sensor.currentValue);
        });
        return super.addCamera(c);
    }

    @Override
    public ScalarConcepts senseNumber(FloatSupplier v, ScalarConcepts.ScalarEncoder model, Term... states) {
        ScalarConcepts sc = super.senseNumber(v, model, states);
        sc.forEach(x -> spec.in(x.signal::asFloat) );
        return sc;
    }

    public class RLActionConcept extends ActionConcept {

        private final Signal belief;

        protected RLActionConcept(@NotNull Term term, @NotNull NAR n) {
            super(term, n);
            this.belief = new Signal(BELIEF, n.truthResolution);
        }

        void believe(float freq) {
            long pStart = now;
            long pEnd = now + nar.dur();
            LongSupplier stamper = nar.time::nextStamp;
            belief.set(term(), $.t(freq, nar.confDefault(BELIEF)), stamper, pStart, nar.dur(), nar);
        }

        public void enable() {
            believe(1);
        }
        @Override
        public void clear() {
            believe(0);
        }

        @Override
        public Stream<Task> update(long now, int dur, NAR nar) {
            return Stream.of(belief.get());
        }
    }

    @Override
    public @Nullable ActionConcept actionToggle(@NotNull Term s, @NotNull Runnable on, @NotNull Runnable off) {

        RLActionConcept m = new RLActionConcept(s, nar);

        spec.out(2, (i) -> {
            switch (i) {
                case 0:
                    off.run();
                    m.believe(0);
                    break;
                case 1:
                    on.run();
                    m.believe(1);
                    break;
            }
        });

        return addAction(m);
    }
}
