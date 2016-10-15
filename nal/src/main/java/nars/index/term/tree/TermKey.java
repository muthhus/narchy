package nars.index.term.tree;

import io.airlift.compress.lz4.Lz4Compressor;
import io.airlift.compress.lz4.Lz4Decompressor;
import io.airlift.compress.lz4.Lz4RawCompressor;
import io.airlift.compress.lz4.Lz4RawDecompressor;
import io.airlift.compress.snappy.SnappyCompressor;
import io.airlift.compress.snappy.SnappyRawCompressor;
import nars.IO;
import nars.Op;
import nars.Symbols;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.util.DynByteSeq;
import nars.util.data.rope.StringHack;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static nars.IO.SPECIAL_OP;
import static nars.IO.writeEvidence;
import static nars.IO.writeUTFWithoutLength;
import static nars.util.data.rope.StringHack.bytes;

/**
 * TODO lazily compute
 */
public class TermKey extends DynByteSeq {

    private final static ThreadLocal<Lz4Compressor> compressor = ThreadLocal.withInitial(()->new Lz4Compressor());
    final static Lz4Decompressor decompressor = new Lz4Decompressor();
    private final static float minCompressionRatio = 0.9f;

    public TermKey(@NotNull Term conceptualizable) {
        super(conceptualizable.volume() * 4 + 64 /* ESTIMATE */);
        try {
            writeTermSeq(this, conceptualizable, false);

            int before = length();
            if (compress()) {
                int after = length();
                System.out.println(conceptualizable + "\t" + before + " -> " + after + "\t" + new String(array()));
            }

            //this.writeByte(0); //null terminator, signifying end-of-term
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TermKey(@NotNull Task task) {
        super(task.volume() * 4 + 64 /* ESTIMATE */);
        try {
            //Term, Occurrence, Truth, Evidence
//            writeTermSeq(this, task.term(), true);
//            writeLong(task.occurrence());
//            IO.writeTruth(this, task);
//            IO.writeEvidence(this, task.evidence());

            //writeUTFWithoutLength(this, task.term().toString());
            writeCompoundSeq(this, task.term(), true);

            char punc = task.punc();
            this.writeByte(punc);


            writeLong(task.occurrence());

            if ((punc == Symbols.BELIEF) || (punc == Symbols.GOAL)) {
                writeInt(task.truth().hashCode());
            }

            writeEvidence(this, task.evidence());

            compress();

            //System.out.println(task + " uncompressed=" + uncLength + " compressed=" + compressedLength);

            {
//            byte[] reUncompressed = new byte[uncLength];
//            int unC2 = decompressor.decompress(compressed,0, length(), reUncompressed, 0, reUncompressed.length );
//            reUncompressed = Arrays.copyOfRange(reUncompressed, 0, unC2);
//            byte[] original = Arrays.copyOfRange(uncompressed, 0, uncLength);
//            if (!Arrays.equals(original, reUncompressed)) {
//                System.err.println(task + " compression failure:\n\t" + new String(original) + "\n\t" + new String(reUncompressed));
//            }
            }

            //writeByte(0); //null terminator, signifying end-of-term

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean compress() {
        int uncLength = length();

        //byte[] original = Arrays.copyOfRange(bytes, 0, len);
        byte[] uncompressed = this.bytes;
        byte[] compressed = new byte[Lz4RawCompressor.maxCompressedLength(uncLength)]; //this.bytes; //new byte[1024];
        //public int compress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset, int maxOutputLength) {


        int compressedLength = compressor.get()
                .compress(uncompressed, 0, uncLength, compressed, 0, compressed.length);
        if (compressedLength <= (int)(uncLength * minCompressionRatio)) {
            this.bytes = compressed;
            this.position = compressedLength;
            return true;
        }
        return false;
    }


    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }


    public static void writeTermSeq(@NotNull DataOutput out, @NotNull Term term, boolean includeTemporal) throws IOException {


        if (term instanceof Atomic) {
            if (IO.isSpecial(term)) {
                out.writeByte(SPECIAL_OP);
            }
            //out.writeUTF(term.toString());
            //IO.writeUTFWithoutLength(out, term.toString());

            writeStringBytes(out, term);

            //out.writeByte(term.op().ordinal()); //put operator last
        } else {
            writeCompoundSeq(out, (Compound) term, includeTemporal);
        }
    }

    public static void writeStringBytes(DataOutput out, Object o) throws IOException {
        out.write(bytes(o.toString()));
    }

    public static void writeStringBytes(DataOutput out, String s) throws IOException {
        out.write(bytes(s));
    }


    public static void writeCompoundSeq(@NotNull DataOutput out, @NotNull Compound c, boolean includeTemporal) throws IOException {

        out.writeByte('(');
        writeTermContainerSeq(out, c.subterms(), includeTemporal);
        out.writeByte(')');

        @NotNull Op o = c.op();
        out.writeByte(o.ordinal()); //put operator last
        if (o.image) {
            out.writeByte((byte) c.dt());
        } else if (includeTemporal && o.temporal) {
            out.writeInt(c.dt());
        }

    }


    static void writeTermContainerSeq(@NotNull DataOutput out, @NotNull TermContainer c, boolean includeTemporal) throws IOException {

        int siz = c.size();
        for (int i = 0; i < siz; i++) {
            writeTermSeq(out, c.term(i), includeTemporal);
            if (i < siz-1)
                out.writeByte(',');
        }

    }


}
