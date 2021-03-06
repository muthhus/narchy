package nars.op;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by me on 3/30/17.
 */
public class DepIndepVarIntroductionTest {

    final NAR n = NARS.shell();

    @Test
    public void testIntroduceIndepVar() throws Narsese.NarseseException {

        assertEquals("[((a-->$1)==>(b-->$1))]",
                introduce("((a-->c)==>(b-->c))", 16).toString());

        assertEquals("[((a-->$1)=|>(b-->$1))]",
                introduce("((a-->c)=|>(b-->c))", 16).toString());

    }

    @Test
    public void testIntroduceDepVar() throws Narsese.NarseseException {

        assertEquals("[((a-->#1)&&(b-->#1))]",
                introduce("(&&,(a-->c),(b-->c))", 16).toString());

        assertEquals("[((#1-->a)&&(#1-->b))]",
                introduce("(&&,(c-->a),(c-->b))", 16).toString());

        assertEquals("[(&&,(c-->#1),(#1-->a),(#1-->b))]",
                introduce("(&&,(x-->a),(x-->b),(c-->x))", 16).toString());

        assertEquals("[(&&,(c-->(a,b,#1)),(#1-->a),(#1-->b)), (&&,(c-->(a,#1,x)),(x-->a),(x-->#1)), (&&,(c-->(#1,b,x)),(x-->b),(x-->#1))]",
                introduce("(&&,(x-->a),(x-->b),(c-->(a,b,x)))", 16).toString());

    }

    private TreeSet<Term> introduce(String term, int iterations) throws Narsese.NarseseException {
        TreeSet<Term> s = new TreeSet();
        for (int i = 0; i < iterations; i++) {
            Term u = $.$("varIntro(" + term + ")").eval(n.terms);
            if (u!=null)
                s.add(u);
        }
        return s;
    }
}