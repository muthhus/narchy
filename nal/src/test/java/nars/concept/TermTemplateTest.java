package nars.concept;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import org.junit.Test;

import java.util.DoubleSummaryStatistics;
import java.util.List;

import static nars.Global.dereference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/21/16.
 */
public class TermTemplateTest {

    @Test
    public void testTemplate1() {
        NAR n = new Default();
        Task t = n.inputTask("((($3-->(/,REPR,_,$4))&&($1-->(/,REPR,_,$2)))==>({($1,$2),($3,$4)}-->REPR)).");
        n.step();

        n.index.print(System.out);

        Concept c = t.concept(n);

        List<Termed> templates = dereference(((CompoundConcept)c).termLinkTemplates);
        assertEquals(12, templates.size());

        String s = Joiner.on('\n').join(templates);
        System.out.println(s);
        assertTrue(s.contains("({($1,$2),($3,$4)}-->REPR)"));

        //DoubleSummaryStatistics ss = templates.stream().mapToDouble(l -> l.strength).summaryStatistics();
        //System.out.println(ss);
        //assertEquals(1f, ss.getSum(), 0.01f);

        //assertTrue(ss.getMax() - ss.getAverage() > 0.02); //some variation due to repeat subterms

    }

}