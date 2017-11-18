//package astar.impl;
//
//import astar.Find;
//
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//
//public class ClosedSetHash implements IClosedSet {
//    private final HashMap<Integer, Find> hashMap;
//    private final Comparator<Find> comp;
//
//    public ClosedSetHash(Comparator<Find> comp) {
//        this.hashMap = new HashMap<Integer, Find>();
//        this.comp = comp;
//
//    }
//
//    @Override
//    public boolean contains(Find node) {
//        return this.hashMap.containsKey(node.keyCode());
//    }
//
//    @Override
//    public void add(Find node) {
//        this.hashMap.put(node.keyCode(), node);
//    }
//
//    @Override
//    public Find min() {
//        return Collections.min(hashMap.values(), comp);
//    }
//
//}
