package nars.term;

import nars.Op;
import nars.Symbols;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static nars.Symbols.*;

/**
 * Created by me on 1/2/16.
 */
public interface TermPrinter {

    static void appendSeparator(@NotNull Appendable p) throws IOException {
        p.append(ARGUMENT_SEPARATOR);
        //if (pretty) p.append(' ');
    }

    static void writeCompound1(@NotNull Op op, @NotNull Term singleTerm, @NotNull Appendable writer) throws IOException {
        writer.append(COMPOUND_TERM_OPENER);
        writer.append(op.str);
        writer.append(ARGUMENT_SEPARATOR);
        singleTerm.append(writer);
        writer.append(COMPOUND_TERM_CLOSER);
    }

    static void compoundAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {

        p.append(COMPOUND_TERM_OPENER);

        c.op().append(c, p);

        if (c.size() == 1)
            p.append(ARGUMENT_SEPARATOR);

        c.appendArgs(p, true);

        appendCloser(p);

    }

    static void appendCloser(@NotNull Appendable p) throws IOException {
        p.append(COMPOUND_TERM_CLOSER);
    }

    static void append(@NotNull Compound c, @NotNull Appendable p) throws IOException {
        final Op op = c.op();

        switch (op) {
            case SET_INT_OPENER:
            case SET_EXT_OPENER:
                setAppend(c, p);
                break;
            case PRODUCT:
                productAppend(c, p);
                break;
            case IMAGE_INT:
            case IMAGE_EXT:
                imageAppend(c, p);
                break;
            //case INHERIT: inheritAppend(c, p, pretty); break;
            //case SIMILAR: similarAppend(c, p, pretty); break;
            default:
                if (op.isStatement() || c.size()==2) {
                    if (Op.isOperation(c)) {
                        operationAppend((Compound) c.term(0), (Atomic)c.term(1), p); //TODO Appender
                    } else {
                        statementAppend(c, p, op);
                    }
                } else {
                    compoundAppend(c, p);
                }
                break;
        }
    }

    static void inheritAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(Symbols.COMPOUND_TERM_OPENER);
        b.append(p);
        p.append(Symbols.INHERIT_SEPARATOR);
        a.append(p);
        p.append(Symbols.COMPOUND_TERM_CLOSER);
    }
    static void similarAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(Symbols.COMPOUND_TERM_OPENER);
        a.append(p);
        p.append(Symbols.SIMILAR_SEPARATOR);
        b.append(p);
        p.append(Symbols.COMPOUND_TERM_CLOSER);
    }

    static void statementAppend(@NotNull Compound c, @NotNull Appendable p, @NotNull Op op) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(COMPOUND_TERM_OPENER);
        a.append(p);

        sep(p);

        op.append(c, p);

        sep(p);

        b.append(p);

        p.append(COMPOUND_TERM_CLOSER);
    }

    static void sep(@NotNull Appendable w) throws IOException {
        //if (pretty) w.append(' ');
    }

    static void productAppend(@NotNull Compound product, @NotNull Appendable p) throws IOException {

        int s = product.size();
        p.append(COMPOUND_TERM_OPENER);
        for (int i = 0; i < s; i++) {
            product.term(i).append(p);
            if (i < s - 1) {
                p.append(",");
            }
        }
        p.append(COMPOUND_TERM_CLOSER);
    }

    static void imageAppend(@NotNull Compound image, @NotNull Appendable p) throws IOException {

        int len = image.size();

        p.append(COMPOUND_TERM_OPENER);
        p.append(image.op().str);

        int relationIndex = image.relation();
        int i;
        for (i = 0; i < len; i++) {
            Term tt = image.term(i);

            p.append(ARGUMENT_SEPARATOR);
            //if (pretty) p.append(' ');

            if (i == relationIndex) {
                p.append(Symbols.IMAGE_PLACE_HOLDER);
                p.append(ARGUMENT_SEPARATOR);
                //if (pretty) p.append(' ');
            }

            tt.append(p);
        }
        if (i == relationIndex) {
            p.append(ARGUMENT_SEPARATOR);
            //if (pretty) p.append(' ');
            p.append(Symbols.IMAGE_PLACE_HOLDER);
        }

        p.append(COMPOUND_TERM_CLOSER);

    }

    static void setAppend(@NotNull Compound set, @NotNull Appendable p) throws IOException {

        int len = set.size();

        //duplicated from above, dont want to store this as a field in the class
        char opener, closer;
        if (set.op() == Op.SET_EXT) {
            opener = Op.SET_EXT_OPENER.ch;
            closer = Symbols.SET_EXT_CLOSER;
        } else {
            opener = Op.SET_INT_OPENER.ch;
            closer = Symbols.SET_INT_CLOSER;
        }

        p.append(opener);
        for (int i = 0; i < len; i++) {
            Term tt = set.term(i);
            if (i != 0) p.append(Symbols.ARGUMENT_SEPARATOR);
            tt.append(p);
        }
        p.append(closer);
    }

    static void operationAppend(@NotNull Compound argsProduct, @NotNull Atomic operator, @NotNull Appendable p) throws IOException {

        //Term predTerm = operator.identifier(); //getOperatorTerm();
//        if ((predTerm.volume() != 1) || (predTerm.hasVar())) {
//            //if the predicate (operator) of this operation (inheritance) is not an atom, use Inheritance's append format
//            appendSeparator(p, pretty);
//            return;
//        }


        Term[] xt = argsProduct.terms();

        //append the operator name without leading '^'
        p.append(operator.toString().substring(1));  //predTerm.append(p, pretty);

        p.append(COMPOUND_TERM_OPENER);

        int n = 0;
        for (Term t : xt) {
            if (n != 0) {
                p.append(ARGUMENT_SEPARATOR);
                /*if (pretty)
                    p.append(' ');*/
            }

            t.append(p);


            n++;
        }

        p.append(COMPOUND_TERM_CLOSER);

    }

    public static StringBuilder stringify(Compound c) {
        StringBuilder sb = new StringBuilder(/* conservative estimate */ c.volume()*2 );
        try {
            c.append(sb);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb;
    }
}
