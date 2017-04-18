package alice.tuprolog;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static alice.tuprolog.TestGolog.source;
import static alice.tuprolog.TestGolog.theory;
import static org.junit.Assert.assertEquals;

public class TestEinsteinRiddle {
    
    @Test
    public void einsteinsRiddle() throws InterruptedException, IOException, URISyntaxException, InvalidTheoryException {

        final boolean[] finished = {false};

        //The answer is the German owns the fish.
        new Prolog()
            .addTheory(theory("einsteinsRiddle.pl"))
            .solve("einstein(_,X), write(X).", o -> {
                System.out.println(o);
                if (finished[0])
                    return;

                assertEquals("yes.\nX / german", o.toString());
                finished[0] = true;
            });
    }
    
}
