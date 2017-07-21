package nars.term.transform;

import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Param;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.derive.match.EllipsisOneOrMore;
import nars.derive.match.EllipsisZeroOrMore;
import nars.derive.rule.PremiseRule;
import nars.index.term.BasicTermIndex;
import nars.index.term.PatternTermIndex;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;
import java.util.Set;

import static nars.$.$;
import static nars.Op.*;
import static nars.derive.match.Ellipsis.firstEllipsis;
import static org.junit.Assert.*;

/**
 * Created by me on 12/12/15.
 */
public class EllipsisTest {


    public interface EllipsisTestCase {
        @NotNull
        Compound getPattern();
        @Nullable
        Compound getResult() throws Narsese.NarseseException;
        @Nullable
        Compound getMatchable(int arity) throws Narsese.NarseseException;

        default Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> selectedFixed = $.newHashSet(arity);

            TermIndex index = new BasicTermIndex(1024);

            Compound y = getMatchable(arity);
            assertNotNull(y);
            assertTrue(y.isNormalized());
            //no variables in Y
            y.forEach(yy -> assertFalse(yy instanceof Variable));

            Compound r = getResult();
            assertTrue(r.isNormalized());

            Compound x = getPattern();
            assertTrue(x.isNormalized());

            //no unmatched variables
            ///x.forEach(xx -> { assertFalse(xx.toString() + " is var", xx instanceof Variable ); });

            Term ellipsisTerm = firstEllipsis(x);
                        assertNotNull(ellipsisTerm);



            for (int seed = 0; seed < Math.max(1,repeats*arity) /* enough chances to select all combinations */; seed++) {

                //AtomicBoolean matched = new AtomicBoolean(false);

                System.out.println(seed + ": " + x + " " + y + " .. " + r);

                Unify f = new Unify(index, VAR_PATTERN, new XorShift128PlusRandom(1+seed), Param.UnificationStackMax, Param.UnificationTTLMax) {

                    @Override
                    public void onMatch() {
                        //System.out.println(x + "\t" + y + "\t" + this);


                        Term a = xy(ellipsisTerm);
                        if (a instanceof EllipsisMatch) {
                            EllipsisMatch varArgs = (EllipsisMatch) a;

                            //matched.set(true);

                            assertEquals(getExpectedUniqueTerms(arity), varArgs.size());

                            Term u = xy(varArgs);
                            if (u == null) {
                                u = varArgs;
                            }

                            Set<Term> varArgTerms = $.newHashSet(1);
                            if (u instanceof EllipsisMatch) {
                                EllipsisMatch m = (EllipsisMatch)u;
                                m.forEach(varArgTerms::add);
                            } else {
                                varArgTerms.add(u);
                            }

                            assertEquals(getExpectedUniqueTerms(arity), varArgTerms.size());

                            testFurther(selectedFixed, this, varArgTerms);

                        } else {
                            assertNotNull(a);
                            //assertEquals("?", a);
                        }


        /*else
            changed |= (u!=this);*/





                        //2. test substitution
                        Term s = Termed.termOrNull(transform(x));
                        if (s!=null) {
                            //System.out.println(s);
                            if (s.varPattern()==0)
                                selectedFixed.add(s);

                            assertEquals(s.toString() + " should be all subbed by " + this.xy.toString(), 0, s.varPattern());
                        }

                    }
                };

                f.unifyAll(x, y);

//                assertTrue(f.toString() + " " + matched,
//                        matched.get());

            }


            return selectedFixed;

        }

        int getExpectedUniqueTerms(int arity);

        default void testFurther(Set<Term> selectedFixed, Unify f, Set<Term> varArgTerms) {

        }

        default void test(int arityMin, int arityMax, int repeats) throws Narsese.NarseseException {
            for (int arity = arityMin; arity <= arityMax; arity++) {
                test(arity, repeats);
            }
        }
    }

    public abstract static class CommutiveEllipsisTest implements EllipsisTestCase {
        protected final String prefix;
        protected final String suffix;
        @NotNull
        protected final Compound p;
        public final String ellipsisTerm;

        public CommutiveEllipsisTest(String ellipsisTerm, String prefix, String suffix) throws Narsese.NarseseException {
            this.prefix = prefix;
            this.suffix = suffix;
            this.ellipsisTerm = ellipsisTerm;
            p = new PatternTermIndex().pattern(
                    getPattern(prefix, suffix)
            );
        }


        static String termSequence(int arity) {
            StringBuilder sb = new StringBuilder(arity * 3);
            for (int i = 0; i < arity; i++) {
                sb.append( (char)('a' + i) );
                if (i < arity-1)
                    sb.append(',');
            }
            return sb.toString();
        }

        @Nullable
        protected abstract Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException;


        @NotNull
        @Override
        public Compound getPattern() {
            return p;
        }

        @Nullable
        @Override
        public Compound getMatchable(int arity) throws Narsese.NarseseException {
            return $(prefix + termSequence(arity) + suffix);
        }
    }

    public static class CommutiveEllipsisTest1 extends CommutiveEllipsisTest {

        static final Term fixedTerm = $.varPattern(1);


        public CommutiveEllipsisTest1(String ellipsisTerm, String[] openClose) throws Narsese.NarseseException {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> selectedFixed = super.test(arity, repeats);

            /** should have iterated all */
            assertEquals(selectedFixed.toString(), arity, selectedFixed.size());
            return selectedFixed;
        }

        @Override
        public int getExpectedUniqueTerms(int arity) {
            return arity-1;
        }

        @Override public void testFurther(Set<Term> selectedFixed, @NotNull Unify f, @NotNull Set<Term> varArgTerms) {
            assertEquals(2, f.xy.size());
            Term fixedTermValue = f.xy(fixedTerm);
            assertNotNull(f.toString(), fixedTermValue);
            assertTrue(fixedTermValue instanceof Atomic);
            assertFalse(varArgTerms.contains(fixedTermValue));
        }


        @NotNull
        @Override
        public Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException {
            PatternTermIndex pi = new PatternTermIndex();
            Compound pattern = (Compound) pi.term(prefix + "%1, " + ellipsisTerm + suffix).term();
            return pattern;
        }



        @NotNull
        @Override
        public Compound getResult() throws Narsese.NarseseException {
            final PatternTermIndex pi = new PatternTermIndex();
            return pi.<Compound>term("<%1 --> (" + ellipsisTerm + ")>").normalize().term();
        }

    }

    /** for testing zero-or-more matcher */
    public static class CommutiveEllipsisTest2 extends CommutiveEllipsisTest {

        public CommutiveEllipsisTest2(String ellipsisTerm, String[] openClose) throws Narsese.NarseseException {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> s = super.test(arity, repeats);
            Term the = s.isEmpty() ? null : s.iterator().next();
            assertNotNull(the);
            assertTrue(!the.toString().substring(1).isEmpty() && the.toString().substring(1).charAt(0) == 'Z');
            return s;
        }

        @Nullable
        @Override
        public Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException {
            return $(prefix + ellipsisTerm + suffix);
        }



        @Nullable
        @Override
        public Compound getResult() throws Narsese.NarseseException {
            String s = prefix + "Z, " + ellipsisTerm + suffix;
            Compound c = $(s);
            assertNotNull(s + " produced null compound", c);
            return c;
        }

        @Override
        public int getExpectedUniqueTerms(int arity) {
            return arity;
        }
    }

//    @Test
//    public void testEllipsisEqualityWithPatternVariable() {
//
//        @NotNull Ellipsis tt = Ellipsis.EllipsisPrototype.make(1,1);
//        @NotNull Ellipsis uu = Ellipsis.EllipsisPrototype.make(1,1);
//
//        assertEquals(tt, uu);
//        assertEquals(tt, $.v(VAR_PATTERN, 1));
//        assertNotEquals(tt, $.v(VAR_PATTERN, 2));
//        assertNotEquals(tt, $.v(VAR_DEP, 1));
//    }

    @Test
    public void testEllipsisOneOrMore() throws Narsese.NarseseException {
        String s = "%prefix..+";
        Ellipsis.EllipsisPrototype t = $(s);
        assertNotNull(t);
        assertEquals(s, t.toString());
        //assertEquals("%prefix", t.target.toString());
        assertEquals(EllipsisOneOrMore.class, t.normalize(1).getClass());

        //assertEquals(t.target, $("%prefix")); //equality between target and itself
    }

    @Test public void testEllipsisZeroOrMore() throws Narsese.NarseseException {
        String s = "%prefix..*";
        Ellipsis.EllipsisPrototype t = $(s);
        assertNotNull(t);
        assertEquals(s, t.toString());
        //assertEquals("%prefix", t.target.toString());
        assertEquals(EllipsisZeroOrMore.class, t.normalize(0).getClass());
    }


//    @Test public void testEllipsisExpression() {
//        //TODO
//    }

    @NotNull
    public static String[] p(String a, String b) { return new String[] { a, b}; }

    @Ignore
    @Test public void testVarArg0() throws Narsese.NarseseException {
        //String rule = "(%S --> %M), ((|, %S, %A..+ ) --> %M) |- ((|, %A, ..) --> %M), (Belief:DecomposePositiveNegativeNegative)";
        String rule = "(%S ==> %M), ((&&,%S,%A..+) ==> %M) |- ((&&,%A..+) ==> %M), (Belief:DecomposeNegativePositivePositive, Order:ForAllSame, SequenceIntervals:FromBelief)";

        Compound _x = $.$('<' + rule + '>');
        assertTrue(_x.toString(), _x instanceof PremiseRule);
        PremiseRule x = (PremiseRule)_x;
        //System.out.println(x);
        x = x.normalizeRule(new PatternTermIndex());
        //System.out.println(x);

        assertEquals(
                "(((%1==>%2),((%1&&%3..+)==>%2)),(((&&,%3..+)==>%2),((DecomposeNegativePositivePositive-->Belief),(ForAllSame-->Order),(FromBelief-->SequenceIntervals))))",
                x.toString()
        );

    }

    @Test public void testEllipsisMatchCommutive1_0() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(|,", ")")).test(2, 2, 4);
    }
    @Test public void testEllipsisMatchCommutive1_00() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&,", ")")).test(2, 2, 4);
    }

    @Test public void testEllipsisMatchCommutive1_1() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("{", "}")).test(2, 4, 4);
    }
    @Test public void testEllipsisMatchCommutive1_2() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("[", "]")).test(2, 4, 4);
    }
    @Test public void testEllipsisMatchCommutive1_3() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&&,", ")")).test(2, 4, 4);
    }



    @Test public void testEllipsisMatchCommutive2() throws Narsese.NarseseException {
        for (String e : new String[] { "%1..+" }) {
            for (String[] s : new String[][] { p("{", "}"), p("[", "]"), p("(", ")") }) {
                new EllipsisTest.CommutiveEllipsisTest2(e, s).test(1, 5, 0);
            }
        }
    }

    @Test public void testEllipsisMatchCommutive2_empty() throws Narsese.NarseseException {
        for (String e : new String[] { "%1..*" }) {
            for (String[] s : new String[][] { p("(", ")") }) {
                new EllipsisTest.CommutiveEllipsisTest2(e, s).test(0, 2, 0);
            }
        }
    }

    static void testCombinations(Compound X, @NotNull Compound Y, int expect) {
        X = new PatternTermIndex().pattern(X);

        for (int seed = 0; seed < 3 /*expect*5*/; seed++) {

            Set<String> results = $.newHashSet(0);

            Random rng = new XorShift128PlusRandom(seed);
            Unify f = new Unify($.terms, VAR_PATTERN, rng, Param.UnificationStackMax, Param.UnificationTTLMax) {
                @Override
                public void onMatch() {
                    results.add(xy.toString());
                }
            };

            f.unifyAll(X, Y);

            results.forEach(System.out::println);
            assertEquals(expect, results.size());
        }

    }

    @Test public void testEllipsisCombinatorics1() throws Narsese.NarseseException {
        //rule: ((&&,M,A..+) ==> C), ((&&,A,..) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)
        testCombinations(
                $("(&&, %1..+, %2)"),
                $("(&&, x, y, z)"),
                3);
    }

    @Test public void testMatchAll2() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%1,%2) --> (|,%2,%3))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }
    @Test public void testMatchAll3() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%X,%Z,%A) --> (|,%Y,%Z,%A))"),
                $("((|,bird,man, swimmer)-->(|,man, animal,swimmer))"),
                2);
    }

    @Test public void testRepeatEllipsisAWithoutEllipsis() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%X,%Y) --> (|,%Y,%Z))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1 /* weird */);
    }

    @Test public void testRepeatEllipsisA() throws Narsese.NarseseException {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%Y,%A..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }
    @Test public void testRepeatEllipsisA2() throws Narsese.NarseseException {

        testCombinations(
                $("((%X,%A..+) --> (%Y,%A..+))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test public void testRepeatEllipsisA0() throws Narsese.NarseseException {
        testCombinations(
                $("((%A, %X) --> (%B, %X))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test public void testRepeatEllipsisB() throws Narsese.NarseseException {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%X,%B..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }

    @Test public void testIntersection1() throws Narsese.NarseseException {
        testCombinations(
                $("(%M --> (|,%S,%A..+))"),
                $("(m-->(|,s,a))"),
                2);
        testCombinations(
                $("(%M --> (&,%S,%A..+))"),
                $("(m-->(&,s,a))"),
                2);
    }



    @Test public void testEllipsisInMinArity() {
        Atomic a = Atomic.the("a");
        Ellipsis b = new EllipsisOneOrMore($.varPattern(1));

        for (Op o : Op.values()) {
            if (o.minSize <= 1) continue;
            if (o.virtual)
                continue;

            if (o.statement) continue;

            if (o!=DIFFe && o!=DIFFi) {
                assertEquals(o + " with normal term", a, $.the(o, a));
            } else {
                assertEquals(Null, $.the(o, a));
            }

            assertEquals(o + " with ellipsis not reduced",
                    o.statement ? VAR_PATTERN : o,
                    $.the(o,b).op());
        }
    }

    //TODO case which actually needs the ellipsis and not single term:
}