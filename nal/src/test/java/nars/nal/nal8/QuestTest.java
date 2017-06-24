package nars.nal.nal8;

import nars.NAR;
import nars.Narsese;
import nars.nar.NARBuilder;
import nars.time.Tense;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$;
import static nars.Op.QUEST;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/26/15.
 */
public class QuestTest {


    @Test public void testQuestAfterGoal1() throws Narsese.NarseseException {
        testQuest(true, 0, 256);
    }
    @Test public void testQuestAfterGoal2() throws Narsese.NarseseException {
        testQuest(true, 1, 256);
    }
    @Test public void testQuestAfterGoal3() throws Narsese.NarseseException {
        testQuest(true, 4, 256);
    }

    @Test
    public void testQuestBeforeGoal() throws Narsese.NarseseException {
        testQuest(false, 1, 32);
        testQuest(false, 4, 32);
    }
    @Test
    public void testQuestBeforeGoal0() throws Narsese.NarseseException {
        testQuest(false, 0, 64);
    }


    public void testQuest(boolean goalFirst, int timeBetween, int timeAfter) throws Narsese.NarseseException {
        //Param.DEBUG = true;
        final NAR nar = new NARBuilder().get();
        //nar.log();

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

    public void question(NAR nar, AtomicBoolean valid) throws Narsese.NarseseException {
        nar.question($("a:?b@"), ETERNAL, QUEST, (q, a) -> {
            //System.out.println("answer: " + a);
            //System.out.println(" " + a.getLog());
            if (a.toString().contains("(b-->a)!"))
                valid.set(true);
        });
    }

    public void goal(NAR nar) throws Narsese.NarseseException {
        nar.goal(nar.term("a:b"), Tense.Eternal, 1.0f, 0.9f);
    }


}
