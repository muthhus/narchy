package de.julian.baehr.es.commaAndPlus;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import de.julian.baehr.es.commaAndPlus.recombine.Discrete;
import de.julian.baehr.es.commaAndPlus.recombine.IRecombinator;
import de.julian.baehr.es.commaAndPlus.recombine.Intermediate;
import de.julian.baehr.es.commaAndPlus.recombine.None;
import de.julian.baehr.es.commaAndPlus.type.CommaType;
import de.julian.baehr.es.commaAndPlus.type.IType;
import jcog.Texts;

public class Main {

	public static void main(String[] args){
		int times = 10;
		long[] longs = new long[times];
		for(int i = 0; i < longs.length; i++){
			longs[i] = run();
			System.out.println("i:"+i);
		}
		longs = LongStream.of(longs).sorted().toArray();
		double avg = LongStream.of(longs).average().getAsDouble();
		System.out.println("best:\t" + longs[0]);
		System.out.println("worst:\t" + longs[times-1]);
		System.out.println("avg:\t" + avg);
		
		for(int i = 0; i < times; i++)
			System.out.println(longs[i]);
	}
	
	public static long run() {
	
		final int problemDimension = 3;
		
		IType type = new CommaType();
		
		IObjectiveFunction objectiveFunction = individual -> {
			double fitness = 0;
			
			for(int i = 0; i < individual.getVectorSize()-1; i++){
				fitness += 100*Math.pow(individual.objectVariable[i+1] - Math.pow(individual.objectVariable[i], 2), 2) + Math.pow(1 - individual.objectVariable[i], 2);
			}
			
			return fitness;
		};
		
		final Supplier<double[]> initialVectorSupplier = () -> {
			double[] vector = new double[problemDimension];

			for(int i = 0; i < vector.length; i++)
				vector[i] = 5;
			
			return vector;
		};
		
		IRecombinator recombinatorObject = new Discrete();
		IRecombinator recombinatorStrategy = new Intermediate();
		IRecombinator recombinatorAngles = new None();
		
		List<Individual> parents = null, children = new ArrayList<>();
		
		int parentCount = 4;
		int childCount = 20;
		
		final double minFitness = 1E-5;
		
		Individual bestSoFar = null;
		
		//algorithm
		parents = generateParents(parentCount, initialVectorSupplier);
		evaluate(parents, objectiveFunction);
		sort(parents);
		bestSoFar = getBestSoFar(parents, bestSoFar);
//		print(bestSoFar);
				
		long steps = 0;
		do{
			
			recombine(parents, children, childCount, recombinatorObject, recombinatorStrategy, recombinatorAngles);
			mutate(children, false);
			evaluate(children, objectiveFunction);
			select(sort(type.getPossibleParents(parents, children)), parents);

			Individual temp = bestSoFar;
			bestSoFar = getBestSoFar(parents, bestSoFar);
			
			if(bestSoFar != temp){
				System.out.println("Step: " + steps);
				print(bestSoFar);
			}
			steps++;
		}while(bestSoFar.fitness > minFitness);// && maxSteps > steps
		
//		System.out.println();
//		System.out.println("Result after " + steps + " steps:");
//		print(bestSoFar);
//		
		//Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("" + steps + "\n"), null);
		return steps;
	}
	
	static void print(Individual individual){
		System.out.println("Individual score=" + individual.fitness);
		System.out.print("\tobject: ");
		System.out.println(Texts.n4(individual.objectVariable));

		System.out.print("\tstddev: ");
		System.out.println(Texts.n4(individual.standardDeviations));
		System.out.println();
	}
	
	static Individual getBestSoFar(List<Individual> parents, Individual bestSoFar){

		if(bestSoFar == null)
			return parents.get(0);
		else
			return parents.get(0).fitness <= bestSoFar.fitness ? parents.get(0) : bestSoFar;
	}
	
	static void select(List<Individual> individuals, List<Individual> parents){
		
		for(int i = 0; i < parents.size(); i++)
			parents.set(i, individuals.get(i));
	}
	
	static void mutate(List<Individual> individuals, boolean successRate){
		for(Individual i : individuals)
			Mutator.mutate(i, 3);
	}
	
	static void evaluate(List<Individual> individuals, IObjectiveFunction objectiveFunction){
		for(Individual i : individuals)
			i.calculateFitness(objectiveFunction);
	}
	
	static List<Individual> sort(List<Individual> individuals){
		individuals.sort(Comparator.comparingDouble(i -> i.fitness));
		return individuals;
	}
	
	static void recombine(List<Individual> parents, List<Individual> children, int childCount, IRecombinator recombinatorObject, IRecombinator recombinatorStrategy, IRecombinator recombinatorAngles){
		
		if(recombinatorObject == null)recombinatorObject = new None();
		if(recombinatorStrategy == null)recombinatorStrategy = new None();
		if(recombinatorAngles == null)recombinatorAngles = new None();
		
		if(childCount < parents.size())
			throw new RuntimeException("ChildCount must be greater or equal to ParentCount!");
		
		children.clear();

		for(int i = 0; i < childCount; i++){
			Individual child = new Individual();
			
			child.objectVariable = recombinatorObject.recombine(parents.get(0).objectVariable, parents.get(1).objectVariable);
			child.standardDeviations = recombinatorStrategy.recombine(parents.get(0).standardDeviations, parents.get(1).standardDeviations);
			
			children.add(child);
		}
	}
	
	static List<Individual> chooseParents(List<Individual> parents){
		
		Random random = new Random();
		List<Individual> remainingPossibleParents = new LinkedList<>(parents);
		List<Individual> childParents = new ArrayList<>();
		for(int i = 0; i < 2; i++){
			
			Individual chosenParent = remainingPossibleParents.remove(random.nextInt(remainingPossibleParents.size()));
			childParents.add(chosenParent);
		}
		
		return childParents;
	}

	static List<Individual> generateParents(int parentCount, Supplier<double[]> initialVectorSupplier){
		List<Individual> parents = new ArrayList<>();

		for(int i = 0; i < parentCount; i++){

			Individual individual = new Individual();

			individual.objectVariable = initialVectorSupplier.get();
			double[] standardDeviations = new double[individual.getVectorSize()];
			Arrays.fill(standardDeviations, 1);
			individual.standardDeviations = standardDeviations;

			parents.add(individual);
		}

		return parents;
	}
}
