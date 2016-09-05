package nars.nal.nal8;

import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import nars.time.Tense;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/26/15.
 */
public class QuestTest {


    @Test
    public void testQuestAfterGoal()  {
        testQuest(true, 0, 16);
        testQuest(true, 1, 16);
        testQuest(true, 4, 16);
    }

    @Test
    public void testQuestBeforeGoal()  {
        testQuest(false, 0, 16);
        testQuest(false, 1, 16);
        testQuest(false, 4, 16);
    }

    public void testQuest(boolean goalFirst, int timeBetween, int timeAfter) throws Narsese.NarseseException {
        final NAR nar = new Default();

        AtomicBoolean valid = new AtomicBoolean(false);

        if (goalFirst) {
            goal(nar);
            nar.run(timeBetween);
            question(nar, valid);
        } else {
            question(nar, valid);
            nar.run(timeBetween);
            goal(nar);
        }

        nar.run(timeAfter);

        assertTrue(valid.get());
    }

    public void question(NAR nar, AtomicBoolean valid) {
        nar.ask($("a:?b@"), ETERNAL, '@', a -> {
            //System.out.println("answer: " + a);
            //System.out.println(" " + a.getLog());
            if (a.toString().contains("(b-->a)!"))
                valid.set(true);
            return true;
        });
    }

    public void goal(NAR nar) {
        nar.goal(nar.term("a:b"), Tense.Eternal, 1.0f, 0.9f);
    }


}
