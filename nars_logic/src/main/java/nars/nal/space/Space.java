package nars.nal.space;

import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import nars.Op;
import nars.term.TermVector;
import nars.term.Termed;
import nars.term.compound.Compound;
import nars.term.compound.GenericCompound;
import nars.util.data.Util;

import java.io.IOException;
import java.util.Objects;


/** linear combination of a vector of terms, each representing a basis vectors of the vector space they form */
public class Space extends GenericCompound {

    /** matches each of the subterms in their order */
    final FloatArrayList vector;
    final int hash2;

    public Space(TermVector subterms) {
        //this(subterms, FloatArrayList.newWithNValues((int)subterms.size(), (float)Float.NaN) /* blank */);
        this(subterms, (FloatArrayList) null);
    }

    public Space(TermVector subterms, float... f) {
        this(subterms, new FloatArrayList(f));
    }

    @Override public Compound anonymous() {
        if (vector == null) return super.anonymous();
        else return new Space(subterms());
    }


    public Space(TermVector subterms, FloatArrayList vector) {
        super(Op.SPACE, -1, subterms);
        this.vector = vector;
        if (vector!=null) {
            if (vector.size()!=subterms.size()) {
                throw new RuntimeException("invalid dimensions: " + subterms + " with " + vector.size());
            }
            this.hash2 = Util.hashCombine(super.hashCode(), vector.hashCode());
        } else {
            this.hash2 = super.hashCode();
        }

    }

    @Override
    public boolean equals(Object that) {
        return (that instanceof Space) && super.equals(that) &&
            Objects.equals(vector,  ((Space)((Termed)that).term()).vector);
    }

    @Override
    public int hashCode() {
        return hash2;
    }

    @Override
    public int compareTo(Object that) {
        int c = super.compareTo(that);
        if (c == 0) {
            FloatArrayList a = vector;
            FloatArrayList b = ((Space)that).vector;
            if (a == b) return 0;
            //TODO maybe hash2 can be compared first for fast

            int n = a.size();
            for (int i = 0; i < n; i++) {
                int d = Float.compare(a.get(i), b.get(i));
                if (d != 0) return d;
            }
            return 0;
        }
        return c;
    }

    @Override
    public void appendArg(Appendable p, boolean pretty, int i) throws IOException {
        term(i).append(p, pretty);

        FloatArrayList vv = this.vector;
        if (vv !=null) {
            float v = vv.get(i);
            if (Float.isFinite(v)) {
                p.append('*').append(Float.toString(v));
            }
        }
    }

}
