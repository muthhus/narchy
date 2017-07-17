package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.index.term.TermIndex;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    private @Nullable Term xterm;

    @Nullable private Term result;


    public SubUnify(TermIndex index, Op type, Random r, int ttl) {
        super(index, type, r, Param.UnificationStackMax, ttl);
    }

    public SubUnify(@NotNull Unify parent, @Nullable Op type) {
        super(parent.terms, type, parent.random, parent.versioning);
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
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
        //apply the match before the xy/yx mapping gets reverted after leaving the termutator
        //int start = target!=null ? target.now() : -1;

//        boolean fail = false;
        if (xterm != null) {

//            if (target != null) {
//                if (!target.put(this)) {
//                    fail = true;
//                }
//            }


                result = transform(xterm);
//                if (result == null)
//                    fail = true;

        } else {
            //return false; //??
        }


//        if (fail) {
//            //s.revert(start);
//            //return s.live(); //try again if ttl > 0
//        } else {
//            //return false; //success, done
//
//            return true; //HACK done after once, but TODO try multiple
//        }

        return false;
    }

    public void tryMatch(@NotNull Term x, @NotNull Term y) {
        this.xterm = null;
        this.result = null;
        unify(x, y, true);
    }

    @Nullable
    public Term tryMatch(@Nullable Term xterm, @NotNull Term x, @NotNull Term y) {
        this.xterm = xterm;
        this.result = null;
        unify(x, y, true);
        return result;
    }

}
