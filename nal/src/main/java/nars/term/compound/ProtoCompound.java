package nars.term.compound;

import nars.Op;
import nars.term.Term;
import nars.util.Util;

import static nars.nal.Tense.DTERNAL;

/**
 * compound builder
 */
public interface ProtoCompound {

    Op o();

    Term[] terms();

    int dt();



    public static ProtoCompound the(Op o, Term[] args) {
        return the(o, DTERNAL, args);
    }

    public static ProtoCompound the(Op o, int dt, Term[] args) {
        return new RawProtoCompound(o, dt, args);
    }


    public class RawProtoCompound implements ProtoCompound {


        private final Op op;
        private final Term[] args;
        private final int dt;

        private final int hash;

        protected RawProtoCompound(Op op, int dt, Term... t) {
            this.op = op;
            this.dt = dt;
            this.args = t;

            this.hash = Util.hashCombine(Util.hashCombine(t), op.ordinal(), dt);
        }

        @Override
        public final Op o() {
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
