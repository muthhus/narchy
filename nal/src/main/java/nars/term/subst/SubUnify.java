package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    private final Unify parent;
    private @Nullable Term transformed;

    @Nullable private Term result;


    public SubUnify(@NotNull Unify parent, @Nullable Op type) {
        super(parent.terms, type, parent.random, Param.UnificationStackMax, 0);
        this.parent = parent;
    }

    @Override
    public void onDeath() {
        parent.onDeath();
    }

    @Override
    public void unify(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.ttl = parent.ttl; //load
        super.unify(x, y, finish);
        parent.ttl = ttl; //restore
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


    @Nullable
    public Term tryMatch(@Nullable Term transformed, @NotNull Term x, @NotNull Term y) {
        this.transformed = transformed;
        this.result = null;
        unify(x, y, true);

        return result;
    }

}
