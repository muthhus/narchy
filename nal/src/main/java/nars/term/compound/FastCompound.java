package nars.term.compound;

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

import static nars.time.Tense.DTERNAL;

/**
 * Annotates a GenericCompound with cached data to accelerate pattern matching
 * TODO not finished yet
 */
public class FastCompound implements Compound {

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


    public byte[] subOffsets(int after) {
        Op[] ov = Op.values();
        byte[] layerLength = new byte[MAX_LAYERS];
        byte[] layerStack = new byte[MAX_LAYERS];


        byte subsAtRoot = layerLength[0] = layerStack[0] = skeleton[after + 1]; //expected # subterms
        byte[] offsets = new byte[subsAtRoot];

        after += 2; //compound header
        int layer = 0;

        for (int i = after; i < skeleton.length; ) {

            if (layer == 0) {
                if (layerStack[0] == 0)
                    break;
                int oi = layerLength[0] - layerStack[0];
                assert(oi >= 0 && oi < offsets.length && offsets[oi] == 0);
                offsets[oi] = (byte) i;
            } else
                layerLength[layer]++;

            /*
            System.out.println(ov[op] + " layer " + layer + " pos=" + i +
                    "\t" + Arrays.toString(layerStack) + "\t" +Arrays.toString(layerLength));
            */

            layerStack[layer]--;


            byte op = skeleton[i];

            boolean descend;
            if (ov[op].atomic) {
                descend = false;
                i += 2; //skip past atom id
            } else {
                byte subSubs = skeleton[i + 1]; //skip past sub count

                assert(layerStack[layer+1] == 0);
                layerStack[layer + 1] = subSubs;
                descend = true;
                i += 2; //compound header
            }


            if (descend)
                layer++;
            else {
                if (layerStack[layer] == 0 && layer > 0) {

                    layer--;
//                    if (--layer < 0)
//                        break;

                }
            }


        }

        //HACK
        if (layer > 0) {
            offsets[offsets.length - 1] = (byte) (skeleton.length - 1);
        }

//        assert(layer == 0); //return to layer 0
        for (int i = 0; i < offsets.length - 1; i++) {
            assert (offsets[i] < offsets[i + 1]); //increasing only
        }

        //System.out.println("offsets: " + Arrays.toString(offsets));

        return offsets;
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

    private static class SubtermView implements TermContainer {
        private final FastCompound c;

        private int offset = -1;
        int subs; //subterms at current offset
        private Op op; //op at current offset
        private byte[] subOffsets;

        public SubtermView(FastCompound terms, int offset) {
            this.c = terms;
            go(offset);
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

            this.subOffsets = null; //TODO avoid recompute offsets if at the same layer

            return this;
        }

        byte[] subOffsets() {
            if (subOffsets == null) {
                subOffsets = c.subOffsets(offset);
            }
            return subOffsets;
        }


        @Override
        public Term sub(int i) {
            assert (i < subs);

            byte[] subOffests = subOffsets();

            int subOffset = subOffsets[i];
            Op opAtSub = Op.values()[c.skeleton[subOffset]];
            if (opAtSub.atomic) {
                return IO.termFromBytes(c.atoms[c.skeleton[subOffset + 1]]);
            } else {
                //TODO sub view
                return opAtSub.the(DTERNAL, new SubtermView(c, subOffset).theArray());
            }
        }

        @Override
        public int subs() {
            return subs;
        }
    }
}
