package objenome0.evolve;

import junit.framework.TestCase;
import objenome0.problem.ProblemSTGP;
import objenome0.problem.STGPRegression;
import objenome0.solver.evolve.Population;
import objenome0.solver.evolve.TypedOrganism;
import objenome0.util.BenchmarkSolutions;
import org.junit.Ignore;
import org.junit.Test;

@Ignore //until API is finished
public class STGPRegressionTest extends TestCase {

	@Test public void testRegression() {
            
            ProblemSTGP e = new STGPRegression(20, BenchmarkSolutions.XpXXpXXX);
            
            Population<TypedOrganism> p = e.cycle();
            
            TypedOrganism best = p.best();
            
            //assertTrue(best.depth() > 1);            
            assertEquals(100, p.size());
            assertNotNull(p.best());

            
            System.out.println(p.best());
            System.out.println(p.size());            
            System.out.println(p);            
            System.out.println(best.evaluate());
            
            
            
	}
}