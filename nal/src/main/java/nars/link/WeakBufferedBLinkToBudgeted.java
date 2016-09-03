//package nars.link;
//
//import nars.budget.Budgeted;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Adds an additional condition that deletes the link if the referenced
// * Budgeted is deleted
// */
//public final class WeakBufferedBLinkToBudgeted<B extends Budgeted> extends WeakBufferedBLink<B> {
//
//    public WeakBufferedBLinkToBudgeted(@NotNull B id, @NotNull Budgeted b, float scal) {
//        super(id, b, scal);
//    }
//
//    @Override
//    public void commit() {
//        B val = id.get();
//        if (val == null || val.isDeleted()) delete();
//        else super.commit();
//    }
//}
