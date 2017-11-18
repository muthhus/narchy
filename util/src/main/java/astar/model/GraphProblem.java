package astar.model;

import astar.Problem;
import astar.Solution;

import java.util.HashMap;

public class GraphProblem<X extends Solution> implements Problem<X> {

    protected HashMap<X, HashMap<X, Integer>> adj =
            new HashMap<>();

    @Override
    public double cost(X current, X next) {
        return adj.get(current).get(next);
    }

    @Override
    public Iterable<X> next(X current) {
        return adj.get(current).keySet();
    }

}
