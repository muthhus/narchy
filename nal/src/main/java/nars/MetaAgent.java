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

    /** colocated with the agent's NAR */
    public MetaAgent(NAgent agent) {
        this(agent, agent.nar);
    }

    public MetaAgent(NAgent agent, NAR agentNAR) {
        super($.func("meta", agent.id), agentNAR);
        this.agent = agent;

        senseNumber($.p("happy"), ()->agentNAR.emotion.happy());
        senseNumber($.p("sad"), ()->agentNAR.emotion.sad());
        senseNumber($.p("busy"), ()->(float)agentNAR.emotion.busyPri.getSum());
        senseNumber($.p("lern"), ()->(float)agentNAR.emotion.learning());

        actionUnipolar($.p("curi"), (f) -> {
            float newCuriosity = Util.lerp(f, 0.25f, 0f);
            System.out.println("curiosity: " + newCuriosity);
            agent.curiosity.setValue(newCuriosity);
            return true;
        });

        //warning this will feedback and affect this NAR unless it's in a separate NAR
        actionUnipolar($.p("quaMin"), (f) -> {
            float newQuaMin = Util.lerp(f, 0.1f, 0f);
            System.out.println("quaMin: " + newQuaMin);
            agentNAR.quaMin.setValue(newQuaMin);
            return true;
        });
    }

    @Override
    protected float act() {
        //TODO other qualities to maximize: runtime speed, memory usage, etc..
        return agent.happy.asFloat();
    }

}
