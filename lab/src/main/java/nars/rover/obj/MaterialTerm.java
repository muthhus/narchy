package nars.rover.obj;

import com.artemis.Component;
import nars.term.Term;

import java.util.function.Supplier;

/**
 * Created by me on 3/30/16.
 */
public class MaterialTerm extends Component {

    public Supplier<Term> term;

    public MaterialTerm(Supplier<Term> term) {
        this.term = term;
    }
}
