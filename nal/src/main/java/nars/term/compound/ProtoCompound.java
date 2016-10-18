package nars.term.compound;

import nars.Op;
import nars.term.Term;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * compound builder
 */
public interface ProtoCompound {

    Op op();

    Term[] terms();

    int dt();




    public class RawProtoCompound implements ProtoCompound {


        @NotNull private final Op op;
        @NotNull private final Term[] args;
        @NotNull private final int dt;

        private final int hash;

        public RawProtoCompound(@NotNull Op op, int dt, @NotNull Term... t) {
            this.op = op;
            this.dt = dt;
            this.args = t;

            this.hash = Util.hashCombine(Util.hashCombine(t), op.ordinal(), dt);
        }

        @Override
        public String toString() {
            return "RawProtoCompound:" +
                    op +
                    '(' + dt +
                    ", " + Arrays.toString(args) +
                    ')';
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RawProtoCompound)) return false;

            RawProtoCompound that = (RawProtoCompound) o;

            if (dt != that.dt) return false;
            if (op != that.op) return false;

            return Util.equals(args, that.args);
        }

        @Override
        public final Op op() {
            return op;
        }

        @Override
        public final Term[] terms() {
            return args;
        }

        @Override
        public final int dt() {
            return dt;
        }
    }







}
