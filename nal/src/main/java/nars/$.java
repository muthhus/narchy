package nars;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import com.google.common.base.Strings;
import com.google.common.escape.Escapers;
import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import nars.derive.LambdaPred;
import nars.derive.PrediTerm;
import nars.index.term.StaticTermIndex;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import nars.term.obj.JsonTerm;
import nars.term.var.AbstractVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.VarPattern;
import nars.term.var.Variable;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.*;
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
public interface $ {


//    public static final org.slf4j.Logger logger = LoggerFactory.getLogger($.class);
//    public static final Function<Object, Term> ToStringToTerm = (x) -> Atomic.the(x.toString());

    @NotNull
    static <T extends Term> T $(@NotNull String term) throws Narsese.NarseseException {
        return (T) Narsese.term(term, true);
    }

    static <T extends Term> T $safe(@NotNull String term) {
        try {
            return $(term);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

//    public static @NotNull <O> ObjRef<O> ref(String term, O instance) {
//        return new ObjRef(term, instance);
//    }

    Atom emptyQuote = (Atom) Atomic.the("\"\"");

    Escapers.Builder quoteEscaper = Escapers.builder().addEscape('\"', "\\\"");


    @NotNull
    static Atomic quote(@NotNull Object text) {
        String s = text.toString();

        int length = s.length();

        if (length == 0)
            return emptyQuote;

        if (s.charAt(0) == '\"' && s.charAt(length - 1) == '\"') {
            if (length == 1) {
                s = "\"\\\"\"";
            } else {
                //already quoted the empty string
            }
        } else {
            s = ("\"" + quoteEscaper.build().escape(s) + '"');
        }

        return Atomic.the(s);
    }


    @NotNull
    static Term[] the(@NotNull String... id) {
        int l = id.length;
        Term[] x = new Term[l];
        for (int i = 0; i < l; i++)
            x[i] = Atomic.the(id[i]);
        return x;
    }


    @NotNull
    static Atom the(char c) {
        return (Atom) Atomic.the(String.valueOf(c));
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     * returns a Term if the two inputs are equal to each other
     */
    @Nullable
    static <T extends Term> T inh(Term subj, Term pred) {
        return (T) INH.the(DTERNAL, subj, pred);
    }

    static <T extends Term> T inh(Term subj, String pred) {
        return $.inh(subj, $.the(pred));
    }


    @Nullable
    static <T extends Term> T inh(@NotNull String subj, @NotNull String pred) throws Narsese.NarseseException {
        return (T) inh($(subj), $(pred));
    }


    @NotNull
    static <T extends Term> T sim(@NotNull Term subj, @NotNull Term pred) {
        return (T) SIM.the(DTERNAL, subj, pred);
    }


    static @NotNull Term func(@NotNull String opTerm, @NotNull Term... arg) {
        return func(Atomic.the(opTerm), arg);
    }


    static @NotNull Term func(@NotNull String opTerm, @NotNull String... arg) throws Narsese.NarseseException {
        return func(Atomic.the(opTerm), $.array(arg));
    }

    /**
     * function ((a,b)==>c) aka: c(a,b)
     */
    @NotNull
    static Term func(@NotNull Atomic opTerm, @NotNull Term... arg) {
        return INH.the($.p(arg), opTerm);
    }

    @NotNull
    static Term func(@NotNull Atomic opTerm, @NotNull Collection<Term> arg) {
        return INH.the($.p(arg), opTerm);
    }


    @NotNull
    static <T extends Term> T impl(@NotNull Term a, @NotNull Term b) {
        return (T) IMPL.the(DTERNAL, a, b);
    }

    @NotNull
    static <T extends Term> T impl(@NotNull Term a, int dt, @NotNull Term b) {
        return (T) IMPL.the(dt, a, b);
    }

    @NotNull
    static Term p(@NotNull Collection<? super Term> t) {
        return $.p(t.toArray(new Term[t.size()]));
    }

    @NotNull
    static Term p(@NotNull Term... t) {
        return (t.length == 0) ? ZeroProduct : PROD.the(DTERNAL, t);
    }

    @NotNull
    static Term p(@NotNull TermContainer t) {
        return p((Term[]) t.toArray());
    }

    /**
     * creates from a sublist of a list
     */
    @NotNull
    static Term p(@NotNull List<Term> l, int from, int to) {
        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return $.p(x);
    }

    @NotNull
    static Term p(@NotNull String... t) {
        return $.p((Term[]) $.the(t));
    }

    @NotNull
    static Term p(@NotNull int... t) {
        return $.p((Term[]) $.the(t));
    }

    /**
     * warning: generic variable
     */
    static @NotNull Variable v(@NotNull Op type, @NotNull String name) {

//        if (name.length()==1) {
//            char c = name.charAt(0);
//            if (c >= '1' && c <= '9')
//                return $.v(type, c-'0'); //explicit use of normalized var
//        }

        return new UnnormalizedVariable(type, type.ch + name);
    }


    @NotNull
    @Deprecated
    static Variable varDep(int i) {
        return v(VAR_DEP, i);
    }

    static @NotNull Variable varDep(@NotNull String s) {
        return v(VAR_DEP, s);
    }

    @NotNull
    @Deprecated
    static Variable varIndep(int i) {
        return v(VAR_INDEP, i);
    }

    static @NotNull Variable varIndep(@NotNull String s) {
        return v(VAR_INDEP, s);
    }

    @NotNull
    static Variable varQuery(int i) {
        return v(VAR_QUERY, i);
    }

    static @NotNull Variable varQuery(@NotNull String s) {
        return v(VAR_QUERY, s);
    }

    @NotNull
    static VarPattern varPattern(int i) {
        return (VarPattern) v(VAR_PATTERN, i);
    }


    /**
     * Try to make a new compound from two components. Called by the logic rules.
     * <p>
     * A -{- B becomes {A} --> B
     *
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    @Nullable
    static <T extends Term> T inst(@NotNull Term subj, Term pred) {
        return (T) INH.the(SETe.the(subj), pred);
    }

    @Nullable
    static <T extends Term> T instprop(@NotNull Term subject, @NotNull Term predicate) {
        return (T) INH.the(SETe.the(subject), SETi.the(predicate));
    }

    @Nullable
    static <T extends Term> T prop(Term subject, Term predicate) {
        return (T) INH.the(subject, SETi.the(predicate));
    }

//    public static Term term(final Op op, final Term... args) {
//        return builder.term(op, args);
//    }

    @NotNull
    static TaskBuilder belief(@NotNull Term term, @NotNull Truth copyFrom) {
        return belief(term, copyFrom.freq(), copyFrom.conf());
    }

    @NotNull
    static TaskBuilder belief(@NotNull Term term, float freq, float conf) {
        return task(term, BELIEF, freq, conf);
    }

    @NotNull
    static TaskBuilder goal(@NotNull Term term, float freq, float conf) {
        return task(term, GOAL, freq, conf);
    }

    @NotNull
    static TaskBuilder task(@NotNull String term, byte punct, float freq, float conf) throws Narsese.NarseseException {
        return task($.$(term), punct, freq, conf);
    }

    @NotNull
    static TaskBuilder task(@NotNull Term term, byte punct, float freq, float conf) {
        return task(term, punct, t(freq, conf));
    }

    @NotNull
    static TaskBuilder task(@NotNull Term term, byte punct, Truth truth) {
        return new TaskBuilder(term, punct, truth);
    }

    @NotNull
    static Term sete(@NotNull Collection<? extends Term> t) {
        return SETe.the(DTERNAL, (Collection) t);
    }

    /**
     * construct set_ext of key,value pairs from a Map
     */
    @NotNull
    static Term seteMap(@NotNull Map<Term, Term> map) {
        return $.sete(
                map.entrySet().stream().map(
                        e -> $.p(e.getKey(), e.getValue()))
                        .collect(Collectors.toSet())
        );
    }

    @NotNull
    static Term p(@NotNull char[] c, @NotNull CharToObjectFunction<Term> f) {
        Term[] x = new Term[c.length];
        for (int i = 0; i < c.length; i++) {
            x[i] = f.valueOf(c[i]);
        }
        return $.p(x);
    }

    @NotNull
    static <X> Term p(@NotNull X[] x, @NotNull Function<X, Term> toTerm) {
        return $.p((Term[]) terms(x, toTerm));
    }

    static <X> Term[] terms(@NotNull X[] map, @NotNull Function<X, Term> toTerm) {
        return Stream.of(map).map(toTerm::apply).toArray(Term[]::new);
    }

    @NotNull
    static <X> Term seteMap(@NotNull Map<Term, ? extends X> map, @NotNull Function<X, Term> toTerm) {
        return $.sete(
                map.entrySet().stream().map(
                        e -> $.p(e.getKey(), toTerm.apply(e.getValue())))
                        .collect(Collectors.toSet())
        );
    }

    private static Term[] array(@NotNull Collection<? extends Term> t) {
        return t.toArray(new Term[t.size()]);
    }

    private static Term[] array(String... s) throws Narsese.NarseseException {
        int l = s.length;
        Term[] tt = new Term[l];
        for (int i = 0; i < l; i++)
            tt[i] = $.$(s[i]);

        return tt;
    }

    @NotNull
    static Term seti(@NotNull Collection<Term> t) {
        return $.seti(array(t));
    }

    @NotNull
    static Term sete(Term... t) {
        return SETe.the(DTERNAL, t);

    }

    /**
     * shorthand for extensional set
     */
    @NotNull
    static Term s(Term... t) {
        return sete(t);
    }

    @NotNull
    static Term seti(Term... t) {
        return SETi.the(DTERNAL, t);
    }


    /**
     * unnormalized variable
     */
    static @NotNull Variable v(char ch, @NotNull String name) {
        return v(AbstractVariable.typeIndex(ch), name);
    }

    /**
     * normalized variable
     */
    static @NotNull AbstractVariable v(@NotNull Op type, int id) {
        return AbstractVariable.the(type, id);
    }

    @Nullable
    static <T extends Term> T conj(Term... a) {
        return (T) CONJ.the(DTERNAL, a);
    }

    @Nullable
    static Term conj(@NotNull Collection<Term> collection, @NotNull Term... append) {
        if (append.length == 0)
            throw new RuntimeException("unnecessary append");
        int cs = collection.size();
        Term[] ca = new Term[cs + append.length];
        collection.toArray(ca);
        int i = cs;
        for (Term t : append) {
            ca[i++] = t;
        }
        return CONJ.the(DTERNAL, ca);
    }


    /**
     * parallel conjunction &| aka &&+0
     */
    @NotNull
    static Term parallel(Term... s) {
        return CONJ.the(0, s);
    }

    @NotNull
    static Term parallel(@NotNull Collection<Term> s) {
        return CONJ.the(0, s);
    }

    @NotNull
    static Term disj(@NotNull Term... a) {
        return DISJ.the(a);
    }

    class Logging {
        static {
            Thread.currentThread().setName("$");

            //http://logback.qos.ch/manual/layouts.html

            Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            LoggerContext loggerContext = LOG.getLoggerContext();
            // we are not interested in auto-configuration
            loggerContext.reset();

            PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
            logEncoder.setContext(loggerContext);
            //logEncoder.setPattern("\\( %highlight(%level),%green(%thread),%yellow(%logger{0}) \\): \"%message\".%n");
            logEncoder.setPattern("\\( %green(%thread),%highlight(%logger{0}) \\): \"%message\".%n");

            logEncoder.start();


            ConsoleAppender c = new ConsoleAppender();
            c.setContext(loggerContext);
            c.setEncoder(logEncoder);
            c.setImmediateFlush(false);
            //c.setWithJansi(true);
            c.start();

            LOG.addAppender(c);

//            SyslogAppender syslog = new SyslogAppender();
//            syslog.setPort(5000);
//            syslog.setFacility("LOCAL6");
//            syslog.setContext(loggerContext);
//            syslog.setCharset(Charset.forName("UTF8"));
//            syslog.start();
//            LOG.addAppender(syslog);

//            SocketAppender sa = new SocketAppender();
//            sa.setName("socketlog");
//            sa.setContext(loggerContext);
//            sa.setQueueSize(1);
//            sa.setEventDelayLimit(Duration.buildByMilliseconds(100));
//            sa.setRemoteHost("localhost");
//            sa.setPort(4560);
//            sa.setIncludeCallerData(true);
//            sa.setReconnectionDelay(Duration.buildByMilliseconds(200));
//            sa.start();
//            LOG.addAppender(sa);

//        logRoot.debug("Message 1");
//        logRoot.info("Message 1");
//        logRoot.warn("Message 2");
//        logRoot.error("Message 2");


//        } catch (Throwable t) {
//            System.err.println("Logging Disabled: " + t);
//        }
        }

    }


    @Nullable
    static Term diffi(Term a, Term b) {
        return DIFFi.the(DTERNAL, a, b);
    }

    @Nullable
    static Term diffe(Term a, Term b) {
        return DIFFe.the(DTERNAL, a, b);
    }


    @Nullable
    static Term secte(Term... x) {
        return SECTe.the(DTERNAL, x);
    }


    @Nullable
    static Term secti(Term... x) {
        return SECTi.the(DTERNAL, x);
    }


    /**
     * create a literal atom from a class (it's name)
     */
    @NotNull
    static Atom the(@NotNull Class c) {
        return (Atom) Atomic.the(c.getName());
    }


    /**
     * gets the atomic term of an integer, with specific radix (up to 36)
     */
    @NotNull
    static Atom the(int i, int radix) {
//        //fast lookup for single digits
//        if ((i >= 0) && (i <= 9)) {
//            Term a = digits[i];
//            if (a == null)
//                a = digits[i] = the(Integer.toString(i, radix));
//            return (Atom) a;
//        }
//        //return Atom.the(Utf8.toUtf8(name));

        return (Atom) Atomic.the(Integer.toString(i, radix));

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
    static Atomic the(int v) {
        return Int.the(v);
    }

    @NotNull
    static Atomic the(float v) {
        if (Util.equals((float) Math.floor(v), v, Float.MIN_VALUE * 2)) {
            //close enough to be an int, so it doesnt need to be quoted
            return the((int) v);
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


    @Nullable
    static Truth t(float f, float c) {
        return t(f, c, 0);
    }

    @Nullable
    static Truth t(float f, float c, float minConf) {
        return new PreciseTruth(f, c);
    }

    static Prioritized b(float p) {
        return new Pri(p);
    }

    /**
     * negates each entry in the array
     */
    static void neg(@NotNull Term[] array) {
        Util.map(Term::neg, array, array);
//        int l = array.length;
//        for (int i = 0; i < l; i++) {
//            array[i] = $.neg(array[i]);
//        }
    }


    /**
     * static storeless term builder
     */
    StaticTermIndex terms = new StaticTermIndex();

    @NotNull
    static Atomic the(@NotNull byte[] id) {
        return Atomic.the(new String(id));
    }

    @NotNull
    static Atomic the(byte c) {
        return the(new byte[]{c});
    }

    @NotNull
    static Term[] the(@NotNull int... i) {
        int l = i.length;
        Term[] x = new Term[l];
        for (int j = 0; j < l; j++) {
            x[j] = the(i[j]);
        }
        return x;
    }

    static Term the(boolean b) {
        return b ? Op.True : Op.False;
    }

    static Term the(Object o) {
        if (o instanceof Term)
            return ((Term) o);

        if (o instanceof Number) {
            if (o instanceof Integer)
                return Int.the(((Integer) o).intValue());
        }

        return Atomic.the(o.toString());
    }

    /**
     * conjunction sequence (2-ary)
     */
    @Nullable
    static Term seq(Term x, int dt, Term y) {
        return CONJ.the(dt, x, y); //must be a vector, not set
    }


    @NotNull
    static <K, V> Map<K, V> newHashMap() {
        return newHashMap(0);
    }

    @NotNull
    static <K, V> Map<K, V> newHashMap(int capacity) {
        return new HashMap<>(capacity);

        //return new UnifiedMap(capacity);
        //return new UnifriedMap(capacity /*, loadFactor */);

        //return new FasterHashMap(capacity);
        //return new FastMap<>(); //javolution http://javolution.org/apidocs/javolution/util/FastMap.html

        //return new LinkedHashMap(capacity);
    }

    static @NotNull <X> List<X> newArrayList() {
        return new FasterList<>(0);
        //return new ArrayList();
    }

    @NotNull
    static <X> List<X> newArrayList(int capacity) {
        return new FasterList(capacity);
        //return new ArrayList(capacity);
    }

    static @NotNull <X> Set<X> newHashSet(int capacity) {
//        if (capacity < 4) {
//            return new UnifiedSet(0);
//        } else {
        //return new UnifiedSet(capacity);
        //return new SimpleHashSet(capacity);
        return new HashSet(capacity);
        //return new LinkedHashSet(capacity);
//        }
    }

//    @NotNull
//    public static <X> Set<X> newHashSet(@NotNull Collection<X> values) {
//        Set<X> s = newHashSet(values.size());
//        s.addAll(values);
//        return s;
//    }

//    public static @Nullable <C> Reference<C> reference(@Nullable C s) {
//        return s == null ? null :
//                //new SoftReference<>(s);
//                //new WeakReference<>(s);
//                Param.DEBUG ? new SoftReference<>(s) : new WeakReference<>(s);
//    }

//    @Nullable
//    public static <C> Reference<C>[] reference(@Nullable C[] s) {
//        int l = Util.lastNonNull((Object[]) s);
//        if (l > -1) {
//            l++;
//            Reference<C>[] rr = new Reference[l];
//            for (int i = 0; i < l; i++) {
//                rr[i] = reference(s[i]);
//            }
//            return rr;
//        }
//        return null;
//    }

//    public static void dereference(@NotNull Reference[] p) {
//        for (int i = 0; i < p.length; i++) {
//            Reference x = p[i];
//            if (x != null)
//                x.clear();
//            p[i] = null;
//        }
//    }
//
//    @Nullable
//    public static <C> C dereference(@Nullable Reference<C> s) {
//        return s == null ? null : s.get();
//    }
//
//    @Nullable
//    public static <C> C dereference(@Nullable Reference<C>[] s, int index) {
//        if (s == null || index >= s.length) return null;
//        return dereference(s[index]);
//    }


    @NotNull
    static <X> List<X> newArrayList(@NotNull X... x) {
        return new FasterList(x);
//        FasterList<X> l = (FasterList) $.newArrayList(x.length);
//        l.addAll(x);
//        return l;
    }

    static Term pRadix(int x, int radix, int maxX) {
        Term[] tt = radixArray(x, radix, maxX);
        return $.p(tt);
    }


    /**
     * most significant digit first, least last. padded with zeros
     */
    static @NotNull Term[] radixArray(int x, int radix, int maxX) {
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


    static @NotNull Term pRecurseIntersect(char prefix, @NotNull Term... t) {
        final int[] index = {0};
        return $.secte($.terms(t, x -> {
            return Atomic.the(Strings.repeat(String.valueOf(prefix), ++index[0]) + x);
        }));
    }

    static @NotNull Term pRecurse(@NotNull Term... t) {
        int tl = t.length;
        Term inner = t[--tl];
        Term nextInner = inner.op()!=PROD ? $.p(inner) : inner; //wrap innermost item in product too, for fairness
        while (tl > 0) {
            Term next = t[--tl];
            nextInner = next.op()!=PROD ? $.p(next, nextInner) : $.p( ArrayUtils.add(next.subterms().theArray(), nextInner) );
        }
        return nextInner;
    }

    static @Nullable Compound inhRecurse(@NotNull Term... t) {
        int tl = t.length;
        @NotNull Term bottom = t[--tl];
        Compound nextInner = $.inh(t[--tl], bottom); //wrap innermost item in product too, for fairness
        while (nextInner != null && tl > 0) {
            nextInner = $.inh(t[--tl], nextInner);
        }
        return nextInner;
    }

//    public static void logLevel(Class logClass, Level l) {
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logClass)).setLevel(l);
//    }

//    @NotNull
//    public static TaskBuilder command(@NotNull Compound op) {
//        //TODO use lightweight CommandTask impl without all the logic metadata
//        TaskBuilder t = new TaskBuilder(op, COMMAND, null);
//        t.setPri(1f);
//        return t;
//    }
//
//    @NotNull
//    public static TaskBuilder command(@NotNull String functor, Term... args) {
//        //TODO use lightweight CommandTask impl without all the logic metadata
//        return command(func(functor, args));
//    }

    @NotNull
    static String unquote(@NotNull Term s) {
        return unquote(s.toString());
    }

    @NotNull
    static String unquote(String x) {
        while (true) {
            int len = x.length();
            if (len > 0 && x.charAt(0) == '\"' && x.charAt(len - 1) == '\"') {
                x = x.substring(1, len - 1);
            } else {
                return x;
            }
        }
    }


    /**
     * instantiate new Javascript context
     */
    static NashornScriptEngine JS() {
        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    }

    static Term nonNull(@Nullable Term term) {
        return term != null ? term : Null;
    }

    static <X> PrediTerm<X> IF(Term t, Predicate<X> test) {
        return new LambdaPred<>(t, test);
    }

    static <X> PrediTerm<X> AND(PrediTerm<X> a, PrediTerm<X> b) {
        return new LambdaPred<>($.conj(a, b), (X x) -> {
            return a.test(x) && b.test(x);
        });
    }

    static <X> PrediTerm<X> OR(PrediTerm<X> a, PrediTerm<X> b) {
        return new LambdaPred<>($.disj(a, b), (X x) -> {
            return a.test(x) || b.test(x);
        });
    }

    static int intValue(Term intTerm) throws NumberFormatException {
        if (intTerm instanceof Int)
            return ((Int) intTerm).id;
//        if (intTerm instanceof Atom) {
//            String xs = intTerm.toString();
//            return Texts.i(xs);
//        } else {
        throw new NumberFormatException();
        //   }
    }

    static int intValue(Term intTerm, int ifNotInt) {
        if (intTerm instanceof Int)
            return ((Int) intTerm).id;
        else
            return ifNotInt;
    }

    static Term fromJSON(String j) {
        return JsonTerm.the(j);
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
