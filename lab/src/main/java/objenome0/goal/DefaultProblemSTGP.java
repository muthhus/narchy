package objenome0.goal;

import objenome0.op.DoubleVariable;
import objenome0.op.Node;
import objenome0.op.Variable;
import objenome0.op.VariableNode;
import objenome0.op.math.*;
import objenome0.op.trig.Sine;
import objenome0.problem.ProblemSTGP;
import objenome0.solver.evolve.*;
import objenome0.solver.evolve.init.Full;
import objenome0.solver.evolve.mutate.OnePointCrossover;
import objenome0.solver.evolve.mutate.PointMutation;
import objenome0.solver.evolve.mutate.SubtreeCrossover;
import objenome0.solver.evolve.mutate.SubtreeMutation;
import objenome0.solver.evolve.selection.RouletteSelector;
import objenome0.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.List;

/**
 * ProblemSTGP with some generally useful default settings
 */
public abstract class DefaultProblemSTGP extends ProblemSTGP {

    public DefaultProblemSTGP(int populationSize, int expressionDepth, boolean arith, boolean trig, boolean exp, boolean piecewise) {

        the(Population.SIZE, populationSize);

        //List<TerminationCriteria> criteria = new ArrayList<>();
        //criteria.add(new MaximumGenerations());

        //the(EvolutionaryStrategy.TERMINATION_CRITERIA, criteria);
        //the(MaximumGenerations.MAXIMUM_GENERATIONS, 1);

        the(TypedOrganism.MAXIMUM_DEPTH, expressionDepth);

        the(Breeder.SELECTOR, new RouletteSelector());
        //the(Breeder.SELECTOR, new TournamentSelector(7));

        List<OrganismOperator> operators = new ArrayList<>();
        operators.add(new PointMutation());
        the(PointMutation.PROBABILITY, 0.1);
        the(PointMutation.POINT_PROBABILITY, 0.02);
        operators.add(new OnePointCrossover());
        the(OnePointCrossover.PROBABILITY, 0.1);
        operators.add(new SubtreeCrossover());
        the(SubtreeCrossover.PROBABILITY, 0.1);
        operators.add(new SubtreeMutation());
        the(SubtreeMutation.PROBABILITY, 0.1);
        the(Breeder.OPERATORS, operators);

        double elitismRate = 0.2;
        the(BranchedBreeder.ELITISM, (int)Math.ceil(populationSize * elitismRate));




        the(Initialiser.METHOD, new Full());
        //the(Initialiser.METHOD, new RampedHalfAndHalf());

        RandomSequence randomSequence = new MersenneTwisterFast();
        the(RandomSequence.RANDOM_SEQUENCE, randomSequence);


        ArrayList syntax = new ArrayList();

        //+2.0 allows it to grow
        syntax.add( new DoubleERC(randomSequence, -1.0, 2.0, 2));

        if (arith) {
            syntax.add(new Add());
            syntax.add(new Subtract());
            syntax.add(new Multiply());
            syntax.add(new DivisionProtected());
        }
        if (trig) {
            syntax.add(new Sine());
            //syntax.add(new Tangent());
        }
        if (exp) {
            //syntax.add(new LogNatural());
            //syntax.add(new Exp());
            syntax.add(new Power());
        }
        if (piecewise) {
            syntax.add(new Min2());
            syntax.add(new Max2());
            syntax.add(new Absolute());
        }

        for (Variable v : initVariables())
            syntax.add(new VariableNode(v));


        the(TypedOrganism.SYNTAX, syntax.toArray(new Node[syntax.size()]));
        the(TypedOrganism.RETURN_TYPE, Double.class);

        the(FitnessEvaluator.FUNCTION, initFitness());
    }


    protected abstract FitnessFunction initFitness();
    protected abstract Iterable<Variable> initVariables();

    public static DoubleVariable doubleVariable(String n) {
        return new DoubleVariable(n);
    }

}
