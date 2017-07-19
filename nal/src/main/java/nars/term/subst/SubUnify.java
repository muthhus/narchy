package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    private @Nullable Term transformed;

    @Nullable private Term result;


    public SubUnify(TermIndex index, Op type, Random r, int ttl) {
        super(index, type, r, Param.UnificationStackMax, ttl);
    }

    public SubUnify(@NotNull Unify parent, @Nullable Op type, int ttl) {
        super(parent.terms, type, parent.random, parent.versioning);
        setTTL(ttl);
    }

//    @Override
//    public @Nullable Term resolve(@NotNull Term x) {
//
//        assert(target!=null);
//
//        Term thisResolved = super.resolve(x);
//        if (thisResolved == null)
//            return target.resolve(x);
//        else
//            return thisResolved;
//    }
//
//    @Override
//    public @Nullable Term xy(@NotNull Term x) {
//
//        return resolve(x);//new MapSubst(xy).transform(x, index);
////        assert(target!=null);
////
////        Term thisResolved = super.xy(x);
////        if (thisResolved == null)
////            return target.xy(x);
////        else
////            return thisResolved;
//    }

    /**
     * terminate after the first match
     */
    @Override
    public void onMatch() {

        if (transformed != null) {
            Term result = transform(transformed);
            if (result instanceof Compound) {
                this.result = result;
                stop(); //done
            }
        }
    }

    public void tryMatch(@NotNull Term x, @NotNull Term y) {
        this.transformed = null;
        this.result = null;
        unify(x, y, true);
    }

    @Nullable
    public Term tryMatch(@Nullable Term transformed, @NotNull Term x, @NotNull Term y) {
        this.transformed = transformed;
        this.result = null;
        unify(x, y, true);
        return result;
    }

}
