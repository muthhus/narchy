package nars.concept;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Task;
import nars.nar.Default;
import nars.term.container.TermContainer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/21/16.
 */
public class TermTemplateTest {

    @Test
    public void testTemplate1() {
        testTemplate(
                "((($3-->(/,REPR,_,$4))&&($1-->(/,REPR,_,$2)))==>({($1,$2),($3,$4)}-->REPR))",
                6, "({($1,$2),($3,$4)}-->REPR)");
    }

    @Test
    public void testTemplate2() {
        testTemplate("goto($1)", 3, "goto  $1  ($1)");
        testTemplate("a(b)", 3, "a  b  (b)");
        testTemplate("b(a)", 3, "a  b  (a)");
        testTemplate("(?1-->(a&b))", 4, "a  b  ?1  (a&b)");
    }

    public void testTemplate(String term, int count, String expected) {
        NAR n = new Default();
        Task t = n.inputAndGet(term+".");
        n.next();

        //n.concepts.print(System.out);

        Concept c = t.concept(n);

        TermContainer templates = c.templates();
        String s = Joiner.on("  ").join(templates);

        String msg = term + "\t templates=" + s;

        assertEquals(msg, count, templates.size());
        assertTrue(msg, s.contains(expected));

        //DoubleSummaryStatistics ss = templates.stream().mapToDouble(l -> l.strength).summaryStatistics();
        //System.out.println(ss);
        //assertEquals(1f, ss.getSum(), 0.01f);

        //assertTrue(ss.getMax() - ss.getAverage() > 0.02); //some variation due to repeat subterms

    }

}