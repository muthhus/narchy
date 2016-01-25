package nars.term;

import nars.Op;
import nars.Symbols;
import nars.nal.nal8.Operator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static nars.Symbols.*;

/**
 * Created by me on 1/2/16.
 */
public interface TermPrinter {
    static void appendSeparator(@NotNull Appendable p, boolean pretty) throws IOException {
        p.append(ARGUMENT_SEPARATOR);
        if (pretty) p.append(' ');
    }

    static void writeCompound1(@NotNull Op op, @NotNull Term singleTerm, @NotNull Appendable writer, boolean pretty) throws IOException {
        writer.append(COMPOUND_TERM_OPENER);
        writer.append(op.str);
        writer.append(ARGUMENT_SEPARATOR);
        singleTerm.append(writer, pretty);
        writer.append(COMPOUND_TERM_CLOSER);
    }

    static void compoundAppend(@NotNull Compound c, @NotNull Appendable p, boolean pretty) throws IOException {

        p.append(COMPOUND_TERM_OPENER);

        c.op().append(c, p);

        if (c.size() == 1)
            p.append(ARGUMENT_SEPARATOR);

        c.appendArgs(p, pretty, true);

        appendCloser(p);

    }

    static void appendCloser(@NotNull Appendable p) throws IOException {
        p.append(COMPOUND_TERM_CLOSER);
    }

    static void append(@NotNull Compound c, @NotNull Appendable p, boolean pretty) throws IOException {
        final Op op = c.op();

        switch (op) {
            case SET_INT_OPENER:
            case SET_EXT_OPENER:
                setAppend(c, p, pretty);
                break;
            case PRODUCT:
                productAppend(c, p, pretty);
                break;
            case IMAGE_INT:
            case IMAGE_EXT:
                imageAppend(c, p, pretty);
                break;
            //case INHERIT: inheritAppend(c, p, pretty); break;
            //case SIMILAR: similarAppend(c, p, pretty); break;
            default:
                if (op.isStatement() || c.size()==2) {
                    if (Op.isOperation(c)) {
                        operationAppend((Compound) c.term(0), (Operator) c.term(1), p, pretty); //TODO Appender
                    } else {
                        statementAppend(c, p, pretty, op);
                    }
                } else {
                    compoundAppend(c, p, pretty);
                }
                break;
        }
    }

    static void inheritAppend(@NotNull Compound c, @NotNull Appendable p, boolean pretty) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(Symbols.COMPOUND_TERM_OPENER);
        b.append(p, pretty);
        p.append(Symbols.INHERIT_SEPARATOR);
        a.append(p, pretty);
        p.append(Symbols.COMPOUND_TERM_CLOSER);
    }
    static void similarAppend(@NotNull Compound c, @NotNull Appendable p, boolean pretty) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(Symbols.COMPOUND_TERM_OPENER);
        a.append(p, pretty);
        p.append(Symbols.SIMILAR_SEPARATOR);
        b.append(p, pretty);
        p.append(Symbols.COMPOUND_TERM_CLOSER);
    }

    static void statementAppend(@NotNull Compound c, @NotNull Appendable p, boolean pretty, @NotNull Op op) throws IOException {
        Term a = Statement.subj(c);
        Term b = Statement.pred(c);

        p.append(COMPOUND_TERM_OPENER);
        a.append(p, pretty);

        sep(p, pretty);

        op.append(c, p);

        sep(p, pretty);

        b.append(p, pretty);

        p.append(COMPOUND_TERM_CLOSER);
    }

    static void sep(@NotNull Appendable w, boolean pretty) throws IOException {
        if (pretty) w.append(' ');
    }

    static void productAppend(@NotNull Compound product, @NotNull Appendable p, boolean pretty) throws IOException {

        int s = product.size();
        p.append(COMPOUND_TERM_OPENER);
        for (int i = 0; i < s; i++) {
            product.term(i).append(p, pretty);
            if (i < s - 1) {
                p.append(pretty ? ", " : ",");
            }
        }
        p.append(COMPOUND_TERM_CLOSER);
    }

    static void imageAppend(@NotNull Compound image, @NotNull Appendable p, boolean pretty) throws IOException {

        int len = image.size();

        p.append(COMPOUND_TERM_OPENER);
        p.append(image.op().str);

        int relationIndex = image.relation();
        int i;
        for (i = 0; i < len; i++) {
            Term tt = image.term(i);

            p.append(ARGUMENT_SEPARATOR);
            if (pretty) p.append(' ');

            if (i == relationIndex) {
                p.append(Symbols.IMAGE_PLACE_HOLDER);
                p.append(ARGUMENT_SEPARATOR);
                if (pretty) p.append(' ');
            }

            tt.append(p, pretty);
        }
        if (i == relationIndex) {
            p.append(ARGUMENT_SEPARATOR);
            if (pretty) p.append(' ');
            p.append(Symbols.IMAGE_PLACE_HOLDER);
        }

        p.append(COMPOUND_TERM_CLOSER);

    }

    static void setAppend(@NotNull Compound set, @NotNull Appendable p, boolean pretty) throws IOException {

        int len = set.size();

        //duplicated from above, dont want to store this as a field in the class
        char opener, closer;
        if (set.op(Op.SET_EXT)) {
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
            tt.append(p, pretty);
        }
        p.append(closer);
    }

    static void operationAppend(@NotNull Compound argsProduct, @NotNull Operator operator, @NotNull Appendable p, boolean pretty) throws IOException {

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
                if (pretty)
                    p.append(' ');
            }

            t.append(p, pretty);


            n++;
        }

        p.append(COMPOUND_TERM_CLOSER);

    }
}
