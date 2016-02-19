package nars.term;

import nars.nar.Terminal;
import nars.term.variable.CommonVariable;
import nars.term.variable.GenericNormalizedVariable;
import nars.term.variable.GenericVariable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {

    Terminal p = new Terminal();

    @Test
    public void commonVariableTest1() {
        assertEquals("%1%2",
                CommonVariable.make(
                        (GenericVariable)p.term("%1"),
                        (GenericVariable)p.term("%2")).toString(),

        //reverse order
                CommonVariable.make(
                        (GenericVariable)p.term("%2"),
                        (GenericVariable)p.term("%1")).toString());
    }

    @Test
    public void commonVariableTest2() {
        //different lengths
        assertEquals("%12%2",
                CommonVariable.make(
                        (GenericVariable)p.term("%12"),
                        (GenericVariable)p.term("%2")).toString(),
        //different lengths
                CommonVariable.make(
                        (GenericVariable)p.term("%2"),
                        (GenericVariable)p.term("%12")).toString());

    }

    @Ignore
    @Test
    public void commonVariableInstancing() {
        //different lengths

        GenericNormalizedVariable ca = CommonVariable.make(
                (GenericVariable)p.term("%1"),
                (GenericVariable)p.term("%2"));
        GenericNormalizedVariable cb = CommonVariable.make(
                (GenericVariable)p.term("%2"),
                (GenericVariable)p.term("%1"));

        assertEquals(ca, cb);
        Assert.assertTrue("efficient re-use of common variable of name length=1", ca == cb);
    }
}