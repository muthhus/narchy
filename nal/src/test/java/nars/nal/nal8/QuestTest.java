package nars.nal.nal8;

import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.nal.Tense;
import nars.nar.Default;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/26/15.
 */
public class QuestTest {

    int exeCount;

    @Test
    public void testQuest() throws Narsese.NarseseException {

        Global.DEBUG = true;


        NAR nar = new Default(1, 1, 1, 1);

        //nar.log();

        nar.goal(nar.term("a:b"), Tense.Eternal, 1.0f, 0.9f);
        nar.step();

        AtomicBoolean valid = new AtomicBoolean(false);

        nar.onAnswer(nar.task("a:?b@"), a -> {
            //System.out.println("answer: " + a);
            //System.out.println(" " + a.getLog());
            if (a.toString().contains("(b-->a)!"))
                valid.set(true);
        });

        nar.run(100);

        assertTrue(valid.get());
    }


}
