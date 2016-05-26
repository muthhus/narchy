//package nars.budget;
//
///**
// * TODO a view of a budget existing at an offset in a float[] array
// */
//public abstract class RawBudgetInArray extends Budget {
//
//    final static int fieldSize = 4;
//    private int i;
//    private final float[] array;
//
//    public RawBudgetInArray(float[] array, int offset) {
//        this.array = array;
//        seek(offset);
//    }
//
//    public void seek(int offset) {
//        //int fieldSize = (4 : 3);
//
//        this.i = offset * fieldSize;
//    }
//
//    @Override
//    public final float pri() {
//        return array[i];
//    }
//
//    @Override
//    public final float dur() {
//        return array[i+1];
//    }
//
//    @Override
//    public final float qua() {
//        return array[i+2];
//    }
//
//    @Override
//    public void _setPriority(float p) {
//        array[i] = p;
//    }
//
//    @Override
//    public void _setDurability(float d) {
//        array[i+1] = d;
//    }
//
//    @Override
//    public void _setQuality(float q) {
//        array[i+2] = q;
//    }
//
//    //    @Override
////    public final float[] getTriple() {
////        return Array.
////        return array[i+1];
////    }
//
//}
