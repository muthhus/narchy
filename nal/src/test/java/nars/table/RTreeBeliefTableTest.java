package nars.table;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.TaskConcept;
import nars.nar.Terminal;
import nars.term.Compound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RTreeBeliefTableTest {

    @Test
    public void testBasicOperations() throws Narsese.NarseseException {
        NAR n = new Terminal();
        TaskConcept X = (TaskConcept) n.conceptualize($.$("a:b"));
        RTreeBeliefTable t = new RTreeBeliefTable(4);

        assertEquals(0, t.size());

        Compound x = X.term();
        Task a = $.belief(x, 1f, 0.9f).time(1).apply(n); a.pri(0.5f);
        t.add(a, X, n);
        assertEquals(1, t.size());

        t.add(a, X, n);
        assertEquals(1, t.size()); //no change for inserted duplicate

        Task b = $.belief(x, 0f, 0.9f).time(3).apply(n); b.pri(0.5f);
        t.add(b, X, n);
        Task c = $.belief(x, 0.1f, 0.9f).time(3).apply(n); c.pri(0.5f);
        t.add(c, X, n);

        Task d = $.belief(x, 0.1f, 0.9f).time(0,3, 4).apply(n); d.pri(0.5f);
        t.add(d, X, n);

        assertEquals(4, t.size()); //no change for inserted duplicate

        t.print(System.out);
    }

}
