package mcaixictw;

import mcaixictw.worldmodels.WorldModel;
import mcaixictw.worldmodels.WorldModelSettings;
import nars.experiment.pacman.Pacman;
import nars.learn.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AIXIAgent implements Agent {

    final static Logger logger = LoggerFactory.getLogger(AIXIAgent.class);

    final int bitsPerInput = 1; //TODO allow more
    final int rewardBits = 3; //TODO allow more
    ControllerSettings controllerSettings = new ControllerSettings();
    UCTSettings uctSettings = new UCTSettings();
    private WorldModel world;
    private AIXI aixi;


    @Override
    public void start(int inputs, int actions) {
        WorldModelSettings modelSettings = new WorldModelSettings();
        modelSettings.setFacContextTree(true);
        modelSettings.setDepth(3);

        uctSettings.setHorizon(3);
        uctSettings.setMcSimulations(100);
        uctSettings.setRecycleUCT(true);
        //final int historySize = 16384;
        //uctSettings.setHorizon(4);

        world = WorldModel.build("agent", modelSettings);

        int numInputs = inputs * bitsPerInput;
        this.aixi = new AIXI(actions, numInputs, rewardBits, controllerSettings, uctSettings, world);

        logger.info("inputs: {}, actions: {}", numInputs, actions);
    }

    @Override
    public int act(float reward, float[] nextObservation) {
        final int[] nextAction = new int[1];

        if (rewardBits!=3)
            throw new UnsupportedOperationException(); //HACK
        int irr = (int)(reward * 4) + 4;
        if (irr > 7) irr = 7;
        if (irr < 0) irr = 0;
        //System.out.println(reward + " -> " + irr);

        aixi.run(nextObservation, bitsPerInput, irr, false, (a) -> {
            nextAction[0] = a;
        });
        return nextAction[0];
    }

    @Override
    public String summary() {
        return aixi.toString();
    }

    public static void main(String[] args) {
        new Pacman(1, 2)
        //new PongEnvironment()
                .run(new AIXIAgent(), 256*64);
    }
}
