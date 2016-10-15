package nars.index.term.tree;

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

import static nars.IO.SPECIAL_OP;
import static nars.IO.writeEvidence;
import static nars.IO.writeUTFWithoutLength;
import static nars.util.data.rope.StringHack.bytes;

/**
 * TODO lazily compute
 */
public class TermKey extends DynByteSeq {

    public TermKey(@NotNull Term conceptualizable) {
        super(conceptualizable.volume() * 4 + 4 /* ESTIMATE */);
        try {
            writeTermSeq(this, conceptualizable, false);
            //this.writeByte(0); //null terminator, signifying end-of-term
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TermKey(@NotNull Task task) {
        super(task.volume() * 4 + 32 /* ESTIMATE */);
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

            //writeByte(0); //null terminator, signifying end-of-term

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
