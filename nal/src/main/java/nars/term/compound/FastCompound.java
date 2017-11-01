package nars.term.compound;

import jcog.Util;
import nars.Builder;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.UncheckedBytes;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiFunction;

import static nars.time.Tense.DTERNAL;

/**
 * Annotates a GenericCompound with cached data to accelerate pattern matching
 * TODO not finished yet
 */
public class FastCompound implements Compound {

    static public final Op[] ov = Op.values();

    private static final int MAX_LAYERS = 8;
    private int MAX_LAYER_LEN = 8;

    @NotNull
    private final byte[][] atoms;
    @NotNull
    private final byte[] skeleton;

    final int hash;
    private final int hashSubterms;
    final byte volume;
    boolean normalized;

    public FastCompound(byte[][] atoms, byte[] skeleton, int hash, int hashSubterms, byte volume, boolean normalized) {
        this.atoms = atoms;
        this.skeleton = skeleton;
        this.hash = hash;
        this.hashSubterms = hashSubterms;
        this.volume = volume;
        this.normalized = normalized;
    }


    public static FastCompound get(Compound c) {
        if (c instanceof FastCompound)
            return ((FastCompound) c);

        ObjectByteHashMap<Term> atoms = new ObjectByteHashMap();
        UncheckedBytes skeleton = new UncheckedBytes(Bytes.wrapForWrite(new byte[128]));

        skeleton.writeUnsignedByte(c.op().ordinal());
        skeleton.writeUnsignedByte(c.subs());
        final byte[] numAtoms = {0};
        ByteFunction0 nextUniqueAtom = () -> numAtoms[0]++;
        c.recurseSubTerms((child, parent) -> {
            skeleton.writeUnsignedByte((byte) child.op().ordinal());
            if (child.op().atomic) {
                int aid = atoms.getIfAbsentPut(child, nextUniqueAtom);
                skeleton.writeUnsignedByte((byte) aid);
            } else {
                skeleton.writeUnsignedByte(child.subs());
                //TODO use last bit of the subs byte to indicate presence or absence of subsequent 'dt' value (32 bits)
            }
            return true;
        }, null);

        //TODO sort atoms to canonicalize its dictionary for sharing with other terms

        byte[][] a = new byte[atoms.size()][];
        for (ObjectBytePair<Term> p : atoms.keyValuesView()) {
            a[p.getTwo()] = IO.termToBytes(p.getOne());
        }

        return new FastCompound(a, skeleton.toByteArray(), c.hashCode(), c.hashCodeSubTerms(), (byte) c.volume(), c.isNormalized());
    }

    public void print() {
//        System.out.println(skeleton.toDebugString());
//        System.out.println(skeleton.toHexString());
//        System.out.println(skeleton.to8bitString());

        System.out.println("skeleton: (" + skeleton.length + " bytes)\t");
        System.out.println(new UncheckedBytes(Bytes.wrapForRead(skeleton)).toHexString());
        System.out.println("atoms:\t");
        for (byte[] b : atoms) {
            System.out.println("\t" + IO.termFromBytes(b) + " (" + b.length + " bytes)");
        }
        System.out.println();
    }

    @Override
    public Op op() {
        return Op.values()[skeleton[0]];
    }

    @Override
    public int subs() {
        return skeleton[1];
    }

    @Override
    public TermContainer subterms() {
        return new SubtermView(this, 0);
    }

    public interface ByteIntPredicate {
        boolean test(byte a, int b);
    }

    public int[] subtermOffsetsAt(int at) {
        int[] b = new int[subtermCountAt(at)];
        subtermsAt(at, (i, c) -> {
            b[i] = c;
            return true;
        });
        return b;
    }

    /**
     * returns byte[] of the offset of each subterm
     */
    public void subtermsAt(int at, ByteIntPredicate each /* subterm #, offset # */) {
        byte[] skeleton = this.skeleton;

        assert (!ov[skeleton[at]].atomic);

        byte[] stack = new byte[MAX_LAYERS];

        byte depth = 0;
        byte subs0 = stack[0] = subtermCountAt(at);

        at += 2; //skip compound header

        for (byte i = 0; i < subs0; ) {
            if (!each.test(i, at))
                break;

            byte op = skeleton[at++]; //get op and skip past it

            if (ov[op].atomic) {
                at++; //skip past atom id
            } else {
                stack[++depth] = skeleton[at++]; //store subcount and skip past it
            }

            if (depth == 0)
                i++;

            if (--stack[depth] == 0)
                depth--; //ascend
        }

    }

    public byte subtermCountAt(int at) {
        return skeleton[at + 1];
    }

    /**
     * seeks and returns the offset of the ith subterm
     */
    public int subtermOffsetAt(int subterm, int at) {

        byte[] skeleton = this.skeleton;

        assert (!ov[skeleton[at]].atomic);

        if (subterm == 0) {
            //quick case
            return at + 2;
        }

        byte[] stack = new byte[MAX_LAYERS];

        byte depth = 0;
        stack[0] = skeleton[at + 1];

        at += 2; //skip compound header

        for (byte i = 0; i < subterm; ) {
            byte op = skeleton[at++]; //get op and skip past it

            if (ov[op].atomic) {
                at++; //skip past atom id
            } else {
                stack[++depth] = skeleton[at++]; //store subcount and skip past it
            }

            if (depth == 0)
                i++;

            if (--stack[depth] == 0)
                depth--; //ascend
        }

        return at;
    }


    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int hashCodeSubTerms() {
        return hashSubterms;
    }

    @Override
    public void setNormalized() {
        normalized = true;
    }

    @Override
    public boolean isNormalized() {
        return normalized;
    }

    @Override
    public int dt() {
        return DTERNAL; //TODO
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }


    /**
     * subterm, or sub-subterm, etc.
     */
    public Term term(int offset) {
        Op opAtSub = ov[skeleton[offset]];
        if (opAtSub.atomic) {
            return IO.termFromBytes(atoms[skeleton[offset + 1]]);
        } else {
            //TODO sub view
            //return opAtSub.the(DTERNAL, subs(subOffset));
            return new GenericCompound(opAtSub, Builder.Subterms.the.apply(new SubtermView(this, offset).theArray()));
        }
    }

    public Term sub(byte i, int containerOffset) {

        int subOffset = subtermOffsetAt(i, containerOffset);
        return term(subtermOffsetAt(i, containerOffset));

    }

    public Term[] subs(int offset) {
        //new SubtermView(this, offset).theArray()
        int[] b = subtermOffsetsAt(offset);
        byte bb = (byte) b.length;
        Term[] t = new Term[bb];
        for (byte i = 0; i < bb; i++) {
            t[i] = term(b[i]);
        }
        return t;
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (that instanceof FastCompound) {
            FastCompound f = (FastCompound)that;
            int aa = atoms.length;
            if (aa == f.atoms.length) {
                if (Arrays.equals(skeleton, f.skeleton)) {
                    for (int i = 0; i < aa; i++)
                        if (!Arrays.equals(atoms[i], f.atoms[i]))
                            return false;
                    return true;
                }
            }
        } else {
            if (Compound.equals(this, (Term) that)) {
                //            if (that instanceof GenericCompound) {
                //                equivalent((GenericCompound)that);
                //            }
                return true;
            }
        }
        return false;
    }

    private static class SubtermView implements TermContainer {
        private final FastCompound c;

        private int offset = -1;
        int subs; //subterms at current offset
        private Op op; //op at current offset
//        private byte[] subOffsets;

        public SubtermView(FastCompound terms, int offset) {
            this.c = terms;
            go(offset);
        }

        @Override
        public boolean equals(Object obj) {
            return
                (this == obj)
                        ||
                (obj instanceof TermContainer) && equalTerms(((TermContainer) obj).theArray());
        }

        @Override
        public int hashCode() {
            return intify((i, t) -> Util.hashCombine(t.hashCode(), i), 1);
            //throw new UnsupportedOperationException();
        }


        public SubtermView go(int offset) {
            if (this.offset == offset)
                return this;

            this.offset = offset;
            op = Op.values()[c.skeleton[offset]];
            if (op.atomic) {
                subs = 0;
            } else {
                subs = c.skeleton[offset + 1];
            }

            //this.subOffsets = null; //TODO avoid recompute offsets if at the same layer

            return this;
        }

//        byte[] subOffsets() {
//            if (subOffsets == null) {
//                subOffsets = c.subOffsets(offset);
//            }
//            return subOffsets;
//        }


        @Override
        public Term sub(int i) {
            assert (i < subs);
            return c.sub((byte) i, offset);
        }

        @Override
        public int subs() {
            return subs;
        }
    }

    /**
     * for use in: Builder.Compound.the
     */
    public static final BiFunction<Op, Term[], Term> FAST_COMPOUND_BUILDER = (op, terms) -> {
        GenericCompound g = new GenericCompound(op, Op.subterms(terms));
        try {

            if (!g.isTemporal())
                return get(g);
            else
                return g;

        } catch (Throwable t) {
            return g;
        }
    };

}
