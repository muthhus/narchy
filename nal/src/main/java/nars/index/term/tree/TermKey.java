package nars.index.term.tree;

import nars.IO;
import nars.Op;
import nars.Symbols;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.util.ByteBufferlet;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import static nars.IO.SPECIAL_OP;
import static nars.IO.writeUTFWithoutLength;

/**
 * TODO lazily compute
 */
public class TermKey extends ByteBufferlet {

    public TermKey(@NotNull Term conceptualizable) {
        super(conceptualizable.volume() * 8 /* ESTIMATE */);
        try {
            writeTermSeq(this, conceptualizable, false);
            this.writeByte(0); //null terminator, signifying end-of-term
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TermKey(@NotNull Task task) {
        super(task.volume() * 8 + 32 /* ESTIMATE */);
        try {
            //Term, Occurrence, Truth, Evidence
//            writeTermSeq(this, task.term(), true);
//            writeLong(task.occurrence());
//            IO.writeTruth(this, task);
//            IO.writeEvidence(this, task.evidence());

            //writeUTFWithoutLength(this, task.term().toString());
            IO.writeCompound(this, task.term());

            char punc = task.punc();
            this.writeByte(punc);

            for (long x : task.evidence())
                writeUTFWithoutLength(this, Long.toString(x, 36));

            writeUTFWithoutLength(this, Long.toString(task.occurrence(), 36));

            if ((punc == Symbols.BELIEF) && (punc == Symbols.GOAL)) {
                writeUTFWithoutLength(this, Integer.toString(task.truth().hashCode(), 36));
            }

            writeByte(0); //null terminator, signifying end-of-term

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            IO.writeUTFWithoutLength(out, term.toString());
            //out.writeByte(term.op().ordinal()); //put operator last
        } else {
            writeCompoundSeq(out, (Compound) term, includeTemporal);
        }
    }

    public static void writeCompoundSeq(@NotNull DataOutput out, @NotNull Compound c, boolean includeTemporal) throws IOException {

        writeTermContainerSeq(out, c.subterms(), includeTemporal);

        @NotNull Op o = c.op();
        out.writeByte(o.ordinal()); //put operator last
        if (o.image || (includeTemporal && o.temporal))
            out.writeByte(c.dt());

    }


    static void writeTermContainerSeq(@NotNull DataOutput out, @NotNull TermContainer c, boolean includeTemporal) throws IOException {
        int siz = c.size();

        for (int i = 0; i < siz; i++) {
            writeTermSeq(out, c.term(i), includeTemporal);
//            if (i < siz-1)
//                out.writeByte(',');
        }

    }


}
