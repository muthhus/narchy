package objenome0.solver;

import objenome0.op.Node;
import objenome0.op.math.*;
import objenome0.solver.evolve.RandomSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Civilization with suitable default settings
 */
public class DefaultCivilization extends Civilization {


    public DefaultCivilization(int threads, int populationSize, int maximumDepth, Node... additionalOperators) {
        super(threads, populationSize, maximumDepth, additionalOperators);
    }

    @Override
    public List<Node> getOperators(RandomSequence random) {
        List<Node> l = new ArrayList();

        l.add(new DoubleERC(random, -1.0, 2.0, 2));

        l.add(new Add());
        l.add(new Subtract());
        l.add(new Multiply());
        l.add(new DivisionProtected());


        l.add(new Min2());
        l.add(new Max2());
        l.add(new Absolute());
        //l.add(new HyperbolicTangent());

    /*LibraryCompletionSpeed prototype = new LibraryCompletionSpeed();
    for (Variable v : prototype.var)
        l.add(new VariableNode(v));*/

        return l;
    }
}
