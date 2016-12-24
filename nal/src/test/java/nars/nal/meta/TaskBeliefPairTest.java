package nars.nal.meta;

import nars.$;
import nars.Narsese;
import nars.term.Compound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 8/29/15.
 */
public class TaskBeliefPairTest {

    final Narsese parse = Narsese.the();

    @Test
    public void testNoNormalization() throws Exception {

        String a = "<x --> #1>";
        String b = "<y --> #1>";
        Compound p = $.p(
            parse.term(a),
            parse.term(b)
        );
        String expect = "((x-->#1),(y-->#1))";
        assertEquals(expect, p.toString());

//        Term pn = p.normalized();
//        assertEquals(expect, pn.toString());
//
//        p.set((Term)parse.term(a), parse.term(b));
//        assertEquals(expect, p.toString());
    }
}