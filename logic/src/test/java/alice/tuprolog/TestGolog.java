package alice.tuprolog;

import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class TestGolog {
    
    @Test
    public void golog1() throws Exception {


        Prolog p = new Prolog();

        p.setSpy(true);
        p.addExceptionListener(System.out::println);
        p.addOutputListener(System.out::println);
        //p.addQueryListener(e -> System.out.println(e));


        p.addLibrary("alice.tuprolog.lib.EDCGLibrary");
        p.addTheory(theory("golog.pl"));
        p.addTheory(theory("golog.elevator.pl"));


        p.solve(p.term("nextFloor(M,s0)."), System.out::println, 0);




    }

    public static Theory theory(String classPath) throws IOException, URISyntaxException, InvalidTheoryException {
        return new Theory(source(classPath));
    }

    public static String source(String classPath) throws IOException, URISyntaxException {
        return Resources.toString(TestGolog.class.getResource(classPath).toURI().toURL(), java.nio.charset.Charset.defaultCharset());
    }

}
