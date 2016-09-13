package nars.experiment.rover;

import nars.nar.Default;
import spacegraph.SpaceGraph;
import spacegraph.obj.Maze;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {


    public static void main(String[] args) {
        Rover r = new Rover(new Default());

        r.add(new RetinaGrid("cam1", v(), v(0,0,1), v(0.1f,0,0), v(0,0.1f,0), 6,6, 4f));

        new SpaceGraph<>(
                new Maze("x", 20, 20),
                r
        ).setGravity(v(0, 0, -5)).show(1000, 1000);
    }

}
