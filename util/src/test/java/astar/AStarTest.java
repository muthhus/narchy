package astar;

import astar.model.SpaceProblem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AStarTest {

    @Test
    public void SearchNodeTest2D() {
        List<Solution> path = new AStarGoalFind(
                SpaceProblem.SpaceFind.PROBLEM,
                SpaceProblem.at(1, 1),
                SpaceProblem.at(3, 3)
        ).plan;

        assertEquals("[1,1, 2,1, 2,2, 3,2, 3,3]", path.toString());
        assertEquals(path.size(), 5);
    }

//    @Test
//    public void SearchNodeCityTest() {
//        ArrayList<Find> path = new AStarFindGoal().shortestPath(
//                new City("Saarbrücken"),
//                new City("Würzburg"));
//        double e = 0.00001;
//        assertEquals(path.get(0).f(), 222.0, e);
//        assertEquals(path.get(1).f(), 228, e);
//        assertEquals(path.get(2).f(), 269, e);
//        assertEquals(path.get(3).f(), 289, e);
//        assertEquals(path.toString(), "[Saarbrücken,f:222.0, Kaiserslautern,f:228.0, Frankfurt,f:269.0, Würzburg,f:289.0]");
//    }
}

