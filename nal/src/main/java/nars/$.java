package nars;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.concept.util.ConceptBuilder;
import nars.index.term.TermIndex;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.obj.IntTerm;
import nars.term.var.AbstractVariable;
import nars.term.var.GenericVariable;
import nars.term.var.VarPattern;
import nars.term.var.Variable;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.list.FasterList;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;

/***
 *     oooo       oo       .o.       ooooooooo.
 *    `888b.      8'      .888.      `888   `Y88.
 *     88`88b.    88     .8"888.      888   .d88'  .ooooo oo oooo  oooo   .ooooo.  oooo d8b oooo    ooo
 *     88  `88b.  88    .8' `888.     888ooo88P'  d88' `888  `888  `888  d88' `88b `888""8P  `88.  .8'
 *     88    `88b.88   .88ooo8888.    888`88b.    888   888   888   888  888ooo888  888       `88..8'
 *     88      `8888  .8'     `888.   888  `88b.  888   888   888   888  888    .o  888        `888'
 *     8o        `88 o88o     o8888o o888o  o888o `V8bod888   `V88V"V8P' `Y8bod8P' d888b        .8'
 *                                                      888.                                .o..P'
 *                                                      8P'                                 `Y8P'
 *                                                      "
 *
 *                                              NARquery
 *                                          Core Utility Class
 */
public enum $ {
    ;

    public static final org.slf4j.Logger logger = LoggerFactory.getLogger($.class);
    public static final Function<Object, Term> ToStringToTerm = (x) -> $.the(x.toString());

    @NotNull
    public static <T extends Term> T $(@NotNull String term) throws Narsese.NarseseException {
        return terms.parse(term);
    }




//    public static @NotNull <O> ObjRef<O> ref(String term, O instance) {
//        return new ObjRef(term, instance);
//    }

    @NotNull
    public static Atomic the(@NotNull String id) {
        return new Atom(id);
    }

    @NotNull
    public static Atom quote(Object text) {
        return (Atom)$.the("\"" + text + '"');
    }


    /** warning: this will modify the StringBuilder parameter in generating the quote, to avoid duplicating the instance */
    @NotNull public static Atom quote(StringBuilder getsModified) {
        return (Atom)$.the(getsModified.insert(0, '"').append('"'));
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
    public static IntTerm the(int i) {
        //return the(i, 10);
        return new IntTerm(i);
    }

    @NotNull
    public static Atom the(char c) {
        return (Atom) the(String.valueOf(c));
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     *  returns a Term if the two inputs are equal to each other
     */
    @Nullable
    public static Compound inh(Term subj, Term pred) {

//        if ((predicate instanceof Operator) && if (subject instanceof Product))
//            return new GenericCompound(Op.INHERITANCE, (Operator)predicate, (Product)subject);
//        else
        return compoundOrNull(compound(INH, subj, pred));
    }


    @Nullable
    public static Compound inh(@NotNull String subj, @NotNull String pred) {
        return inh((Term)$(subj), (Term)$(pred));
    }


    @Nullable
    public static Term sim(@NotNull Term subj, @NotNull Term pred) {
        return compound(SIM, subj, pred);
    }


    public static Compound func(@NotNull String opTerm, @Nullable Term... arg) {
        return func($.the(opTerm), arg);
    }

    /** function ((a,b)==>c) aka: c(a,b) */
    @NotNull
    public static Compound func(@NotNull Atomic opTerm, @Nullable Term... arg) {
        return (Compound) compound(
                INH,
                arg == null ? Terms.ZeroProduct : $.p(arg),
                opTerm
        );
    }

    @NotNull public static Compound func(@NotNull Atomic opTerm, @Nullable Collection<Term> arg) {
        return (Compound) compound(
                INH,
                arg == null ? Terms.ZeroProduct : $.p(arg),
                opTerm
        );
    }


    @NotNull
    public static Term impl(@NotNull Term a, @NotNull Term b) {
        return compound(IMPL, a, b);
    }

    @NotNull
    public static Term impl(@NotNull Term a, int dt, @NotNull Term b) {
        return compound(IMPL, dt, a, b);
    }

    @NotNull
    public static Term neg(@NotNull Term x) {
        return terms.neg(x);
    }

    @Nullable public static Term negIf(@NotNull Term x, boolean negate) {
        return negate ? neg(x) : x;
    }

    @NotNull
    public static Compound p(@NotNull Collection<? super Term> t) {
        return $.p(t.toArray(new Term[t.size()]));
    }

    @NotNull
    public static Compound p(@NotNull Term... t) {
        return (t.length == 0) ? Terms.ZeroProduct : (Compound) compound(PROD, t);
    }
    @NotNull
    public static Compound p(@NotNull TermContainer t) {
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
    public static Compound p(@NotNull String... t) {
        return $.p((Term[]) $.the(t));
    }
    @NotNull
    public static Compound p(@NotNull int... t) {
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
    public static Variable varQuery(int i) {
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
     *  A -{- B becomes {A} --> B
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    @Nullable
    public static Term inst(@NotNull Term subj, Term pred) {
        return terms.inst(subj, pred);
    }
    @Nullable
    public static Term instprop(@NotNull Term subject, @NotNull Term predicate) {
        return terms.instprop(subject, predicate);
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
        return task(term, Symbols.BELIEF, freq, conf);
    }

    @NotNull
    public static MutableTask goal(@NotNull Compound term, float freq, float conf) {
        return task(term, Symbols.GOAL, freq, conf);
    }

    @NotNull
    public static MutableTask task(@NotNull String term, char punct, float freq, float conf) throws Narsese.NarseseException {
        return task((Compound)$.$(term), punct, freq, conf);
    }

    @NotNull
    public static MutableTask task(@NotNull Compound term, char punct, float freq, float conf) {
        return task(term, punct, t(freq, conf));
    }
    @NotNull
    public static MutableTask task(@NotNull Compound term, char punct, Truth truth) {
        return new MutableTask(term, punct, truth);
    }

    @NotNull
    public static Compound sete(@NotNull Collection<? extends Term> t) {
        return (Compound) compound(SETe, (Collection)t);
    }

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
    public static Compound p(@NotNull char[] c, @NotNull CharToObjectFunction<Term> f) {
        Term[] x = new Term[c.length];
        for (int i = 0; i < c.length; i++) {
            x[i] = f.valueOf(c[i]);
        }
        return $.p(x);
    }

    @NotNull
    public static <X> Compound p(@NotNull X[] x, @NotNull Function<X, Term> toTerm) {
        return $.p( (Term[])terms(x, toTerm) );
    }

    public static <X> Term[] terms(@NotNull X[] map, @NotNull Function<X, Term> toTerm) {
        return Stream.of(map).map(e -> toTerm.apply(e)).toArray(n -> new Term[n]);
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
        return (Compound) compound(SETe, t);

    }

    /** shorthand for extensional set */
    @NotNull
    public static Compound s(Term... t) {
        return sete(t);
    }

    @NotNull
    public static Compound seti(Term... t) {
        return (Compound) compound(SETi, t);
    }

//    /**
//     * Try to make a new compound from two components. Called by the logic rules.
//     * <p>
//     *  A -]- B becomes A --> [B]
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
        return v(AbstractVariable.typeIndex(ch), name);
    }

    /** normalized variable */
    public static @NotNull AbstractVariable v(@NotNull Op type, int counter) {
        return AbstractVariable.cached(type, counter);
    }

    @Nullable
    public static Term conj(Term... a) {
        return compound(CONJ, a);
    }
    @Nullable
    public static Term conj(@NotNull Collection<Term> collection, @NotNull Term... append) {
        if (append.length == 0)
            throw new RuntimeException("unnecessary append");
        int cs = collection.size();
        Term[] ca = new Term[cs + append.length];
        collection.toArray(ca);
        int i = cs;
        for (Term t : append) {
            ca[i++] = t;
        }
        return compound(CONJ, ca);
    }




    /** parallel conjunction &| aka &&+0 */
    @Nullable
    public static Term parallel(Term... s) {
        return compound(CONJ, 0, s);
    }
    @Nullable
    public static Term parallel(@NotNull Collection<Term> s) {
        return compound(CONJ, 0, s);
    }

    @Nullable
    public static Term disj(@NotNull Term... a) {
        return terms.disjunction(a);
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
    public static final Logger logRoot;

    /** NALogging non-axiomatic logging encoder. log events expressed in NAL terms */
    @NotNull
    public static final PatternLayoutEncoder logEncoder;

    static {
        Thread.currentThread().setName("$");

        //http://logback.qos.ch/manual/layouts.html

        logRoot = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logEncoder = new PatternLayoutEncoder();

        try {

            LoggerContext loggerContext = logRoot.getLoggerContext();
            // we are not interested in auto-configuration
            loggerContext.reset();

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

        } catch (Throwable t) {
            System.err.println("Logging Disabled: " + t);
        }
    }


    @Nullable
    public static Term equi(Term subject, Term pred) {
        return compound(EQUI, subject, pred);
    }
    @Nullable
    public static Term equi(Term subject, int dt, Term pred) {
        return compound(EQUI, dt, subject, pred);
    }

    @Nullable
    public static Term diffi(Term a, Term b) {
        return compound(DIFFi, a, b);
    }

    @Nullable
    public static Term diffe(Term a, Term b) {
        return compound(DIFFe, a, b);
    }

    @Nullable
    public static Term imge(Term... x) {
        return compound(IMGe, x);
    }
    @Nullable
    public static Term imgi(Term... x) {
        return compound(IMGi, x);
    }

    @Nullable
    public static Term secte(Term... x) {
        return compound(SECTe, x);
    }



    @Nullable
    public static Term secti(Term... x) { return compound(SECTi, x); }




    @NotNull
    public static Term compound(@NotNull Op op, Term... subterms) {
        return compound(op, DTERNAL, subterms);
    }

    @NotNull
    public static Term compound(@NotNull Op op, int dt, @NotNull Term... subterms) {
        return terms.the(op, dt, subterms);
    }

    @Nullable
    public static Term compound(@NotNull Op op, @NotNull Collection<Term> subterms) {
        return compound(op, DTERNAL, subterms);
    }
    @Nullable
    public static Term compound(@NotNull Op op, int dt, @NotNull Collection<Term> subterms) {
        return compound(op, dt, subterms.toArray(new Term[subterms.size()]));
    }



    /** create a literal atom from a class (it's name) */
    @NotNull
    public static Atom the(@NotNull Class c) {
        return (Atom) $.the(c.getName());
    }


    @NotNull
    public static Atomic the(Number o) {

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

        return (Atom) the(Integer.toString(i, radix));

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
    public static Atomic the(float v) {
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
    public static Atomic the(@NotNull String name, boolean quoteIfNecessary) {
        if (quoteIfNecessary && quoteNecessary(name))
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
    public static Term inhImageExt(@NotNull Compound x, @Nullable Term y, @NotNull Atomic oper) {
        Term[] args = x.terms();
        Term[] aa = ArrayUtils.add(args, 0, oper);
        return inh(y, imageMask( aa.length-1, true, aa));
        /*return inh(
                y,
                imge(x, operation.term(1)  )
        );*/
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
    public static Term imge(@NotNull Compound product, @NotNull Term relation) {
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

        return image(0, true, argument);
        //return the(IMGe, argument);
    }

    @Nullable
    public static Compound image(int relation, Term... elements) {
        return image(relation, true, elements);
    }
    @Nullable
    public static Compound imgi(int relation, Term... elements) {
        return image(relation, false, elements);
    }

    @Nullable
    public static Compound image(int relation, boolean ext, @NotNull Term... elements) {
        Term img = imageMask(relation, ext, elements);

        Term related = elements[relation];
        return ext ? $.inh(related, img) : $.inh(img, related);
    }

    @Nullable
    public static Term imageMask(int relation, boolean ext, @NotNull Term[] elements) {
        Term[] elementsMasked = ArrayUtils.remove(elements, relation);
        return compound(ext ? IMGe : IMGi, relation, elementsMasked);
    }

    @Nullable
    public static Compound imge(int relation, @NotNull Compound product) {
        assert(product.op() == Op.PROD);
        return image(relation, true, product.terms());
    }
    @Nullable
    public static Compound imgi(int relation, @NotNull Compound product) {
        assert(product.op() == Op.PROD);
        return image(relation, false, product.terms());
    }

    @Nullable
    public static Truth t(float f, float c) {
        return t(f, c, Param.TRUTH_EPSILON);
    }

    @Nullable
    public static Truth t(float f, float c, float minConf) {
        return c < minConf ? null : new DefaultTruth(f, c);
    }

    public static Budget b(float p, float q) {
        return new RawBudget(p, q);
    }

    /** negates each entry in the array */
    public static void neg(@NotNull Term[] s) {
        int l = s.length;
        for (int i = 0; i < l; i++) {
            s[i] = $.neg(s[i]);
        }
    }


    /** static storeless term builder */
    public static final StaticTermBuilder terms = new StaticTermBuilder();

    /** determines if the string is invalid as an unquoted term according to the characters present */
    public static boolean quoteNecessary(@NotNull CharSequence t) {
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
//            if (Character.isWhitespace(c)) return true;
            if (!Narsese.isValidAtomChar(c))
                return true;
//            if ((!Character.isDigit(c)) && (!Character.isAlphabetic(c))) return true;
        }
        return false;
    }

    @NotNull
    public static Atomic the(@NotNull byte[] id) {
        return the(new String(id));
    }

    @NotNull
    public static Atomic the(byte c) {
        return the(new byte[] { c });
    }

    @NotNull
    public static Term[] the(@NotNull int... i) {
        int l = i.length;
        Term[] x = new Term[l];
        for (int j = 0; j < l; j++) {
            x[j] = new IntTerm(i[j]);
        }
        return x;
    }

    @Nullable
    public static Term the(Object o) {

        if (o instanceof Term) return ((Term)o);
        if (o instanceof String)
            return the((String)o);
        if (o instanceof StringBuilder)
            return the(o.toString());
        if (o instanceof Number)
            return the((Number)o);

        return null;
    }

    /** conjunction sequence (2-ary) */
    @Nullable public static Term seq(Term x, int dt, Term y) {
        return compound(CONJ, dt, x, y); //must be a vector, not set
    }


    @NotNull
    public static <K,V> Map<K, V> newHashMap() {
        return newHashMap(0);
    }

    @NotNull
    public static <K, V> Map<K,V> newHashMap(int capacity) {
        return new HashMap<>(capacity);

        //return new UnifiedMap(capacity);
        //return new UnifriedMap(capacity /*, loadFactor */);

        //return new FasterHashMap(capacity);
        //return new FastMap<>(); //javolution http://javolution.org/apidocs/javolution/util/FastMap.html

        //return new LinkedHashMap(capacity);
    }

    public static @NotNull <X> List<X> newArrayList() {
        return new FasterList<>(); //GS
        //return new ArrayList();
    }

    @NotNull
    public static <X> List<X> newArrayList(int capacity) {
        return new FasterList(capacity);
        //return new ArrayList(capacity);
    }

    public static @NotNull <X> Set<X> newHashSet(int capacity) {
        if (capacity < 4) {
            return new UnifiedSet(0);
        } else {
            //return new UnifiedSet(capacity);
            //return new SimpleHashSet(capacity);
            return new HashSet(capacity);
            //return new LinkedHashSet(capacity);
        }
    }

    @NotNull
    public static <X> Set<X> newHashSet(@NotNull Collection<X> values) {
        Set<X> s = newHashSet(values.size());
        s.addAll(values);
        return s;
    }

    public static @Nullable <C> Reference<C> reference(@Nullable C s) {
        return s == null ? null :
                //new SoftReference<>(s);
                //new WeakReference<>(s);
                Param.DEBUG ? new SoftReference<>(s) : new WeakReference<>(s);
    }

    @Nullable
    public static <C> Reference<C>[] reference(@Nullable C[] s) {
        int l = Util.lastNonNull((Object[]) s);
        if (l > -1) {
            l++;
            Reference<C>[] rr = new Reference[l];
            for (int i = 0; i < l; i++) {
                rr[i] = reference(s[i]);
            }
            return rr;
        }
        return null;
    }

    public static void dereference(@NotNull Reference[] p) {
        for (int i = 0; i < p.length; i++) {
            Reference x = p[i];
            if (x != null)
                x.clear();
            p[i] = null;
        }
    }

    @Nullable
    public static <C> C dereference(@Nullable Reference<C> s) {
        return s == null ? null : s.get();
    }

    @Nullable
    public static <C> C dereference(@Nullable Reference<C>[] s, int index) {
        if (s == null || index >= s.length) return null;
        return dereference(s[index]);
    }


    @NotNull
    public static <X> List<X> newArrayList(@NotNull X... x) {
        FasterList<X> l = (FasterList)$.newArrayList(x.length);
        l.addAll(x);
        return l;
    }

    public static Compound pRadix(int x, int radix, int maxX) {
        Term[] tt = radixArray(x, radix, maxX);
        return $.p(tt);
    }


    /** most significant digit first, least last. padded with zeros */
    public static @NotNull Term[] radixArray(int x, int radix, int maxX) {
        String xs = Integer.toString(x, radix);
        String xx = Integer.toString(maxX, radix);
        Term[] tt = new Term[xx.length()];
        int ttl = tt.length;
        int xsl = xs.length();
        int p = ttl - xsl;
        int j = 0;
        for (int i = 0; i < ttl; i++) {
            Term n;
            if (p-- > 0) {
                n = $.the(0); //pad with zeros
            } else {
                n = $.the(xs.charAt(j++) - '0');
            }
            tt[i] = n;
        }
        return tt;
    }

    /** generates a recursive product for the given terms array. the terms should be ordered with the
     * most significant digit first. */
    public static @NotNull Compound pRecurse(@NotNull Term... t) {
        int tl = t.length;
        Compound nextInner = $.p(t[tl - 1]); //wrap innermost item in product too, for fairness
        for (int i = tl - 2; i >= 0; i--) {
            nextInner = $.p(t[i], nextInner);
        }
        return nextInner;
    }

    public static void logLevel(Class logClass, Level l) {
        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(logClass)).setLevel(l);
    }

    @NotNull
    public static Task command(@NotNull Compound op) {
        //TODO use lightweight CommandTask impl without all the logic metadata
        MutableTask t = new MutableTask(op, Symbols.COMMAND, null);
        t.setBudget(1f, 1f);
        return t;
    }

    @NotNull
    public static Task command(@NotNull String functor, Term... args) {
        //TODO use lightweight CommandTask impl without all the logic metadata
        return command(func(functor, args));
    }

    @NotNull
    public static String unquote(@NotNull Term s) {
        String x = s.toString();
        if (s instanceof Atom) {
            int len = x.length();
            if (len > 0 && x.charAt(0) == '\"' && x.charAt(len - 1) == '\"') {
                return x.substring(1, len - 1);
            }
        }
        return x;
    }

    /** global shared Javascript context */
    public final static NashornScriptEngine JS = JS();

    /** instantiate new Javascript context */
    public final static NashornScriptEngine JS() {
        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    }

    public static final class StaticTermBuilder extends TermIndex {

        @Override
        public
        @Nullable
        Termed get(@NotNull Term t, boolean createIfMissing) {
            return createIfMissing ? t : null;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public @NotNull String summary() {
            return "";
        }

        @Override
        public void remove(@NotNull Term entry) {

        }


        @Override
        public void clear() {

        }

        @Override
        public void forEach(Consumer<? super Termed> c) {

        }


        @Nullable
        @Override
        public void set(@NotNull Term s, Termed t) {
            throw new UnsupportedOperationException();
        }


        @Nullable
        @Override
        public ConceptBuilder conceptBuilder() {
            return null;
        }

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
