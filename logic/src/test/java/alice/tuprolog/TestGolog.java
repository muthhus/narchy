package alice.tuprolog;

import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class TestGolog {
    
    @Test
    public void golog1() throws Exception {


        Prolog p = new Prolog();

        p.setSpy(true);
        p.addExceptionListener(e -> System.out.println(e));
        p.addOutputListener(e -> System.out.println(e));
        //p.addQueryListener(e -> System.out.println(e));


        p.addLibrary("alice.tuprolog.lib.EDCGLibrary");
        p.addTheory(theory("golog.pl"));
        p.addTheory(theory("golog.elevator.pl"));


        p.solve(p.term("nextFloor(M,s0)."), (s) -> {
            System.out.println(s);

        }, 0);




    }

    public static Theory theory(String classPath) throws IOException, URISyntaxException, InvalidTheoryException {
        return new Theory(source(classPath));
    }

    public static String source(String classPath) throws IOException, URISyntaxException {
        return Resources.toString(TestGolog.class.getResource(classPath).toURI().toURL(), java.nio.charset.Charset.defaultCharset());
    }

}
