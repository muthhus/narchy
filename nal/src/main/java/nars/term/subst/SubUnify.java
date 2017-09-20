package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    @NotNull private final Unify parent;
    private @Nullable Term transformed;

    @Nullable
    private Term result;


    public SubUnify(@NotNull Unify parent, @Nullable Op type, int ttl) {
        super(type, parent.random, Param.UnificationStackMax, ttl);
        this.parent = parent;
    }

    @Override
    public boolean unify(@NotNull Term x, @NotNull Term y, boolean finish) {
        return super.unify(x, y, finish);
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
     *
     */
    @Override
    public void tryMatch() {

        if (transformed != null) {
            Term result = transformed.transform(this);//transform(transformed);
            if (result!=null && !(result instanceof Bool)) {
                if (!result.equals(transformed)) {
                    int before = parent.now();
                    if (
                        //xy.forEachVersioned(parent.xy::tryPut)
                    xy.forEachVersioned((x,y)->{
                        parent.xy.tryPut(x, y);
                        //parent.xy.tryPut(y,x);
                        return true;
                    })
                    ) {
                        this.result = result;
                        parent.addTTL(stop()); //stop and refund parent
                    } else {
                        parent.revert(before); //continue trying
                    }
                }
            }
        }
    }


    @Nullable
    public Term tryMatch(@Nullable Term transformed, @NotNull Term x, @NotNull Term y) {
        this.transformed = transformed;
        this.result = null;
        unify(x, y, true);

        return result;
    }

}
