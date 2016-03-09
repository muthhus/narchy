package nars.op.sys.scheme;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.nal.nal8.operator.TermFunction;
import nars.op.sys.scheme.cons.Cons;
import nars.op.sys.scheme.expressions.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nars.op.sys.scheme.DefaultEnvironment.load;
import static nars.op.sys.scheme.expressions.Expression.NONE;


public class scheme extends TermFunction {

    @Deprecated public static final nars.op.sys.scheme.SchemeClosure env = nars.op.sys.scheme.DefaultEnvironment.newInstance();



    /** adapter class for NARS term -> Scheme expression; temporary until the two API are merged better */
    public static class SchemeProduct extends ListExpression {


        public SchemeProduct(@NotNull Iterable<Term> p) {
            super(Cons.copyOf( Iterables.transform(p, (Term term) -> {

                if (term instanceof Iterable) {
                    //return ListExpression.list(SymbolExpression.symbol("quote"), new SchemeProduct((Product)term));
                     return new SchemeProduct((Iterable) term);
                }
                if (term instanceof Atomic) {

                    String s = ((Atomic)term).toStringUnquoted();

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

    //TODO use termizer

    @Deprecated public static final Function<Object, Term> schemeToNars = new Function<Object, Term>() {
        @NotNull
        @Override
        public Term apply(Object schemeObj) {

            if (schemeObj instanceof Term)
                return (Term)schemeObj;

            if (schemeObj instanceof ListExpression) {
                return apply( ((ListExpression)schemeObj).value );
            } else if (schemeObj instanceof SymbolicProcedureExpression) {
                Cons<Expression> exp = ((SymbolicProcedureExpression) schemeObj).exps;
                return apply(exp);
            }
            //TODO handle other types, like Object[] etc
            else if (schemeObj instanceof Expression) {
                //return Term.get("\"" + schemeObj.print() + "\"" );
                //return Atom.the(Utf8.toUtf8(name));

                return schemeObj == NONE ? null : $.the(((Expression)schemeObj).print());

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
            throw new RuntimeException("Invalid expression for term: " + schemeObj);

        }

        public Term apply(@NotNull Iterable<Expression> e) {
            List<Term> elements = Lists.newArrayList(StreamSupport.stream(e.spliterator(), false).map(schemeToNars::apply).collect(Collectors.toList()));
            return $.p( elements );
        }
    };

    @Override
    public Object function(@NotNull Compound o, TermIndex i) {
        Term[] x = o.terms();
        return eval(x[0]);

        //return null;
    }

    public static Object eval(Term x) {

        if (x instanceof Atomic) {
            //interpret as eval string
            return schemeToNars.apply(
                Evaluator.evaluate(
                    load(((Atomic)x).toStringUnquoted(), env), env)
            );
        }

        return schemeToNars.apply(
            Evaluator.evaluate(
                new SchemeProduct(x instanceof Compound ? (Iterable) x : $.p(x)), env));
        //Set = evaluate as a cond?
//        else {
//
//        }
    }


}
