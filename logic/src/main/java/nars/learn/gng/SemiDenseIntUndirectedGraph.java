package nars.learn.gng;

import com.gs.collections.api.block.predicate.primitive.IntPredicate;
import com.gs.collections.api.block.procedure.primitive.IntProcedure;
import com.gs.collections.api.block.procedure.primitive.ShortIntProcedure;
import com.gs.collections.impl.list.mutable.primitive.ShortArrayList;
import com.gs.collections.impl.map.mutable.primitive.ShortIntHashMap;
import com.gs.collections.impl.set.mutable.primitive.IntHashSet;
import ognl.IntHashMap;

/**
 * Created by me on 5/25/16.
 */
public class SemiDenseIntUndirectedGraph {


    protected final int V; //# of vertices
    protected final ShortIntHashMap[] adj;  //Array of adjacency lists

    //Constructor with a pre-supplied number of vertices
    SemiDenseIntUndirectedGraph(short V) {
        this.V = V;
        
        adj = new ShortIntHashMap[V];

        for (int i = 0; i < V; i++) {
            adj[i] = new ShortIntHashMap(V);
        }

    }

    public void clear() {
        for (ShortIntHashMap a : adj)
            a.clear();
    }

    //Number of vertices
    public int V() {
        return V;
    }

//    //Number of edges
//    public int E() {
//        return E;
//    }

    //Connect two vertices (first to second)
    public void setEdge(short first, short second, int value) {
        ShortIntHashMap[] e = this.adj;
        e[first].put(second, value);
        e[second].put(first, value);
    }

    public int getEdge(short first, short second) {
        return adj[first].get(second);
    }

    public void addEdge(short first, short second, int deltaValue) {
        ShortIntHashMap[] e = this.adj;
        e[first].addToValue(second, deltaValue);
        e[second].addToValue(first, deltaValue);
    }

    public void removeVertex(short v) {
        ShortIntHashMap[] e = this.adj;
        for (int i = 0, eLength = e.length; i < eLength; i++) {
            ShortIntHashMap ii = e[i];
            if (i == v) ii.clear();
            else ii.remove(v);
        }
    }

    public void removeEdge(short first, short second) {
        ShortIntHashMap[] e = this.adj;
        e[first].remove(second);
        e[second].remove(first);
    }

    public void removeEdgeIf(IntPredicate filter) {
        ShortIntHashMap[] e = this.adj;
        ShortArrayList toRemove = new ShortArrayList();
        for (ShortIntHashMap h : e) {
            h.forEachKeyValue((k,v) -> {
                if (filter.accept(v))
                    toRemove.add(k);
            });
            if (!toRemove.isEmpty()) {
                toRemove.forEach(h::removeKey);
                toRemove.clear();
            }
        }
    }

    public void edgesOf(short vertex, ShortIntProcedure each) {
        adj[vertex].forEachKeyValue(each);
    }

//    public String toString() {
//        String s = V + " vertices, " + E + " edges\n";
//
//        for (int v = 0; v < V; v++) {
//            s += v + ": ";
////            this.edgesOf(v, e -> {
////                s += e + " ";
////            });
//            s += "\n";
//        }
//        return s;
//    }


    //Computer the degree of vertex V
    public int degree(int v) {
        return adj[v].size();
    }


//    //Find the vertex with the largest degree
//    public static int maxDegree(Graph G) {
//        int max = 0;
//        for (int v = 0; v < G.V(); v++) {
//            int temp = degree(G, v);
//            if (temp > max) {
//                max = temp;
//            }
//        }
//        return max;
//    }
//
//    //Compute the average degree
//    public static double averageDegree(Graph G) {
//        return (2.0 * G.E() / G.V());
//    }

}
