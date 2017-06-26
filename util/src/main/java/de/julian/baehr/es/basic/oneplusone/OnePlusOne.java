package de.julian.baehr.es.basic.oneplusone;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import de.julian.baehr.es.basic.oneplusone.mutation.IMutationOperation;
import de.julian.baehr.es.basic.oneplusone.mutation.SelfadaptingOnePlusOne;
import de.julian.baehr.es.basic.oneplusone.recombination.DominantRecombination;
import de.julian.baehr.es.basic.oneplusone.recombination.IRecombinationOperator;
import de.julian.baehr.es.basic.oneplusone.type.IType;
import de.julian.baehr.es.basic.oneplusone.type.PlusType;

public class OnePlusOne {

	public static void main(String[] args) {
		
		int times = 20;
		long[] longs = new long[times];
		
		for(int i = 0; i < times; i++)
			longs[i] = run();
			
		System.out.println();
		longs = LongStream.of(longs).sorted().toArray();
		
		for(int i = 0; i < times; i++)
			System.out.println(longs[i]);
	}
	
	public static long run() {
		
		int problemDimension = 4;
		
		//rosenbrock function
		final IObjectiveFunction objectiveFunction = e -> {
			double fitness = 0;
			
			//fitness = Math.pow(1 - e.getObjectiveParameter(0), 2) + 100*Math.pow(e.getObjectiveParameter(1) - Math.pow(e.getObjectiveParameter(0), 2), 2);
			
			for(int i = 0; i < e.getVectorSize()-1; i++){
				fitness += 100*Math.pow(e.getObjectiveParameter(i+1) - Math.pow(e.getObjectiveParameter(i), 2), 2) + Math.pow(1 - e.getObjectiveParameter(i), 2);
			}
			
			return fitness;
		};
		
		//initial vector supply function
		final Supplier<double[]> initialVectorSupplier = () -> {
			double[] vector = new double[problemDimension];
			
			for(int i = 0; i < vector.length; i++)
				vector[i] = 5;
			
			return vector;
		};
		
		IRecombinationOperator recombinationOperator = new DominantRecombination();
		IMutationOperation mutationOperation = new SelfadaptingOnePlusOne();
		IType type = new PlusType();
		
		List<Individual> parents = null, children = new ArrayList<>();
		
		int parentCount = 1;
		int parentsPerChild = 1;
		int childCount = 1;
		
		//final int maxSteps = 1000000000;
		
		Individual bestSoFar = null;
		
		//algorithm
		parents = generateParents(parentCount, initialVectorSupplier);
		evaluate(parents, objectiveFunction);
		sort(parents);
		bestSoFar = getBestSoFar(parents, bestSoFar);
		print(bestSoFar);
		
		long steps = 0;
		do{
			
			recombine(parents, children, parentsPerChild, childCount, recombinationOperator);
			mutate(children, mutationOperation, objectiveFunction);
			evaluate(children, objectiveFunction);
			Individual temp = bestSoFar;
			select(sort(type.getPossibleParents(parents, children)), parents);
			
			bestSoFar = getBestSoFar(parents, bestSoFar);
			
//			if(bestSoFar != temp){
//				System.out.print(steps + " " );
//				print(bestSoFar);
//			}
			
			steps++;
		}while(bestSoFar.getFitness() > 1E-6);
		
		System.out.println("Result (after " + steps + " steps):");
		print(bestSoFar);
		
		return steps;
	}
	
	static Individual getBestSoFar(List<Individual> parents, Individual bestSoFar){

		if(bestSoFar == null)
			return parents.get(0);
		else
			return parents.get(0).getFitness() <= bestSoFar.getFitness() ? parents.get(0) : bestSoFar;
	}
	
	static void select(List<Individual> individuals, List<Individual> parents){
		
		for(int i = 0; i < parents.size(); i++)
			parents.set(i, individuals.get(i));
	}
	
	static void mutate(List<Individual> individuals, IMutationOperation mutationOperation, IObjectiveFunction objectiveFunction){
		for(Individual i : individuals)
			mutationOperation.mutate(i, objectiveFunction);
	}
	
	static void print(Individual individual){
		System.out.print("Individual: ");
		System.out.print(individual.getFitness() + " : ");
		for(int i = 0; i < individual.getVectorSize(); i++)
			System.out.print(individual.getObjectiveParameter(i) + ((i == individual.getVectorSize() - 1) ? "": ", "));
		System.out.println();
	}
	
	static void evaluate(List<Individual> individuals, IObjectiveFunction objectiveFunction){
		for(Individual i : individuals)
			i.calculateFitness(objectiveFunction);;
	}
	
	static List<Individual> sort(List<Individual> individuals){
		individuals.sort((i1, i2) -> Double.compare(i1.getFitness(), i2.getFitness()));
		return individuals;
	}
	
	static void recombine(List<Individual> parents, List<Individual> children, int parentsPerChild, int childCount, IRecombinationOperator recombinationOperator){
		
		if(childCount < parents.size())
			throw new RuntimeException("ChildCount must be greater or equal to ParentCount!");
		
		children.clear();

		for(int i = 0; i < childCount; i++){
			Individual child = recombinationOperator.recombine(chooseParents(parents, parentsPerChild));
			children.add(child);
		}
	}
	
	static List<Individual> chooseParents(List<Individual> parents, int parentsPerChild){
		
		if(parentsPerChild > parents.size())
			throw new RuntimeException("Cannot use more parents per child then there are parents!");
		
		if(parentsPerChild == parents.size())
			return parents;
		
		Random random = new Random();
		List<Individual> remainingPossibleParents = new LinkedList<>(parents);
		List<Individual> childParents = new ArrayList<>();
		for(int i = 0; i < parentsPerChild; i++){
			
			Individual chosenParent = remainingPossibleParents.remove(random.nextInt(remainingPossibleParents.size()));
			childParents.add(chosenParent);
		}
		
		return childParents;
	}
	
	static List<Individual> generateParents(int parentCount, Supplier<double[]> initialVectorSupplier){
		List<Individual> parents = new ArrayList<>();
		
		for(int i = 0; i < parentCount; i++)
			parents.add(new Individual(initialVectorSupplier.get()));
		
		return parents;
	}
}
