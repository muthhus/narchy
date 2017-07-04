package nars;


import nars.derive.meta.match.Ellipsislike;
import nars.index.TermBuilder;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicSingleton;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.util.InvalidTermException;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static nars.index.TermBuilder.flatten;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    VAR_INDEP('$', 6 /*NAL6 for Indep Vars */, OpType.Variable),
    VAR_DEP('#', Op.ANY_LEVEL, OpType.Variable),
    VAR_QUERY('?', Op.ANY_LEVEL, OpType.Variable),


    NEG("--", 5, Args.One) {
        private Term neg(Term x) {
            if (x instanceof Compound) {
                // (--,(--,P)) = P
                if (x.op() == NEG)
                    return x.unneg();
            } else if (x instanceof AtomicSingleton) {
                if (x.equals(False)) return True;
                if (isTrue(x)) return False;
                else return Null;
            }

            return new UnitCompound1(NEG, x);

        }

        @Override
        public Term the(TermBuilder b, int dt, Term[] u) {
            assert (u.length == 1);
            assert (dt == DTERNAL);
            return neg(u[0]);
        }

        @Override
        public @NotNull Term the(int dt, TermContainer subterms) {
            assert (subterms.size() == 1);
            assert (dt == DTERNAL);
            return neg(subterms.sub(0));
        }
    },

    INH("-->", 1, OpType.Statement, Args.Two),
    SIM("<->", true, 2, OpType.Statement, Args.Two),

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo),

    /**
     * intensional intersection
     */
    SECTi("|", true, 3, Args.GTETwo),

    /**
     * extensional difference
     */
    DIFFe("-", false, 3, Args.Two),

    /**
     * intensional difference
     */
    DIFFi("~", false, 3, Args.Two),

    /**
     * PRODUCT
     */
    PROD("*", 4, Args.GTEZero),

    /**
     * extensional image
     */
    IMGe("/", 4, Args.GTEOne),

    /**
     * intensional image
     */
    IMGi("\\", 4, Args.GTEOne),


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        /**
         * array implementation of the conjunction true/false filter
         */
        @NotNull
        private Term[] conjTrueFalseFilter(@NotNull Term... u) {
            int trues = 0; //# of True subterms that can be eliminated
            for (Term x : u) {
                if (isTrue(x)) {
                    trues++;
                } else if (isFalse(x)) {

                    //false subterm in conjunction makes the entire condition false
                    //this will eventually reduce diectly to false in this method's only callee HACK
                    return FalseArray;
                }
            }

            if (trues == 0)
                return u;

            int ul = u.length;
            if (ul == trues)
                return TrueArray; //reduces to an Imdex itself

            Term[] y = new Term[ul - trues];
            int j = 0;
            for (int i = 0; j < y.length; i++) {
                Term uu = u[i];
                if (!isTrue(uu)) // && (!uu.equals(False)))
                    y[j++] = uu;
            }

            assert (j == y.length);

            return y;
        }


        @Override
        public @NotNull Term the(int dt, TermContainer subterms) {
            return the($.terms, dt, subterms.toArray());
        }


        @Override
        public Term the(TermBuilder builder, int dt, Term[] uu) {
            Term[] u = conjTrueFalseFilter(uu);

            final int n = u.length;


            if (n == 1) {
                Term only = u[0];

                //preserve unitary ellipsis for patterns etc
                return only instanceof Ellipsislike ?
                        new GenericCompound(CONJ, TermVector.the(only)) //special
                        :
                        only;

            }

            if (n == 0)
                return Null;


            if (dt == XTERNAL) {
                assert (n == 2); //throw new InvalidTermException(CONJ, XTERNAL, "XTERNAL only applies to 2 subterms, as dt placeholder", u);

                return conjPost(builder.the(CONJ, u).dt(XTERNAL), builder);
            }

            boolean commutive = concurrent(dt);
            if (commutive) {

                return conjPost(junctionFlat(dt, builder, u), builder);

            } else {
                //NON-COMMUTIVE

                //assert (n == 2);
                if (n != 2) {
                    throw new InvalidTermException(CONJ, u, "invalid non-commutive conjunction");
                }

                Term a = u[0];
                Term b = u[1];
                boolean equal = a.equals(b);
                if (equal) {
                    if (dt < 0) {
                        //make dt positive to avoid creating both (x &&+1 x) and (x &&-1 x)
                        dt = -dt;
                    }
                } else {
                    if (a.compareTo(b) > 0) {
                        //ensure lexicographic ordering

                        Term x = u[0];
                        u[0] = u[1];
                        u[1] = x; //swap
                        dt = -dt; //and invert time
                    }
                }

                return conjPost(builder.the(CONJ, u).dt(dt), builder);

            }
        }

        /**
         * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
         * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
         */
        @NotNull
        private Term junctionFlat(int dt, TermBuilder builder, @NotNull Term... u) {

            //TODO if there are no negations in u then an accelerated construction is possible

            assert (u.length > 0 && (dt == 0 || dt == DTERNAL || dt == XTERNAL)); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");

            ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length * 2);

            if (flatten(CONJ, u, dt, s) && !s.isEmpty()) {
                Set<Term> cs = junctionGroupNonDTSubterms(s, dt);
                if (!cs.isEmpty()) {


                    //annihilate common terms inside and outside of disjunction
                    //      ex:
                    //          -X &&  ( X ||  Y)
                    //          -X && -(-X && -Y)  |-   -X && Y
                    Iterator<Term> csi = cs.iterator();
                    List<Term> csa = null;
                    while (csi.hasNext()) {
                        Term x = csi.next();

                        if (x.op() == NEG && x.subIs(0, CONJ)) { //DISJUNCTION
                            Compound disj = (Compound) x.unneg();
                            Set<Term> disjSubs = disj.toSet();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(cs)) {
                                //reconstruct disj if changed
                                csi.remove();

                                if (!disjSubs.isEmpty()) {
                                    Term y = NEG.the($.the(CONJ, disjSubs).dt(disj.dt()));
                                    if (csa == null)
                                        csa = $.newArrayList(1);
                                    csa.add(y);
                                }
                            }
                        }
                    }
                    if (csa != null)
                        cs.addAll(csa);

                    Term[] scs = Terms.sorted(cs);
                    if (scs.length == 1)
                        return scs[0];

                    return new GenericCompound(CONJ, builder.subterms(scs)).dt(dt);
                }
            }

            return False;
        }

        /**
         * this is necessary to keep term structure consistent for intermpolation.
         * by grouping all non-sequence subterms into its own subterm, future
         * flattening and intermpolation is prevented from destroying temporal
         * measurements.
         *
         * @param innerDT will either 0 or DTERNAL (commutive relation)
         */
        private @NotNull Set<Term> junctionGroupNonDTSubterms(@NotNull ObjectByteHashMap<Term> s, int innerDT) {

            Set<Term> outer = new HashSet(s.size());

            for (ObjectBytePair<Term> xn : s.keyValuesView()) {
                Term x = xn.getOne();
                outer.add((xn.getTwo() < 0) ? NEG.the(x) : x);
            }
            return outer;


        }


        private Term conjPost(Term x /* possibly a conjunction */, TermBuilder builder) {

            if (x == null)
                return null;

            if (x.op() != CONJ)
                return x;

            //conjunction/implication reduction:
            if (x.hasAny(IMPL)) {
                //if there is only one implication subterm, then fold into that.
                //if there are more than one, don't do anything (for now)
                Compound c = ((Compound) x);
                int whichImpl = -1;
                for (int i = 0; i < c.size(); i++) {
                    if (c.subIs(i, Op.IMPL)) {
                        if (whichImpl != -1) {
                            //a 2nd implication was found; don't continue
                            whichImpl = -1;
                            break;
                        }
                        whichImpl = i;
                    }
                }

                if (whichImpl != -1) {

                    int ww = whichImpl;
//                Term[] precond = c.subterms().terms(
//                        (IntObjectPredicate<Term>)((i,s)->(i != ww)));
                    Term implPre = ((Compound) c.sub(whichImpl)).sub(0);
                    Term implPost = ((Compound) c.sub(whichImpl)).sub(1);
                    Compound origImpl = (Compound) c.sub(ww);
                    Term newPre = builder.replace(c, origImpl, implPre);
                    if (newPre != null)
                        return builder.the(IMPL, newPre, implPost).dt(origImpl.dt());

                }

            }

            return x;
        }

    },

    //SPACE("+", true, 7, Args.GTEOne),


    /**
     * intensional set
     */
    SETi("[", true, 2, Args.GTEOne), //OPENER also functions as the symbol for the entire compound

    /**
     * extensional set
     */
    SETe("{", true, 2, Args.GTEOne), //OPENER also functions as the symbol for the entire compound


    /**
     * implication
     */
    IMPL("==>", 5, OpType.Statement, Args.Two),

    /**
     * equivalence
     */
    EQUI("<=>", true, 5, OpType.Statement, Args.Two),


    // keep all items which are invlved in the lower 32 bit structuralHash above this line
    // so that any of their ordinal values will not exceed 31
    //-------------
    //NONE('\u2205', Op.ANY, null),


    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),


    //VIRTUAL TERMS
    @Deprecated
    INSTANCE("-{-", 2, OpType.Statement),

    @Deprecated
    PROPERTY("-]-", 2, OpType.Statement),

    @Deprecated
    INSTANCE_PROPERTY("{-]", 2, OpType.Statement),

    @Deprecated
    DISJ("||", true, 5, Args.GTETwo) {
        @Override
        public @NotNull Term the(Term... u) {
            assert(u.length > 1);
            return NEG.the($.the(CONJ, TermBuilder.neg(u)));
        }

        @Override
        public Term the(TermBuilder b, int dt, Term[] u) {
            assert(dt == DTERNAL);
            return the(u);
        }

        @Override
        public @NotNull Term the(int dt, TermContainer subterms) {
            assert(dt == DTERNAL);
            return the(subterms.toArray());
        }
    },

    /**
     * for ellipsis, when seen as a term
     */
    //SUBTERMS("...", 1, OpType.Other)
    ;

    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL, Op.EQUI);

    public static final int OpBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int EvalBits = OpBits; //just an alias for code readabiliy

    public static final int InhAndIMGbits = Op.or(Op.INH, Op.IMGe, Op.IMGi);

    public static final byte BELIEF = '.';
    public static final byte QUESTION = '?';
    public static final byte GOAL = '!';
    public static final byte QUEST = '@';
    public static final byte COMMAND = ';';

    public static final String TENSE_PAST = ":\\:";
    public static final String TENSE_PRESENT = ":|:";
    public static final String TENSE_FUTURE = ":/:";

    public static final String TENSE_ETERNAL = ":-:"; //ascii infinity symbol
    public static final String TASK_RULE_FWD = "|-";

    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';

    public static final char ARGUMENT_SEPARATOR = ',';

    public static final char IMAGE_PLACE_HOLDER = '_';
    public static final char SET_INT_CLOSER = ']';
    public static final char SET_EXT_CLOSER = '}';
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';

    @Deprecated
    public static final char STATEMENT_OPENER = '<';
    @Deprecated
    public static final char STATEMENT_CLOSER = '>';

    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';


    /**
     * Image index ("imdex") symbol for products, and anonymous variable in products
     */
    public final static char ImdexSym = '_';
    public static final Atomic Imdex =
            new UnnormalizedVariable(Op.VAR_DEP, String.valueOf(ImdexSym)) {

                final int RANK = Term.opX(VAR_PATTERN, 20 /* different from normalized variables with a subOp of 0 */);

                @Override
                public int opX() {
                    return RANK;
                }
            };


    public static final char TrueSym = '†';
    public static final char FalseSym = 'Ø';
    public static final char NullSym = (char) 133 /* horizontal ellipsis */;

    /**
     * absolutely invalid
     */
    public static final AtomicSingleton Null = new AtomicSingleton(String.valueOf(NullSym));

    /**
     * absolutely true
     */
    public static final AtomicSingleton True = new AtomicSingleton(String.valueOf(TrueSym)) {
        @NotNull
        @Override
        public Term unneg() {
            return False;
        }
    };

    /**
     * absolutely false
     */
    public static final AtomicSingleton False = new AtomicSingleton(String.valueOf(FalseSym)) {
        @NotNull
        @Override
        public Term unneg() {
            return True;
        }
    };

    public static final Term[] TrueArray = {True};
    public static final Term[] FalseArray = {False};


    /**
     * string representation
     */
    @NotNull
    public final String str;

    /**
     * character representation if symbol has length 1; else ch = 0
     */
    public final char ch;

    public final OpType type;

    /**
     * arity limits, range is inclusive >= <=
     * -1 for unlimited
     */
    public final int minSize, maxSize;


    /**
     * minimum NAL level required to use this operate, or 0 for N/A
     */
    public final int minLevel;

    public final boolean commutative;
    public final boolean temporal;
    public final int bit;
    public final boolean var;
    public final boolean atomic;
    public final boolean statement;
    public final boolean image;

    /**
     * whether this involves an additional numeric component: 'dt' (for temporals) or 'relation' (for images)
     */
    public final boolean hasNumeric;

    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

    Op(@NotNull String s, boolean commutative, int minLevel, @NotNull IntIntPair size) {
        this(s, commutative, minLevel, OpType.Other, size);
    }

    Op(char c, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(Character.toString(c), minLevel, type, size);
    }

    Op(@NotNull String string, int minLevel, @NotNull IntIntPair size) {
        this(string, minLevel, OpType.Other, size);
    }

    Op(@NotNull String string, int minLevel, OpType type) {
        this(string, false /* non-commutive */, minLevel, type, Args.None);
    }

    Op(@NotNull String string, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(string, false /* non-commutive */, minLevel, type, size);
    }

    Op(@NotNull String string, boolean commutative, int minLevel, OpType type, @NotNull IntIntPair size) {

        this.str = string;

        this.commutative = commutative;
        this.minLevel = minLevel;
        this.type = type;

        this.ch = string.length() == 1 ? string.charAt(0) : 0;

        this.minSize = size.getOne();
        this.maxSize = size.getTwo();

        this.var = (type == OpType.Variable);

        this.statement = str.equals("-->") || str.equals("==>") || str.equals("<=>") || str.equals("<->");
        this.temporal = str.equals("&&") || str.equals("==>") || str.equals("<=>");
        //in(or(CONJUNCTION, IMPLICATION, EQUIV));

        this.image = str.equals("/") || str.equals("\\");

        this.hasNumeric = image || temporal;

        //negation does not contribute to structure vector
        this.bit = (1 << ordinal());

        this.atomic = var || str.equals(".") /* atom */ || str.equals("`i") || str.equals("^") || str.equals("`");

    }


    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean isAbsolute(@NotNull Term x) {
        return x instanceof AtomicSingleton;
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return isTrue(x) || isFalse(x);
    }

    public static boolean isTrue(@NotNull Term x) {
        return x == True;
    }

    public static boolean isFalse(@NotNull Term x) {
        return x == False;
    }

    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }


    /**
     * decode a term which may be a functor, return null if it isnt
     */
    @Nullable
    public static Pair<Atomic, Compound> functor(@NotNull Term maybeOperation, TermContext index, boolean mustFunctor) {
        if (maybeOperation instanceof Compound && maybeOperation.hasAll(Op.OpBits)) {
            Compound c = (Compound) maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0 instanceof Compound && s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atom /*&& s1.op() == ATOM*/) {
                        Atom ff = (Atom) index.getIfPresentElse(s1);
                        if (!mustFunctor || ff instanceof Functor) {
                            return Tuples.pair(
                                    ff,
                                    ((Compound) s0)
                            );
                        }

                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Pair<Atomic, Compound> functor(Op cOp, @NotNull Term[] subs, TermContext index, boolean mustFunctor) {
        if (cOp == INH) {
            Term s0 = subs[0];
            if (s0.op() == PROD) {
                Term s1 = subs[1];
                if (s1.op() == ATOM) {
                    Atomic ff = (Atomic) index.getIfPresentElse(s1);
                    if (!mustFunctor || ff instanceof Functor) {
                        return Tuples.pair(
                                ff,
                                ((Compound) s0)
                        );

                    }
                }
            }

        }
        return null;
    }

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/


    @NotNull
    @Override
    public String toString() {
        return str;
    }


    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w) throws IOException {
        append(c, w, false);
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w, boolean invertDT) throws IOException {
        int t = c.dt();
        boolean hasTime = t != Tense.DTERNAL;

        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {

            if (invertDT)
                t = -t;

            if (t >= 0) w.append('+');
            String ts;
            if (t == XTERNAL)
                ts = "-";
            else
                ts = Integer.toString(t);
            w.append(ts).append(' ');
        }
    }

    public boolean commute(int dt, Term[] u) {
        if (commutative) {
            if (temporal) {
                /*if (dt == XTERNAL) {

                }*/
                if (!Op.concurrent(dt))
                    return false;
            }
            return true;
        }
        return false;
    }


    @NotNull
    public Term the(Term... u) {
        return the(u, $.terms);
    }

    @NotNull
    public final Term the(Term[] u, TermBuilder b) {
        return the(b, DTERNAL, u);
    }

    public Term the(TermBuilder b, int dt, Term[] u) {
        if (statement) {
            return statement(this, dt, u[0], u[1]);
        } else {
            return the(dt, b.subterms(commute(dt, u) ? Terms.sorted(u) : u));
        }
    }

    @NotNull
    public Term the(int dt, TermContainer subterms) {
        assert (!atomic);
        int s = subterms.size();
        assert (s > 0);
        switch (s) {
            case 1: {
                assert (dt == DTERNAL);
                return new UnitCompound1(this, subterms.sub(0));
            }
            default: {
                if (statement) {
                    return statement(this, dt, subterms.sub(0), subterms.sub(1));
                } else {
                    return new GenericCompound(this, subterms).dt(dt);
                }
            }
        }
    }

    /**
     * true if matches any of the on bits of the vector
     */
    public final boolean in(int vector) {
        return in(bit, vector);
    }

    @Deprecated
    public boolean isSet() {
        return in(SetsBits);
    }


    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(@NotNull Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    /**
     * specifier for any NAL level
     */
    public static final int ANY_LEVEL = 0;


    /**
     * top-level Op categories
     */
    public enum OpType {
        Statement,
        Variable,
        Other
    }


    enum Args {
        ;
        static final IntIntPair None = pair(0, 0);
        static final IntIntPair One = pair(1, 1);
        static final IntIntPair Two = pair(2, 2);

        static final IntIntPair GTEZero = pair(0, -1);
        static final IntIntPair GTEOne = pair(1, -1);
        static final IntIntPair GTETwo = pair(2, -1);

    }


    public static final int SetsBits = or(Op.SETe, Op.SETi);
    public static final int ImplicationOrEquivalenceBits = or(Op.EQUI, Op.IMPL);
    public static final int TemporalBits = or(Op.CONJ, Op.EQUI, Op.IMPL);
    public static final int ImageBits = or(Op.IMGe, Op.IMGi);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);


    public static final int[] NALLevelEqualAndAbove = new int[8 + 1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1

    private static final int InvalidEquivalenceTerm = or(IMPL, EQUI);
    private static final int InvalidImplicationSubj = or(EQUI, IMPL);
    private static final int InvalidImplicationPred = or(EQUI);

    static boolean validEquivalenceTerm(@NotNull Term t) {
        //return !t.opUnneg().in(InvalidEquivalenceTerm);
        return !t.hasAny(InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }
    @NotNull
    static Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;

        switch (op) {

            case SIM:

                if (subject.equals(predicate))
                    return True;
                if (isAbsolute(subject) || isAbsolute(predicate))
                    return False;
                break;

            case INH:

                if (subject.equals(predicate)) //equal test first to allow, ex: False<->False to result in True
                    return True;
                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate))
                    return False;

                boolean sNeg = subject.op() == NEG;
                boolean pNeg = predicate.op() == NEG;
                if (sNeg && pNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (sNeg && !pNeg) {
                    return NEG.the(statement(op, dt, subject.unneg(), predicate)); //TODO loop and not recurse, needs negation flag to be applied at the end before returning
                } else if (pNeg && !sNeg) {
                    return NEG.the(statement(op, dt, subject, predicate.unneg()));
                }

                break;


            case EQUI:

                //if (isTrue(subject)) return predicate;
                //if (isTrue(predicate)) return subject;
                //if (isFalse(subject)) return neg(predicate);
                //if (isFalse(predicate)) return neg(subject);
                if (concurrent(dt) && subject.equals(predicate))
                    return True;
                if (isTrue(subject) || isFalse(subject))
                    return False; //otherwise they are absolutely inequal

                if (!validEquivalenceTerm(subject))
                    throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (!validEquivalenceTerm(predicate))
                    throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);

                boolean subjNeg = subject.op() == NEG;
                boolean predNeg = predicate.op() == NEG;
                if (subjNeg && predNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (!subjNeg && predNeg) {
                    //factor out (--, ...)
                    return $.neg(statement(op, dt, subject, predicate.unneg()));
                } else if (subjNeg && !predNeg) {
                    //factor out (--, ...)
                    return $.neg(statement(op, dt, subject.unneg(), predicate));
                }

                if (dt == XTERNAL) {

                    //create as-is
                    return $.the(op, XTERNAL, subject, predicate);

                } else {
                    boolean equal = subject.equals(predicate);
                    if (concurrent(dt)) {
                        if (equal) {
                            return True;
                        }
                    } else {
                        if (dt < 0 && equal) {
                            dt = -dt; //use only the forward direction on a repeat
                        }
                    }
                }

                break;

            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False
                if (isTrueOrFalse(subject /* antecedent */)) {
                    if (concurrent(dt))
                        return $.negIf(predicate, isFalse(subject));
                    else {
                        return Null; //no temporal basis
                    }
                }
                if (isAbsolute(predicate /* consequence */))
                    return Null;
                if (subject.hasAny(InvalidImplicationSubj))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (predicate.hasAny(InvalidImplicationPred))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);


                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    return NEG.the($.the(op, dt, subject, predicate.unneg()));
                }

                if (dt == XTERNAL) {
                    //create as-is
                    return $.the(op, subject, predicate).dt(XTERNAL);
                } else {
                    if (concurrent(dt)) {
                        if (subject.equals(predicate))
                            return True;
                    } //else: allow repeat
                }


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (dt != XTERNAL && predicate.op() == IMPL) {
                    Compound cpr = (Compound) predicate;
                    int cprDT = cpr.dt();
                    if (cprDT != XTERNAL) {
                        Term a = cpr.sub(0);

                        subject = $.the(CONJ, subject, a).dt(dt);
                        predicate = cpr.sub(1);
                        return statement(IMPL, cprDT, subject, predicate);
                    }
                }


                break;
        }


        //factor out any common subterms iff concurrent
        if (concurrent(dt)) {

            Term pu = predicate.unneg();
            Term su = subject.unneg();
            //first layer only, not recursively
            if ((pu.varPattern() == 0 && (subject.equals(pu) || subject.containsRecursively(pu))) ||
                    (su.varPattern() == 0 && (predicate.equals(su) || predicate.containsRecursively(su))))
                //(!(su instanceof Variable) && predicate.contains(su)))
                return False; //cyclic

//            if (subject.varPattern() == 0 && predicate.varPattern() == 0 &&
//                    !(subject instanceof Variable) && !(predicate instanceof Variable) &&
//                    (subject.containsRecursively(predicate) || predicate.containsRecursively(subject))) //first layer only, not recursively
//                return False; //cyclic

            if ((op == IMPL || op == EQUI)) { //TODO verify this works as it should


                boolean subjConj = subject.op() == CONJ && concurrent(((Compound) subject).dt());
                boolean predConj = predicate.op() == CONJ && concurrent(((Compound) predicate).dt());
                if (subjConj && !predConj) {
                    final Compound csub = (Compound) subject;
                    //TermContainer subjs = csub.subterms();
                    if (csub.containsRecursively(predicate)) {
                        return False;
//                        Term finalPredicate = predicate;
//                        subject = the(CONJ, csub.dt(), subjs.asFiltered(z -> z.equals(finalPredicate)).toArray());
//                        predicate = False;
//                        return statement(op, dt, subject, predicate);
                    }
                } else if (predConj && !subjConj) {
                    final Compound cpred = (Compound) predicate;
                    //TermContainer preds = cpred.subterms();
                    if (cpred.containsRecursively(subject)) {
                        return False;
//                        Term finalSubject = subject;
//                        predicate = the(CONJ, cpred.dt(), preds.asFiltered(z -> z.equals(finalSubject)).toArray());
//                        subject = False;
//                        return statement(op, dt, subject, predicate);
                    }

                } else if (subjConj && predConj) {
                    final Compound csub = (Compound) subject;
                    TermContainer subjs = csub.subterms();
                    final Compound cpred = (Compound) predicate;
                    TermContainer preds = cpred.subterms();

                    MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                    if (common != null && !common.isEmpty()) {

                        @NotNull Set<Term> sss = subjs.toSet();
                        if (sss.removeAll(common)) {
                            int s0 = sss.size();
                            switch (s0) {
                                case 0:
                                    subject = True;
                                    break;
                                case 1:
                                    subject = sss.iterator().next();
                                    break;
                                default:
                                    subject = $.the(CONJ, csub.dt(), sss);
                                    break;
                            }
                        }

                        @NotNull Set<Term> ppp = preds.toSet();
                        if (ppp.removeAll(common)) {
                            int s0 = ppp.size();
                            switch (s0) {
                                case 0:
                                    predicate = True;
                                    break;
                                case 1:
                                    predicate = ppp.iterator().next();
                                    break;
                                default:
                                    predicate = $.the(CONJ, cpred.dt(), ppp);
                                    if (predicate == null)
                                        return Null;
                                    break;
                            }
                        }

                        return statement(op, dt, subject, predicate);
                    }
                }
            }
        }

//            if (op == INH || op == SIM || dt == 0 || dt == DTERNAL) {
//                if ((subject instanceof Compound && subject.varPattern() == 0 && subject.containsRecursively(predicate)) ||
//                        (predicate instanceof Compound && predicate.varPattern() == 0 && predicate.containsRecursively(subject))) {
//                    return False; //self-reference
//                }
//            }

        if (op.commutative) {

            //normalize co-negation
            boolean sn = subject.op() == NEG;
            boolean pn = predicate.op() == NEG;

            if ((sn == pn) && (subject.compareTo(predicate) > 0)) {
                Term x = predicate;
                predicate = subject;
                subject = x;
                if (!concurrent(dt))
                    dt = -dt;
            }

            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));

        }

        return new GenericCompound(op, TermVector.the(subject, predicate)).dt(dt); //use the calculated ordering, not the TermContainer default for commutives
    }

    /**
     * index of operators which are encoded by 1 byte: must be less than 31 because this is the range for control characters
     */
    static final int numByteSymbols = 15;
    static final Op[] byteSymbols = new Op[numByteSymbols];
    static final ImmutableMap<String, Op> stringToOperator;
    //static final CharObjectHashMap<Op> _charToOperator = new CharObjectHashMap(values().length * 2);


    public static Op fromString(String s) {
        return stringToOperator.get(s);
    }

    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0; //count special ops as level 0, so they can be detected there
            for (int i = l; i <= 8; i++) {
                NALLevelEqualAndAbove[i] |= o.bit;
            }
        }

        final Map<String, Op> _stringToOperator = new HashMap<>(values().length * 2);

        //Setup NativeOperator String index hashtable
        for (Op r : Op.values()) {
            _stringToOperator.put(r.toString(), r);
            int ordinal = r.ordinal();
            if (ordinal < 15)
                Op.byteSymbols[ordinal] = r;
        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);

        //System.out.println(Arrays.toString(byteSymbols));

        //VERIFICATION: Look for any empty holes in the byteSymbols table, indicating that the representation is not contigous
        //index 0 is always 0 to maintain \0's semantics
        //if # of operators are reduced in the future, then this will report that the table size should be reduced (avoiding unnecessary array lookups)
        for (int i = 1; i < Op.byteSymbols.length; i++) {
            if (null == Op.byteSymbols[i])
                throw new RuntimeException("Invalid byteSymbols encoding: index " + i + " is null");
        }

//        //Setup NativeOperator Character index hashtable
//        for (Op r : Op.values()) {
//            char c = r.ch;
//            if (c!=0)
//                Op._charToOperator.put(c, r);
//        }
    }


    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }

}
