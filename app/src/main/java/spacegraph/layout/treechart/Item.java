//package spacegraph.layout.treechart;
//
//import com.gs.collections.impl.factory.SortedSets;
//
//import java.util.SortedSet;
//import java.util.TreeSet;
//
///**
// *
// * @author Tadas Subonis <tadas.subonis@gmail.com>
// */
//public interface Item extends Comparable<Item> {
//
//    Object key();
//
//    double weight();
//
//    String label();
//
//    boolean isContainer();
//
////    SortedSet<Item> content();
//
//    static DefaultItem get(Object o, double size) {
//        return new DefaultItem(o, size);
//    }
//
//    static DefaultItem get(Object o, double size, Object firstChild, double firstChildSize) {
//        DefaultItem i = new DefaultItem(o, size);
//        i.add(firstChild,firstChildSize);
//        return i;
//    }
//
//    class DefaultItem implements Item {
//        private double size;
//        private final Object id;
//
//        public DefaultItem(Object o, double size) {
//            id = o;
//            this.size = size;
//        }
//
//        @Override
//        public Object key() {
//            return id;
//        }
//
//        @Override
//        public double weight() {
//            return size;
//        }
//
//        @Override
//        public String label() {
//            return id.toString();
//        }
//
//        TreeSet<Item> children = null;
//
//        public void add(Object childID, double childsize) {
//            if (children == null) {
//                children = new TreeSet();
//            }
//            children.add(Item.get(childID, childsize));
//            size += childsize;
//        }
//
//        @Override
//        public boolean isContainer() {
//            return children!=null;
//        }
//
//        @Override
//        public SortedSet<Item> content() {
//            if (children == null)
//                return SortedSets.mutable.empty();
//            else
//                return children;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            return compareTo((Item)o)==0;
//        }
//
//        @Override
//        public int compareTo(Item o) {
//            return Double.compare(weight(), o.weight());
//        }
//    }
//}
