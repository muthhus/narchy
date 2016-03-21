package nars.concept;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.nar.Default;
import nars.task.Task;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by me on 3/21/16.
 */
public class TermTemplateTest {

    @Test
    public void testTemplate1() {
        NAR n = new Default();
        Task t = n.inputTask("((($3-->(/,REPR,_,$4))&&($1-->(/,REPR,_,$2)))==>({($1,$2),($3,$4)}-->REPR)).");
        n.step();
        Concept c = t.concept(n);
        List<TermTemplate> templates = c.termlinkTemplates();
        System.out.println(Joiner.on('\n').join(templates));
        assertEquals(7, templates.size());
        assertEquals("({($1,$2),($3,$4)}-->REPR)", templates.get(0).term.toString());
        assertEquals(0.08f, templates.get(0).strength, 0.01f);
        assertEquals(0.16f, templates.get(1).strength, 0.01f);

    }

}