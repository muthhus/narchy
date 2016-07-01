package asanf.FOM;

import asanf.FOM.Util.BMI;
import asanf.FOM.Util.CorrelationFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class MainTest {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		TermFrequencies<String> tf = new TermFrequencies<String>();
		String[] tags = new String[2];
		tags[0] = "NN";
		//tags[1] = "NN";
		int windowSize = 10;
		// vecchi reuters 0.01 / 0.03
		double lower = 0.03;
		double upper = 0.55;
		double alpha = 0.02;
		double beta = 0.55;
		double lambda = 0.022;
		String[] path = new String[3];
		path[1] = "/tmp/u";
		path[2] = "Hackers";

		tf = FuzzyOntologyMiner.extractFrequencies("/tmp/u/silent_weapons_quiet_wars.txt" , windowSize, tags);

		//System.out.println("totWin: " + tf.getTotWindows());

		tf.filterTerms(lower, upper);
			

		
		for(String e: tf){
			System.out.println(e + ": " + tf.getFrequency(e));
		}
		
		for(String i: tf){
			for(String j: tf){
				if(!i.equals(j)){
					double f = tf.getFrequency(i, j);
					if( f > 0 )
						System.out.println(i + " - " + j + ": " + f);
				}
			}
		}
		
		ContextVectors<String> cv;
		CorrelationFunction cf = new BMI(beta);
		System.out.println("Creo i vettori di contesto");
		cv = FuzzyOntologyMiner.createContextVectors(tf, cf, alpha);
		
		ArrayList<String> concepts = cv.getConcepts();
		Collection<String> terms = tf.getTerms();
		
		System.out.println(concepts.size() != terms.size());
		
		
		for(String concept: concepts)
			for(String term: terms)
				if(!concept.equals(term)){
					double mem = cv.getMembership(concept, term);
					
					System.out.println(term + " in " + concept + ": " +mem);
				}
		
		Taxonomy<String> taxonomy;
		
		System.out.println("Creo la tassonomia");
		taxonomy = FuzzyOntologyMiner.createTaxonomy(cv, lambda);
		taxonomy.taxonomyPruning();
		
		for(String c1: concepts)
			for(String c2: concepts){
				double spec = taxonomy.getSpecificity(c1, c2);
				if(spec > 0)
					System.out.println(c1 + " is " + c2 + ": " + taxonomy.getSpecificity(c1, c2));
			}
		
	}

}
