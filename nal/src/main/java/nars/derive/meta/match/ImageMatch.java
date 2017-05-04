package nars.derive.meta.match;

import nars.Op;
import nars.index.term.AppendProtoCompound;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.Imdex;

/**
 * the indicated relation term is inserted
 * at the index location of the original image
 * used to make products from image subterms
 */
public enum ImageMatch {
    ;

    /**
     * @param t            the subvector of image terms collected in the ellipsis match, to be expanded with relationTerm inserted in the correct imdex position
     * @param relationTerm the term which replaces the _ in the formed term vector
     * @param y            the (concrete) image being matched against the pattern
     * @return
     */
    @NotNull
    public static Term put(@NotNull Term raw, @NotNull Term relationTerm, @NotNull Compound y) {

        Term[] t = EllipsisMatch.expand(raw);

        //not sure why this works

        int l = t.length;
        int yOffset = y.size() - l; //where 't' begins in Y

        int j = 0;
        Term[] t2;

        int dt = y.indexOf(relationTerm);
        int ydt = y.dt();
        if (dt == -1) {
            //insert the relation term
            t2 = new Term[l + 1];
            int yOff = ydt - yOffset; //where to expect _ in t
            for (Term x : t) {
                if (j == yOff)
                    t2[j++] = relationTerm;
                t2[j++] = x;
            }
            if (j < l + 1)
                t2[j] = relationTerm; //it replaces the final position
        } else {
            //TODO make this work to share code:
            //t2 = getRemaining(y, 0, dt);

            //insert the relation term where found
            int m = l ;
            t2 = new Term[m];
            int numPreMatched = y.size() - t.length;
            for (int i = 0, tLength = t.length; i < tLength && j < m; ) {
                t2[j++] = t[i++];
                if ((i >= (ydt - numPreMatched)) && (i == (dt - 1)))
                    t2[j++] = Imdex;
            }

        }


        return new ImageEllipsisMatch(t2);
    }

    static final class ImageEllipsisMatch extends EllipsisMatch {

        public ImageEllipsisMatch(Term[] t2) {
            super(t2);
        }

        @Override
        public void expand(@NotNull Op op, @NotNull AppendProtoCompound target) {
            if (op.image) {
                //expand normally because imdexes will be included in the expansion
                super.expand(op, target);
            } else {
                //exclude any imdexes because the target is not an image
                for (Term t : terms) {
                    if (!t.equals(Imdex)) {
                        target.add(t);
                    }
                }
            }
        }

    }

    @NotNull
    public static ImageEllipsisMatch getRemaining(@NotNull Compound y, int from) {
        return getRemaining(y, from, y.dt());
    }
    @NotNull
    public static ImageEllipsisMatch getRemaining(@NotNull Compound y, int from, int dt) {

        int l = y.size();
        //insert the relation term where found
        int m = l - from + 1;
        Term[] t2 = new Term[m];
        int j = 0;
        for (int i = from; i < l; ) {
            if ((i >= (y.dt() - from)) && (i-from == (dt - 1)))
                t2[j++] = Imdex;
            t2[j++] = y.sub(i);
            i++;
        }
        if (m > j)
            t2[j] = Imdex;

        return new ImageEllipsisMatch(t2);
    }


    @NotNull
    public static Term take(@NotNull Term m, int imageIndex) {
        //mask the relation term
        Term[] t = EllipsisMatch.expand(m);
        t[imageIndex] = Imdex;
        return EllipsisMatch.match(t); //needs rehashed; this will be redone with a visitor that applies the Imdex mask on first and only needed construction
    }

//    @Override
//    public boolean applyTo(Subst substitution, Collection<Term> target, boolean fullMatch) {
//        Term relation = substitution.getXY(to);
//        if (relation == null) {
//            if (fullMatch)
//                return false;
//            else
//                relation = to;
//        }
//
//        Term[] t = origin.terms();
//        Compound origin = this.origin;
//
//        int ri = origin.relation();
//
//        int ot = t.length;
//        //int j = (ri == ot) ? 1 : 0; //shift
//        int j = 0;
//        int r = ri - 1;
//        for (int i = 0; i < ot; i++) {
//            target.add( i==r ? relation : t[j]);
//            j++;
//        }
////        System.arraycopy(ot, 0, t, 0, r);
////        t[r] = relation;
////        System.arraycopy(ot, r, t, r+1, ot.length - r);
//
//        return true;
//    }

}
