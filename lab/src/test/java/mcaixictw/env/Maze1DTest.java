package mcaixictw.env;

import mcaixictw.Environment;
import mcaixictw.RunAgentTest;
import mcaixictw.environments.Maze1DEnv;

/**
 * Created by me on 7/4/16.
 */
public class Maze1DTest extends RunAgentTest {



    @Override public Environment environment() {
        return new Maze1DEnv(3);
    }

    @Override
    protected String name() {
        return "Maze1D";
    }
}

