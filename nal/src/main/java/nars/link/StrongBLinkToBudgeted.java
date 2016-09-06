package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public final class StrongBLinkToBudgeted<B extends Budgeted> extends StrongBLink<B> {


    public StrongBLinkToBudgeted(@NotNull B id, Budgeted b) {
        super(id, b);

    }

//    @NotNull
//    @Override
//    public final B get() {
//        B x = id;
//        if (x.isDeleted()) {
//            delete();
//        }
//        return x;
//    }

}
