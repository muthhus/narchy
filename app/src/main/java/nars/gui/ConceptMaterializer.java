package nars.gui;

import nars.term.Termed;

import java.util.function.Function;

/**
 * Created by me on 6/26/16.
 */
public interface ConceptMaterializer extends Function<Termed, ConceptWidget> {


    @Override
    default ConceptWidget apply(Termed x) {
        return new ConceptWidget(x, numEdgesFor(x));
    }

    int numEdgesFor(Termed x);

}
