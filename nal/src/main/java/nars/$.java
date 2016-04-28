package nars;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import nars.nal.meta.match.VarPattern;
import nars.task.MutableTask;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import nars.term.variable.AbstractVariable;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import nars.truth.Truth;
import nars.util.data.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Terms.terms;
import static nars.term.Terms.toSet;

/**
 * core utility class for:
    --building any type of value, either programmatically or parsed from string input
       (which can be constructed in a static context)
    --??
 */
public enum $ /* TODO: implements TermIndex */ {
    ;

    public static final org.slf4j.Logger logger = LoggerFactory.getLogger($.class);
    public static final Function<Object, Term> ToStringToTerm = (x) -> $.the(x.toString());

    public static <T extends Term> T $(@NotNull String term) {
        Termed normalized = Narsese.the().term(term, terms, true);
        if (normalized!=null)
            return (T)(normalized.term());
        return null;

        //        try { }
        //        catch (InvalidInputException e) { }
    }

    @Nullable
    @Deprecated public static MutableTask $(@NotNull String term, char punc) {
        return new MutableTask($.$(term), punc);
    }


//    public static @NotNull <O> ObjRef<O> ref(String term, O instance) {
//        return new ObjRef(term, instance);
//    }

    @NotNull
    public static Atom the(@NotNull String id) {
        return new Atom(id);
    }
    @NotNull
    public static Atom quote(String text) {
        return $.the('"' + text + '"');
    }

    @NotNull
    public static Term[] the(@NotNull String... id) {
        int l = id.length;
        Term[] x = new Term[l];
        for (int i = 0; i < l; i++)
            x[i] = the(id[i]);
        return x;
    }


    @NotNull
    public static Atom the(int i) {
        return the(i, 10);
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     *  returns a Term if the two inputs are equal to each other
     */
    @Nullable
    public static Term inh(Term subj, Term pred) {

//        if ((predicate instanceof Operator) && if (subject instanceof Product))
//            return new GenericCompound(Op.INHERITANCE, (Operator)predicate, (Product)subject);
//        else

        return the(INHERIT, subj, pred);
    }


    @Nullable
    public static Term inh(@NotNull String subj, @NotNull String pred) {
        return inh((Term)$(subj), $(pred));
    }


    @Nullable
    public static Term sim(@NotNull Term subj, @NotNull Term pred) {
        return the(SIMILAR, subj, pred);
    }

    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(@NotNull String operator, String... args) {
        return exec(operator(operator), $.p(args));
    }


    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(@NotNull Atomic opTerm, Term... arg) {
        return exec(opTerm, $.p(arg));
    }

//    static Compound oper(Atom opTerm, Compound args) {
//        return oper(new Operator(opTerm), args);
//    }

    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(@NotNull Atomic opTerm, @Nullable Compound arg) {
        return (Compound) the(
                INHERIT,
                arg == null ? Terms.ZeroProduct : arg,
                opTerm
        );
    }


    @Nullable
    public static Term impl(@NotNull Term a, @NotNull Term b) {
        return the(IMPLICATION, a, b);
    }
    @Nullable
    public static Term impl(@NotNull Term a, int dt, @NotNull Term b) {
        return the(IMPLICATION, -1, dt, TermVector.the(a, b));
    }

    @Nullable
    public static Term neg(@NotNull Term x) {
        return the(NEGATE, x);
    }

    @NotNull
    public static Compound p(@NotNull Collection<? super Term> t) {
        return $.p(t.toArray(new Term[t.size()]));
    }

    @NotNull
    public static Compound p(@NotNull Term... t) {
        return (t.length == 0) ? Terms.ZeroProduct : (Compound) the(PRODUCT, t);
    }
    @NotNull
    public static Compound p(@NotNull TermVector t) {
        return p((Term[])t.terms());
    }

    /** creates from a sublist of a list */
    @NotNull
    static Compound p(@NotNull List<Term> l, int from, int to) {
        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return $.p(x);
    }

    @NotNull
    public static Compound/*<Atom>*/ p(@NotNull String... t) {
        return $.p((Term[]) $.the(t));
    }

    public static @NotNull Variable v(@NotNull Op type, @NotNull String name) {
        return new GenericVariable(type, name);
    }


    @NotNull
    @Deprecated public static Variable varDep(int i) {
        return v(VAR_DEP, i);
    }

    public static @NotNull Variable varDep(@NotNull String s) {
        return v(VAR_DEP, s);
    }

    @NotNull
    @Deprecated public static Variable varIndep(int i) {
        return v(VAR_INDEP, i);
    }

    public static @NotNull Variable varIndep(@NotNull String s) {
        return v(VAR_INDEP, s);
    }

    @NotNull
    @Deprecated public static Variable varQuery(int i) {
        return v(VAR_QUERY, i);
    }

    public static @NotNull Variable varQuery(@NotNull String s) {
        return v(VAR_QUERY, s);
    }

    @NotNull
    public static VarPattern varPattern(int i) {
        return (VarPattern) v(VAR_PATTERN, i);
    }


    /**
     * Try to make a new compound from two components. Called by the logic rules.
     * <p>
     *  A {-- B becomes {A} --> B
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    @Nullable
    public static Term inst(@NotNull Term subj, Term pred) {
        return terms.builder().inst(subj, pred);
    }
    @Nullable
    public static Term instprop(@NotNull Term subject, @NotNull Term predicate) {
        return terms.builder().instprop(subject, predicate);
    }
    @Nullable
    public static Term prop(Term subject, Term predicate) {
        return terms.prop(subject, predicate);
    }

//    public static Term term(final Op op, final Term... args) {
//        return builder.term(op, args);
//    }

    @NotNull
    public static MutableTask belief(@NotNull Compound term, @NotNull Truth copyFrom) {
        return belief(term, copyFrom.freq(), copyFrom.conf());
    }

    @NotNull
    public static MutableTask belief(@NotNull Compound term, float freq, float conf) {
        return new MutableTask(term).belief().truth(freq, conf);
    }

    @NotNull
    public static MutableTask goal(@NotNull Compound term, float freq, float conf) {
        return new MutableTask(term).goal().truth(freq, conf);
    }

//    @NotNull
//    public static MutableTask task(@NotNull String s, Memory m) {
//        return Narsese.the().task(s, m);
//    }

    @NotNull
    public static Compound sete(@NotNull Collection<? extends Term> t) {
        return (Compound) the(SET_EXT, -1, (Collection)t);
    }
//    @NotNull
//    public static Compound seteCollection(@NotNull Collection<? extends Object> c) {
//
//        Termizer z = new DefaultTermizer();
//
//        //return (Compound) builder.finish(SET_EXT, -1, TermSet.the(t));
//        return $.sete(
//                (Collection<? extends Term>) c.stream().map(
//                        z::term).collect( toCollection((Supplier<TreeSet>) TreeSet::new)));
//
//    }
    /** construct set_ext of key,value pairs from a Map */
    @NotNull
    public static Compound seteMap(@NotNull Map<Term,Term> map) {
        return $.sete(
                map.entrySet().stream().map(
                        e -> $.p(e.getKey(),e.getValue()))
                        .collect( Collectors.toSet() )
        );
    }
    @NotNull
    public static <X> Compound seteMap(@NotNull Map<Term,? extends X> map, @NotNull Function<X, Term> toTerm) {
        return $.sete(
                map.entrySet().stream().map(
                        e -> $.p(e.getKey(), toTerm.apply(e.getValue())))
                        .collect( Collectors.toSet())
        );
    }

   private static Term[] array(@NotNull Collection<? extends Term> t) {
        return t.toArray(new Term[t.size()]);
    }

    @NotNull
    public static Compound seti(@NotNull Collection<Term> t) {
        return $.seti(array(t));
    }

    @NotNull
    public static Compound sete(Term... t) {
        return (Compound) the(SET_EXT, t);

    }

    /** shorthand for extensional set */
    @NotNull
    public static Compound s(Term... t) {
        return sete(t);
    }

    @NotNull
    public static Compound seti(Term... t) {
        return (Compound) the(SET_INT, t);
    }

//    /**
//     * Try to make a new compound from two components. Called by the logic rules.
//     * <p>
//     *  A --] B becomes A --> [B]
//     * @param subject The first component
//     * @param predicate The second component
//     * @return A compound generated or null
//     */
//    @Nullable
//    public static Term property(Term subject, Term predicate) {
//        return inh(subject, $.seti(predicate));
//    }

    /** unnormalized variable */
    public static @NotNull Variable v(char ch, @NotNull String name) {

//        if (name.length() < 3) {
//            int digit = Texts.i(name, -1);
//            if (digit != -1) {
//                Op op = Variable.typeIndex(ch);
//                return Variable.the(op, digit);
//            }
//        }

        return new GenericVariable(AbstractVariable.typeIndex(ch), name);
    }

    /** normalized variable */
    public static @NotNull AbstractVariable v(@NotNull Op type, int counter) {
        return AbstractVariable.cached(type, counter);
    }

    @Nullable
    public static Term conj(Term... a) {
        return the(CONJUNCTION, a);
    }
    @Nullable
    public static Term conj(int dt, Term... a) {
        return the(CONJUNCTION, -1, dt, TermSet.the(a));
    }

    @Nullable
    public static Term disj(Term... a) {
        return the(DISJUNCTION, a);
    }

    //static {
//        // assume SLF4J is bound to logback in the current environment
//        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
//
//        try {
//            JoranConfigurator configurator = new JoranConfigurator();
//            configurator.setContext(context);
//            // Call context.reset() to clear any previous configuration, e.g. default
//            // configuration. For multi-step configuration, omit calling context.reset().
//            context.reset();
//            //configurator.doConfigure(args[0]);
//        } catch (Exception je) {
//            // StatusPrinter will handle this
//        }
//        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
//
//        Logger logger = LoggerFactory.getLogger($.class);
//        logger.info("Entering application.");
//
//
//
//        logger.info("Exiting application.");
//
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        // print logback's internal status
//        StatusPrinter.print(lc);
//
//        // assume SLF4J is bound to logback-classic in the current environment
//        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        loggerContext.start();
//        //loggerContext.stop();
    //}

    @NotNull
    public static Logger logRoot;

    /** NALogging non-axiomatic logging encoder. log events expressed in NAL terms */
    @NotNull
    public static PatternLayoutEncoder logEncoder;

    static {
        Thread.currentThread().setName("$");

        //http://logback.qos.ch/manual/layouts.html

        try {
            initLogger();
        } catch (Throwable t) {
            System.err.println("Logging Disabled: " + t);
        }
    }

    static void initLogger() {
        logRoot = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        LoggerContext loggerContext = logRoot.getLoggerContext();
        // we are not interested in auto-configuration
        loggerContext.reset();

        logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(loggerContext);
        //logEncoder.setPattern("\\( %highlight(%level),%green(%thread),%yellow(%logger{0}) \\): \"%message\".%n");
        logEncoder.setPattern("\\( %green(%thread),%highlight(%logger{0}) \\): \"%message\".%n");
        logEncoder.start();


        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(logEncoder);
        appender.start();
        logRoot.addAppender(appender);


        //---TEMPORARY---
        SyslogAppender syslog = new SyslogAppender();
        syslog.setPort(10010);
        syslog.setFacility("LOCAL6");
        syslog.setContext(loggerContext);
        syslog.setCharset(Charset.forName("UTF8"));
        syslog.start();

        logRoot.addAppender(syslog);

//        logRoot.debug("Message 1");
//        logRoot.info("Message 1");
//        logRoot.warn("Message 2");
//        logRoot.error("Message 2");

    }

    @Nullable
    public static Term equiv(Term subject, Term pred) {
        return the(EQUIV, subject, pred);
    }

    @Nullable
    public static Term diffInt(Term a, Term b) {
        return the(DIFF_INT, a, b);
    }

    @Nullable
    public static Term diffExt(Term a, Term b) {
        return the(DIFF_EXT, a, b);
    }

    @Nullable
    public static Term imageExt(Term... x) {
        return the(IMAGE_EXT, x);
    }
    @Nullable
    public static Term imageInt(Term... x) {
        return the(IMAGE_INT, x);
    }

    @Nullable
    public static Term esect(Term... x) {
        return the(INTERSECT_EXT, x);
    }
    @Nullable
    public static Term isect(Term... x) { return the(INTERSECT_INT, x); }


    public static @NotNull Operator operator(@NotNull String name) {
        return new Operator(name);
    }


    @Nullable
    public static Term the(@NotNull Op op, Term... subterms) {
        return the(op, -1, subterms);
    }
    @Nullable
    public static Term the(@NotNull Op op, int relation, Term... subterms) {
        return the(op, relation, TermContainer.the(op, subterms));
    }

    @Nullable
    public static Term the(@NotNull Op op, @NotNull Collection<Term> subterms) {
        return the(op, -1, subterms);
    }
    @Nullable
    public static Term the(@NotNull Op op, int relation, @NotNull Collection<Term> subterms) {
        return the(op, relation, TermContainer.the(op, subterms));
    }

    @Nullable
    public static Term the(@NotNull Op op, int relation, @NotNull TermContainer subterms) {
        return the(op, relation, DTERNAL, subterms);
    }
    @Nullable
    public static Term the(@NotNull Op op, @NotNull TermContainer subterms) {
        return the(op, -1, subterms);
    }


    /** returns null if the result is not a compound */
    @Nullable public static Compound compound(@NotNull Op op, @NotNull TermContainer subterms) {
        Term t = the(op, -1, subterms);
        if (!(t instanceof Compound))
            return null;
        return (Compound)t;
    }


    @Nullable
    public static Term the(@NotNull Op op, int relation, int t, @NotNull TermContainer subterms) {
        return terms.the(op, relation, t, subterms);
    }






    /** create a literal atom from a class (it's name) */
    public static Atom the(@NotNull Class c) {
        return $.the(c.getName());
    }


    @NotNull
    public static Atom the(Number o) {

        if (o instanceof Byte) return the(o.intValue());
        if (o instanceof Short) return the(o.intValue());
        if (o instanceof Integer) return the(o.intValue());

        if (o instanceof Long) return the(Long.toString((long)o));

        if ((o instanceof Float) || (o instanceof Double)) return the(o.floatValue());

        return the(o.toString(), true);
    }

    private static final Term[] digits = new Term[10];

    /** gets the atomic term of an integer, with specific radix (up to 36) */
    @NotNull
    public static Atom the(int i, int radix) {
        //fast lookup for single digits
        if ((i >= 0) && (i <= 9)) {
            Term a = digits[i];
            if (a == null)
                a = digits[i] = the(Integer.toString(i, radix));
            return (Atom) a;
        }
        //return Atom.the(Utf8.toUtf8(name));

        return the(Integer.toString(i, radix));

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

    @NotNull
    public static Atom the(float v) {
        if (Util.equals( (float)Math.floor(v), v, Float.MIN_VALUE*2 )) {
            //close enough to be an int, so it doesnt need to be quoted
            return the((int)v);
        }
        //return Atom.the(Utf8.toUtf8(name));

        return quote(Float.toString(v));

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

    @NotNull
    public static Atom the(@NotNull String name, boolean quoteIfNecessary) {
        if (quoteIfNecessary && Atom.quoteNecessary(name))
            return quote(name);

        //return Atom.the(Utf8.toUtf8(name));

        return the(name);

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

    @Nullable
    public static Term inhImageExt(@NotNull Compound operation, @Nullable Term y, @NotNull Compound x) {
        return inh(
                y,
                imageExt(x, operation.term(1), x.size() - 1 /* position of the variable */)
        );
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the logic rules.
     *
     * @param product  The product
     * @param relation The relation (the operator)
     * @param index    The index of the place-holder (variable)
     * @return A compound generated or a term it reduced to
     */
    @Nullable
    public static Term imageExt(@NotNull Compound product, @NotNull Term relation, int index) {
        int pl = product.size();
//        if (relation.op(PRODUCT)) {
//            Compound p2 = (Compound) relation;
//            if ((pl == 2) && (p2.size() == 2)) {
//                if ((index == 0) && product.term(1).equals(p2.term(1))) { // (/,_,(*,a,b),b) is reduced to a
//                    return p2.term(0);
//                }
//                if ((index == 1) && product.term(0).equals(p2.term(0))) { // (/,(*,a,b),a,_) is reduced to b
//                    return p2.term(1);
//                }
//            }
//        }
        /*Term[] argument =
            builder.concat(new Term[] { relation }, product.cloneTerms()
        );*/
        Term[] argument = new Term[pl];
        argument[0] = relation;
        System.arraycopy(product.terms(), 0, argument, 1, pl - 1);

        return the(IMAGE_EXT, index + 1, argument);
    }

    @Nullable
    public static Compound image(int relation, Term... elements) {
        return image(relation, true, elements);
    }
    @Nullable
    public static Compound imageInt(int relation, Term... elements) {
        return image(relation, false, elements);
    }

    @Nullable
    public static Compound image(int relation, boolean ext, @NotNull Term... elements) {
        Term[] elementsMasked = ArrayUtils.remove(elements, relation);
        Term related = elements[relation];
        Term img = the(ext ? IMAGE_EXT : IMAGE_INT, relation, elementsMasked);

        return (Compound) ( ext ? $.inh(related, img) : $.inh(img, related));
    }
    @Nullable
    public static Compound image(int relation, @NotNull Compound product) {
        assert(product.op() == Op.PRODUCT);
        return image(relation, true, product.terms());
    }
    @Nullable
    public static Compound imageInt(int relation, @NotNull Compound product) {
        assert(product.op() == Op.PRODUCT);
        return image(relation, false, product.terms());
    }

    //TODO add this to a '$.printree' command

    /*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//    /**
//     * Node pretty printer for debugging purposes.
//     *
//     * @author Christoph Beck
//     */
//    public static class NodePrinter {
//        private static boolean isLastSibling(Node node, Node parent) {
//            if (parent != null) {
//                return node == parent.getChild(parent.getCardinality() - 1);
//            }
//            return true;
//        }
//
//        private static void dump(PrintWriter writer, Node node, Stack<Node> predecessors) {
//            if (!predecessors.isEmpty()) {
//                Node parent = null;
//                for (Node predecessor: predecessors) {
//                    if (isLastSibling(predecessor, parent)) {
//                        writer.print("   ");
//                    } else {
//                        writer.print("|  ");
//                    }
//                    parent = predecessor;
//                }
//                writer.println("|");
//            }
//            Node parent = null;
//            for (Node predecessor: predecessors) {
//                if (isLastSibling(predecessor, parent)) {
//                    writer.print("   ");
//                } else {
//                    writer.print("|  ");
//                }
//                parent = predecessor;
//            }
//            writer.print("+- ");
//            writer.println(node.toString());
//
//            predecessors.push(node);
//            for (int i = 0; i < node.getCardinality(); i++) {
//                dump(writer, node.getChild(i), predecessors);
//            }
//            predecessors.pop();
//        }
//
//        public static void dump(PrintWriter writer, Node node) {
//            dump(writer, node, new Stack<Node>());
//        }
//    }
}
