package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.data.byt.DynByteSeq;
import nars.*;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.visit.SubtermVisitorXY;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static nars.$.$;
import static nars.time.Tense.DTERNAL;

/**
 * compound which is stored simply as a byte[] of its serialization
 * which is optimized for streaming and lazy/de-duplicated batched construction
 * purposes.
 * <p>
 * see IO.writeTerm()
 */
public class SerialCompound extends DynByteSeq implements Compound {

    final byte volume;

    public SerialCompound(Compound c) {
        this(c.op(), c.dt(), c.terms());
    }

    public SerialCompound(Op op, int dt, Term[] subterms) {
        super(subterms.length * 4 /* estimate */);

        writeByte(op.ordinal());
        writeByte(subterms.length);

        int v = 1;

        try {

            for (int i = 0; i < subterms.length; i++) {
                Term x = subterms[i];
                IO.writeTerm(this, x);
                v += x.volume();
            }

            IO.writeCompoundSuffix(this, dt, op);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assert(v < Param.COMPOUND_VOLUME_MAX);
        this.volume = (byte) v;

    }

    public Compound build() {
        return build($.terms);
    }

    public Compound build(TermIndex index) {
        return (Compound) IO.termFromBytes(bytes, index);
    }

    @Override
    public final @NotNull Op op() {
        return Op.values()[bytes[0]];
    }

    @Override
    public int size() {
        return bytes[1];
    }

    @Override
    public int volume() {
        return volume;
    }

    @Override
    public @NotNull TermContainer subterms() {
        return build().subterms(); //HACK just a temporary way of accessing them
    }

//    static class SerialTermVector implements TermContainer {
//
//    }

    @Override
    public boolean isNormalized() {
        return false;
    }

    @Override
    public int dt() {
        //last two bytes
        Op o = op();
        if (o.temporal) {
            int p = this.position;
            byte[] bb = Arrays.copyOfRange(bytes, p - 4, p);
            return Ints.fromByteArray(bb);
        } else if (o.image) {
            return bytes[position-1];
        } else {
            return DTERNAL;
        }
    }


}

//    public SerialCompound(@NotNull Op op, int dt, @NotNull TermContainer subs) {
//        super(op, dt, subs);
//
//        uniqueSubs = new ObjectByteHashMap<>(volume());
//        recurseTerms((x) -> uniqueSubs.getIfAbsentPut(x, ()->(byte)uniqueSubs.size()));
//
//        new SubtermVisitorXY(term()) {
//
//            @NotNull
//            @Override
//            public SubtermVisitorXY.Next accept(int subterm, Compound superterm, int depth) {
//                System.out.println(superterm + "(" + subterm + "): " + superterm.term(subterm) + ", depth=" + depth);
//                return Next.Next;
//            }
//        };
//
//        uniqueSubIndex = new Term[uniqueSubs.size()];
//        uniqueSubs.forEachKeyValue((k,v) -> {
//            uniqueSubIndex[v] = k;
//        });
//
//    }
//
//    public void print() {
//        System.out.println(toString() + "\n\tuniqueSubs=" + uniqueSubs);
//    }
