package nars;


import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicSingleton;
import nars.term.container.TermContainer;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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


    NEG("--", 5, Args.One),

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
    CONJ("&&", true, 5, Args.GTETwo),

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
    @Deprecated INSTANCE("-{-", 2, OpType.Statement),
    @Deprecated PROPERTY("-]-", 2, OpType.Statement),
    @Deprecated INSTANCE_PROPERTY("{-]", 2, OpType.Statement),
    @Deprecated DISJ("||", true, 5, Args.GTETwo),

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
    public static final Atomic Imdex =
            new UnnormalizedVariable(Op.VAR_DEP, "_") {

                final int RANK = Term.opX(VAR_PATTERN, 20 /* different from normalized variables with a subOp of 0 */);
                @Override public int opX() { return RANK; }
            };

    /** absolutely invalid */
    public static final AtomicSingleton Null = new AtomicSingleton("Null");

    /**
     * absolutely true
     */
    public static final AtomicSingleton True = new AtomicSingleton("†") {
        @NotNull
        @Override
        public Term unneg() {
            return False;
        }
    };

    /**
     * absolutely false
     */
    public static final AtomicSingleton False = new AtomicSingleton("Ø") {
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
    public static Pair<Atomic, TermContainer> functor(@NotNull Term maybeOperation, TermContext index) {
        if (maybeOperation instanceof Compound && maybeOperation.hasAll(Op.OpBits)) {
            Compound c = (Compound) maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1.op() == ATOM) {
                        Atomic ff = (Atomic) index.getIfPresentElse(s1);

                        return Tuples.pair(
                                ff,
                                ((Compound) s0).subterms()
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


//    @NotNull
//    public final String toString(@NotNull Compound c)  {
//        int t = c.dt();
//
//        return t == Tense.DTERNAL ?
//                str :
//                str + ((t >= 0) ? "+" : "") + (Integer.toString(t));
//    }


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

//    public static int or(int bits, @NotNull Op o) {
//        return bits | o.bit;
//    }

    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

//    public static boolean hasAny(int structure, @NotNull Op o) {
//        return hasAny(structure, o.bit);
//    }

//    public final boolean isIntersect() {
//        return this == Op.SECTe || this == Op.SECTi;
//    }
//
//    public static int or(@NotNull int... i) {
//        int bits = 0;
//        for (int x : i) {
//            bits |= x;
//        }
//        return bits;
//    }

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
