package nars.op.sys.scheme;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.nal.nal8.operator.TermFunction;
import nars.op.sys.scheme.cons.Cons;
import nars.op.sys.scheme.expressions.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nars.op.sys.scheme.DefaultEnvironment.load;


public class scheme extends TermFunction {

    @Deprecated public static final nars.op.sys.scheme.SchemeClosure env = nars.op.sys.scheme.DefaultEnvironment.newInstance();



    /** adapter class for NARS term -> Scheme expression; temporary until the two API are merged better */
    public static class SchemeProduct extends ListExpression {


        public SchemeProduct(@NotNull Iterable<Term> p) {
            super((Cons<Expression>)Cons.copyOf( Iterables.transform(p, (Term term) -> {

                if (term instanceof Iterable) {
                    //return ListExpression.list(SymbolExpression.symbol("quote"), new SchemeProduct((Product)term));
                     return new SchemeProduct((Iterable) term);
                }
                if (term instanceof Atom) {

                    String s = ((Atom)term).toStringUnquoted();

                    //attempt to parse as number
                    try {
                        double d = Double.parseDouble(s);
                        return new NumberExpression((long)d);
                    }
                    catch (NumberFormatException e) { }

                    //atomic symbol
                    return new SymbolExpression(s);
                }
                throw new RuntimeException("Invalid term for scheme: " + term);


            }

            )));
        }
    }

    //TODO make narsToScheme method

    public static final Function<Expression, Term> schemeToNars = new Function<Expression, Term>() {
        @NotNull
        @Override
        public Term apply(Expression schemeObj) {
            if (schemeObj instanceof ListExpression) {
                return apply( ((ListExpression)schemeObj).value );
            } else if (schemeObj instanceof SymbolicProcedureExpression) {
                Cons<Expression> exp = ((SymbolicProcedureExpression) schemeObj).exps;
                return apply(exp);
            }
            //TODO handle other types, like Object[] etc
            else {
                //return Term.get("\"" + schemeObj.print() + "\"" );
                //return Atom.the(Utf8.toUtf8(name));

                return $.the(schemeObj.print());

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

                //  }
            }
            //throw new RuntimeException("Invalid expression for term: " + schemeObj);

        }

        public Term apply(@NotNull Iterable<Expression> e) {
            List<Term> elements = Lists.newArrayList(StreamSupport.stream(e.spliterator(), false).map(schemeToNars::apply).collect(Collectors.toList()));
            return $.p( elements );
        }
    };

    @Override
    public Term function(@NotNull Compound o, TermBuilder i) {
        Term[] x = o.terms();
        Term code = x[0];

        if (code instanceof Atom) {
            //interpret as eval string
            Atom a = (Atom)code;

            return schemeToNars.apply(
                Evaluator.evaluate(
                    load(a.toStringUnquoted(), env), env)
            );

        }

        return code instanceof Compound ?
                schemeToNars.apply(
                    Evaluator.evaluate(
                        new SchemeProduct(((Iterable) code)), env)) :
                schemeToNars.apply(Evaluator.evaluate(new SchemeProduct($.p(x)), env));
        //Set = evaluate as a cond?
//        else {
//
//        }

        //return null;
    }


}
