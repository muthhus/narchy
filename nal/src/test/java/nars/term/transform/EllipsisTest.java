package nars.term.transform;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.index.Indexes;
import nars.index.PatternIndex;
import nars.index.TermIndex;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.meta.match.EllipsisOneOrMore;
import nars.nal.meta.match.EllipsisZeroOrMore;
import nars.nal.rule.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.subst.FindSubst;
import nars.term.var.Variable;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static nars.$.$;
import static nars.Op.DISJ;
import static nars.Op.VAR_DEP;
import static nars.Op.VAR_PATTERN;
import static nars.nal.meta.match.Ellipsis.firstEllipsis;
import static org.junit.Assert.*;

/**
 * Created by me on 12/12/15.
 */
public class EllipsisTest {


    public interface EllipsisTestCase {
        @NotNull
        Compound getPattern();
        @Nullable
        Compound getResult();
        @Nullable
        Compound getMatchable(int arity);

        default Set<Term> test(int arity, int repeats) {
            Set<Term> selectedFixed = $.newHashSet(arity);

            TermIndex index = new Indexes.DefaultTermIndex(1024, new XorShift128PlusRandom(1));

            Compound y = getMatchable(arity);
            assertNotNull(y);
            assertTrue(y.isNormalized());
            //no variables in Y
            y.forEach(yy -> { assertFalse(yy instanceof Variable); });

            Compound r = getResult();
            assertTrue(r.isNormalized());

            Compound<?> x = getPattern();
            assertTrue(x.isNormalized());

            //no unmatched variables
            ///x.forEach(xx -> { assertFalse(xx.toString() + " is var", xx instanceof Variable ); });

            Term ellipsisTerm = firstEllipsis(x);



            for (int seed = 0; seed < Math.max(1,repeats*arity) /* enough chances to select all combinations */; seed++) {

                //AtomicBoolean matched = new AtomicBoolean(false);

                System.out.println(seed + ": " + x + " " + y + " .. " + r);

                FindSubst f = new FindSubst(index, VAR_PATTERN, new XorShift128PlusRandom(1+seed)) {

                    @Override
                    public boolean onMatch() {
                        //System.out.println(x + "\t" + y + "\t" + this);

                        Term a = term(ellipsisTerm);
                        if (a instanceof EllipsisMatch) {
                            EllipsisMatch varArgs = (EllipsisMatch) a;

                            //matched.set(true);

                            assertEquals(getExpectedUniqueTerms(arity), varArgs.size());

                            Set<Term> varArgTerms = $.newHashSet(1);
                            Term u = term(varArgs);
                            if (u == null) {
                                u = varArgs;
                            }

                            if (u instanceof EllipsisMatch) {
                                EllipsisMatch m = (EllipsisMatch)u;
                                Collections.addAll(varArgTerms, m.term);
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
                        Term s = Termed.termOrNull(index.resolve(r, this));
                        if (s!=null) {
                            //System.out.println(s);
                            if (s.varPattern()==0)
                                selectedFixed.add(s);

                            assertEquals(s.toString() + " should be all subbed by " + this.xy.toString(), 0, s.varPattern());
                        }

                        return true;
                    }
                };

                f.matchAll(x, y);

//                assertTrue(f.toString() + " " + matched,
//                        matched.get());

            }


            return selectedFixed;

        }

        int getExpectedUniqueTerms(int arity);

        default void testFurther(Set<Term> selectedFixed, FindSubst f, Set<Term> varArgTerms) {

        }

        default void test(int arityMin, int arityMax, int repeats) {
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

        public CommutiveEllipsisTest(String ellipsisTerm, String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.ellipsisTerm = ellipsisTerm;
            p = (Compound) new PatternIndex().the(
                    getPattern(prefix, suffix)
                ).term();
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
        protected abstract Compound getPattern(String prefix, String suffix);


        @NotNull
        @Override
        public Compound getPattern() {
            return p;
        }

        @Nullable
        @Override
        public Compound getMatchable(int arity) {
            return $(prefix + termSequence(arity) + suffix);
        }
    }

    public static class CommutiveEllipsisTest1 extends CommutiveEllipsisTest {

        static final Term fixedTerm = $.varPattern(1);


        public CommutiveEllipsisTest1(String ellipsisTerm, String[] openClose) {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) {
            Set<Term> selectedFixed = super.test(arity, repeats);

            /** should have iterated all */
            assertEquals(selectedFixed.toString(), arity, selectedFixed.size());
            return selectedFixed;
        }

        @Override
        public int getExpectedUniqueTerms(int arity) {
            return arity-1;
        }

        @Override public void testFurther(Set<Term> selectedFixed, @NotNull FindSubst f, @NotNull Set<Term> varArgTerms) {
            assertEquals(2, f.xy.size());
            Term fixedTermValue = f.term(fixedTerm);
            assertNotNull(f.toString(), fixedTermValue);
            assertTrue(fixedTermValue instanceof Atomic);
            assertFalse(varArgTerms.contains(fixedTermValue));
        }


        @NotNull
        @Override
        public Compound getPattern(String prefix, String suffix) {
            PatternIndex pi = new PatternIndex();
            Compound pattern = (Compound) Narsese.the().term(prefix + "%1, " + ellipsisTerm + suffix, pi).term();
            return pattern;
        }



        @NotNull
        @Override
        public Compound getResult() {
            final PatternIndex pi = new PatternIndex();
            return pi.normalize(pi.parse("<%1 --> (" + ellipsisTerm +  ")>"), true).term();
        }

    }

    /** for testing zero-or-more matcher */
    public static class CommutiveEllipsisTest2 extends CommutiveEllipsisTest {

        public CommutiveEllipsisTest2(String ellipsisTerm, String[] openClose) {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) {
            Set<Term> s = super.test(arity, repeats);
            Term the = s.isEmpty() ? null : s.iterator().next();
            assertNotNull(the);
            assertTrue(the.toString().substring(1).length() > 0 && the.toString().substring(1).charAt(0) == 'Z');
            return s;
        }

        @Nullable
        @Override
        public Compound getPattern(String prefix, String suffix) {
            return $(prefix + ellipsisTerm + suffix);
        }



        @Nullable
        @Override
        public Compound getResult() {
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

    @Test
    public void testEllipsisEqualityWithPatternVariable() {

        @NotNull Ellipsis tt = Ellipsis.EllipsisPrototype.make(1,1);
        @NotNull Ellipsis uu = Ellipsis.EllipsisPrototype.make(1,1);

        assertEquals(tt, uu);
        assertEquals(tt, $.v(VAR_PATTERN, 1));
        assertNotEquals(tt, $.v(VAR_PATTERN, 2));
        assertNotEquals(tt, $.v(VAR_DEP, 1));
    }

    @Test
    public void testEllipsisOneOrMore() {
        String s = "%prefix..+";
        Ellipsis.EllipsisPrototype t = $(s);
        assertNotNull(t);
        assertEquals(s, t.toString());
        //assertEquals("%prefix", t.target.toString());
        assertEquals(EllipsisOneOrMore.class, t.normalize(1).getClass());

        //assertEquals(t.target, $("%prefix")); //equality between target and itself
    }

    @Test public void testEllipsisZeroOrMore() {
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
    @Test public void testVarArg0() {
        //String rule = "(%S --> %M), ((|, %S, %A..+ ) --> %M) |- ((|, %A, ..) --> %M), (Belief:DecomposePositiveNegativeNegative)";
        String rule = "(%S ==> %M), ((&&,%S,%A..+) ==> %M) |- ((&&,%A..+) ==> %M), (Belief:DecomposeNegativePositivePositive, Order:ForAllSame, SequenceIntervals:FromBelief)";

        Compound _x = $.$('<' + rule + '>');
        assertTrue(_x.toString(), _x instanceof PremiseRule);
        PremiseRule x = (PremiseRule)_x;
        //System.out.println(x);
        x = x.normalizeRule(new PatternIndex());
        //System.out.println(x);

        assertEquals(
                "(((%1==>%2),((%1&&%3..+)==>%2)),(((&&,%3..+)==>%2),((DecomposeNegativePositivePositive-->Belief),(ForAllSame-->Order),(FromBelief-->SequenceIntervals))))",
                x.toString()
        );

    }

    @Test public void testEllipsisMatchCommutive1_0() {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(|,", ")")).test(2, 2, 4);
    }
    @Test public void testEllipsisMatchCommutive1_00() {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&,", ")")).test(2, 2, 4);
    }

    @Test public void testEllipsisMatchCommutive1_1() {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("{", "}")).test(2, 4, 4);
    }
    @Test public void testEllipsisMatchCommutive1_2() {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("[", "]")).test(2, 4, 4);
    }
    @Test public void testEllipsisMatchCommutive1_3() {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&&,", ")")).test(2, 4, 4);
    }



    @Test public void testEllipsisMatchCommutive2() {
        for (String e : new String[] { "%1..+" }) {
            for (String[] s : new String[][] { p("{", "}"), p("[", "]"), p("(", ")") }) {
                new EllipsisTest.CommutiveEllipsisTest2(e, s).test(1, 5, 0);
            }
        }
    }
    @Test public void testEllipsisMatchCommutive2_empty() {
        for (String e : new String[] { "%1..*" }) {
            for (String[] s : new String[][] { p("(", ")") }) {
                new EllipsisTest.CommutiveEllipsisTest2(e, s).test(0, 2, 0);
            }
        }
    }

    static void testCombinations(Compound X, @NotNull Compound Y, int expect) {
        X = (Compound) new PatternIndex().the(X).term();
        //Y = (Compound) new PatternIndex().the(Y).term();

        for (int seed = 0; seed < 1 /*expect*5*/; seed++) {

            Set<String> results = $.newHashSet(0);

            Random rng = new XorShift128PlusRandom(seed);
            FindSubst f = new FindSubst($.terms, VAR_PATTERN, rng) {
                @Override
                public boolean onMatch() {
                    results.add(xy.toString());
                    return true;
                }
            };

            f.matchAll(X, Y);

            results.forEach(System.out::println);
            assertEquals(expect, results.size());
        }

    }

    @Test public void testEllipsisCombinatorics1() {
        //rule: ((&&,M,A..+) ==> C), ((&&,A,..) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)
        testCombinations(
                $("(&&, %1..+, %2)"),
                $("(&&, <r --> [c]>, <r --> [w]>, <r --> [f]>)"),
                3);
    }

    @Test public void testMatchAll2() {
        testCombinations(
                $("((|,%1,%2) --> (|,%2,%3))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }
    @Test public void testMatchAll3() {
        testCombinations(
                $("((|,%X,%Z,%A) --> (|,%Y,%Z,%A))"),
                $("((|,bird,man, swimmer)-->(|,man, animal,swimmer))"),
                2);
    }

    @Test public void testRepeatEllipsisAWithoutEllipsis() {
        testCombinations(
                $("((|,%X,%Y) --> (|,%Y,%Z))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1 /* weird */);
    }

    @Test public void testRepeatEllipsisA() {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%Y,%A..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1 /* weird */);
    }
    @Test public void testRepeatEllipsisA2() {

        testCombinations(
                $("((%X,%A..+) --> (%Y,%A..+))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test public void testRepeatEllipsisA0() {
        testCombinations(
                $("((%A, %X) --> (%B, %X))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test public void testRepeatEllipsisB() {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%X,%B..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }

    @Test public void testIntersection1() {
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
        Atom a = $.the("a");
        Ellipsis b = new EllipsisOneOrMore($.varPattern(1));

        for (Op o : Op.values()) {
            if (o.minSize <= 1) continue;
            if (o == DISJ) continue;

            if (o.isStatement()) continue;

            assertEquals(o + " with normal term",
                    a, $.compound(o, a));

            assertEquals(o + " with ellipsis not reduced",
                    o.isStatement() ? VAR_PATTERN : o,
                    $.compound(o,b).op());
        }
    }

    //TODO case which actually needs the ellipsis and not single term:
}