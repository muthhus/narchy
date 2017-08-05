package nars.term.obj;

import org.junit.Test;

import javax.measure.Quantity;

import static org.junit.Assert.*;

public class QuantityCompoundTest {

    @Test
    public void test1() {
        //Unit<?> q = AbstractUnit.parse("km");


        QuantityCompound q = QuantityCompound.the("5 km");
        assertEquals("(km,5)", q.toString());
        QuantityCompound r = QuantityCompound.the("1 s");
        Quantity<?> qDivR = q.quant.divide(r.quant);
        assertEquals("5 km/s", qDivR.toString());

    }
}