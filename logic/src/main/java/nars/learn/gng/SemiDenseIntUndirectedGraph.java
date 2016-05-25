package nars.learn.gng;

import com.gs.collections.api.block.predicate.primitive.IntPredicate;
import com.gs.collections.api.block.procedure.primitive.ShortIntProcedure;
import com.gs.collections.api.block.procedure.primitive.ShortProcedure;
import com.gs.collections.impl.map.mutable.primitive.ShortIntHashMap;

/**
 * Created by me on 5/25/16.
 */
public class SemiDenseIntUndirectedGraph extends ShortIntHashMap {


    protected final int V; //# of vertices
    protected final MyShortIntHashMap[] adj;  //Array of adjacency lists

    //Constructor with a pre-supplied number of vertices
    SemiDenseIntUndirectedGraph(short V) {
        this.V = V;
        
        adj = new MyShortIntHashMap[V];

        for (int i = 0; i < V; i++) {
            adj[i] = new MyShortIntHashMap(V);
        }

    }

    public void clear() {
        for (MyShortIntHashMap a : adj)
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
        MyShortIntHashMap[] e = this.adj;
        e[first].put(second, value);
        e[second].put(first, value);
    }

    public int getEdge(short first, short second) {
        return adj[first].get(second);
    }

    public void addToEdges(short i, int d) {
        adj[i].addToValues(d); //age by one iteration
    }

    public void addToEdge(short first, short second, int deltaValue) {
        MyShortIntHashMap[] e = this.adj;
        e[first].addToValue(second, deltaValue);
        e[second].addToValue(first, deltaValue);
    }

    public void removeVertex(short v) {
        MyShortIntHashMap[] e = this.adj;
        for (int i = 0, eLength = e.length; i < eLength; i++) {
            MyShortIntHashMap ii = e[i];
            if (i == v) ii.clear();
            else ii.remove(v);
        }
    }

    public void removeEdge(short first, short second) {
        MyShortIntHashMap[] e = this.adj;
        e[first].remove(second);
        e[second].remove(first);
    }

    public void removeEdgeIf(IntPredicate filter) {
        MyShortIntHashMap[] e = this.adj;
        for (MyShortIntHashMap h : e) {
            h.filter(filter);
        }
    }

    public void edgesOf(short vertex, ShortIntProcedure eachKeyValue) {
        adj[vertex].forEachKeyValue(eachKeyValue);
    }
    public void edgesOf(short vertex, ShortProcedure eachKey) {
        adj[vertex].forEachKey(eachKey);
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
