//package astar.impl;
//
//import astar.Find;
//
//import java.util.Comparator;
//
//public class OpenSetHash implements IOpenSet {
//    private final HashPriorityQueue<Integer, Find> hashQ;
//    private final Comparator<Find> comp;
//
//    public OpenSetHash(Comparator<Find> comp) {
//        this.hashQ = new HashPriorityQueue<Integer, Find>(comp);
//        this.comp = comp;
//    }
//
//    @Override
//    public void add(Find node) {
//        this.hashQ.add(node.keyCode(), node);
//    }
//
//    @Override
//    public void remove(Find node) {
//        this.hashQ.remove(node.keyCode(), node);
//    }
//
//    @Override
//    public Find poll() {
//        return this.hashQ.poll();
//    }
//
//    @Override
//    public Find getNode(Find node) {
//        return this.hashQ.get(node.keyCode());
//    }
//
//    @Override
//    public int size() {
//        return this.hashQ.size();
//    }
//
//    @Override
//    public String toString() {
//        return this.hashQ.getTreeMap().keySet().toString();
//    }
//
//}
