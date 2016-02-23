package nars.nal.meta;

import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/31/15.
 */
public final class Return extends Atom implements ProcTerm {

    public static final ProcTerm the = new Return();

    private Return() {
        super("return");
    }


    @Override
    public void appendJavaProcedure(@NotNull StringBuilder s) {
        s.append("return;");
    }

    @Override
    public void accept(PremiseEval versioneds) {

    }

}
