package nars.term.obj;

import jcog.math.NumberException;
import nars.$;
import nars.term.ProxyTerm;
import tec.uom.se.AbstractQuantity;
import tec.uom.se.AbstractUnit;
import tec.uom.se.unit.TransformedUnit;

import javax.measure.Quantity;
import javax.measure.Unit;
import java.text.ParseException;

public class QuantityCompound extends ProxyTerm {

    public final Quantity<?> quant;

    public QuantityCompound(Quantity<?> q) {
        super( $.p( $.the(q.getUnit()), $.the( q.getValue() ) ) );
        this.quant = q;
    }

    public static QuantityCompound the(String toParse) throws IllegalArgumentException {
        Quantity<?> q = AbstractQuantity.parse(toParse);
        return new QuantityCompound(q);
    }

}
