package nars;

import jcog.Util;

/**
 * meta-controller of an NAgent
 *      essentially tunes runtime parameters in response to feedback signals
 *      can be instantiated in a NAR 'around' any agent
 */
public class MetaAgent extends NAgent {

    /** agent controlled by this */
    private final NAgent agent;

    public MetaAgent(NAgent agent) {
        super($.func("meta", agent.id), agent.nar);
        this.agent = agent;

        //senseNumber($.p("happy"), agent.happy);
        senseNumber($.p("busy"), ()->(float)nar.emotion.busyPri.getSum());
        senseNumber($.p("lern"), ()->(float)nar.emotion.learning());

        actionBipolar($.p("curi"), (f) -> {
            float newCuriosity = Util.lerp(f, 0.25f, 0f);
            System.out.println("curiosity: " + newCuriosity);
            agent.curiosity.setValue(newCuriosity);
            return true;
        });
    }

    @Override
    protected float act() {
        //TODO other qualities to maximize: runtime speed, memory usage, etc..
        return agent.happy.asFloat();
    }

}
