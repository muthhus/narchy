package nars;

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

    public MetaAgent(NAgent agent, NAR metaNAR) {
        super($.func("meta", agent.id), metaNAR);
        this.agent = agent;
        NAR agentNAR = agent.nar;

        senseNumber($.p("happy"), ()->agentNAR.emotion.happy());
        senseNumber($.p("sad"), ()->agentNAR.emotion.sad());
        senseNumber($.p("busy", "pri"), ()->(float)agentNAR.emotion.busyPri.getSum());
        senseNumber($.p("busy", "vol"), ()->(float)agentNAR.emotion.busyVol.getSum());
        senseNumber($.p("lern"), ()-> agentNAR.emotion.learning());
        senseNumber($.p("dext"), ()-> agent.dexterity());

        actionLerp($.p("curi"), (q) -> agent.curiosity.setValue(q), 0f, 0.25f);

        //warning these can feedback and affect this NAR unless it's in a separate NAR
        actionLerp($.p("quaMin"), (q) -> agentNAR.quaMin.setValue(q), 0f, 0.1f);
//        actionLerp($.p("dur"), (d) -> agentNAR.time.dur(d),
//                0.1f /* 0 might cause problems with temporal truthpolation, examine */,
//                nar.time.dur()*2f /* multiple of the input NAR */);
    }

    @Override
    protected float act() {
        //TODO other qualities to maximize: runtime speed, memory usage, etc..
        return agent.happy.asFloat();
    }

}
