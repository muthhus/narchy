/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package jcog.data.graph.hgraph;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * @author Thomas Wuerthinger
 */
public class HashGraph<N, E> {

    protected Map<N, Node<N, E>> nodes;
    //protected Map<E, Edge<N, E>> edges;

    public HashGraph() {
        this(new HashMap<>(), new HashMap());
    }

    public HashGraph(Map<N, Node<N, E>> nodes, Map<E, Edge<N, E>> edges) {
        this.nodes = nodes;
        //this.edges = edges;
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        nodes().forEach((node) -> {
            node.print(out);
        });
    }

    public Node<N, E> add(N key) {
        final boolean[] created = {false};
        Node<N, E> r = nodes.computeIfAbsent(key, (x) -> {
            created[0] = true;
            return newNode(x);
        });
        if (created[0]) {
            onAdd(r);
        }
        return r;
    }

    protected Node<N, E> newNode(N data) {
        return new Node<>(data);
    }

    protected void onAdd(Node<N, E> r) {

    }

    public boolean edgeAdd(Node<N, E> from, E data, Node<N, E> to) {
        Edge<N, E> ee = new Edge<>(from, to, data);
        if (from.outAdd(ee)) {
            boolean a = to.inAdd(ee);
            assert (a);
            return true;
        }
        return false;
    }

    public Node<N, E> node(Object key) {
        return nodes.get(key);
    }

    public Collection<Node<N, E>> nodes() {
        return nodes.values();
    }

    public void edgeRemove(Edge<N, E> e) {
        e.from.outRemove(e);
        e.to.inRemove(e);
    }

    @FunctionalInterface
    public interface DFSTraverser<N, E> {

        /**
         * path should not be modified by callee. it is left exposed for performance
         * path boolean is true = OUT, false = IN
         */
        boolean visit(Node<N, E> n, FasterList<BooleanObjectPair<Edge<N, E>>> path);

        default boolean visitAgain(Edge<N, E> e, boolean outOrIn) {
            return true;
        }

        default boolean visit(Edge<N, E> e, boolean outOrIn) {
            return true;
        }

        default Stream<Edge<N, E>> edges(Node<N, E> n, boolean outOrIn) {
            return outOrIn ? n.out() : n.in();
        }

    }

    @FunctionalInterface
    public interface BFSTraverser<N, E> {

        void visitNode(Node<N, E> n, int depth);
    }

    /**
     * TODO return zero-copy Iterable
     */
    public List<Node<N, E>> nodesWithIns(int x) {
        return nodesWithIns(x, true);
    }

    /**
     * TODO return zero-copy Iterable
     */
    public List<Node<N, E>> nodesWithIns(int x, boolean includeSelfLoops) {

        List<Node<N, E>> result = new ArrayList<>();
        for (Node<N, E> n : nodes()) {
            if (n.ins(includeSelfLoops) == x) {
                result.add(n);
            }
        }

        return result;

    }

    private void markReachable(Node<N, E> startingNode) {
        ArrayList<Node<N, E>> arr = new ArrayList<>();
        arr.add(startingNode);
        nodes().forEach(Node::setUnreachable);
        traverseDFSNodes(arr, false, true, (node, path) -> {
            node.setReachable();
            return true;
        });
    }

    public void traverseBFS(Node<N, E> startingNode, BFSTraverser tv, boolean longestPath) {

        if (longestPath) {
            markReachable(startingNode);
        }

        for (Node<N, E> n : nodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        Queue<Node<N, E>> queue = new LinkedList<>();
        queue.add(startingNode);
        startingNode.setVisited(true);
        int layer = 0;
        Node<N, E> lastOfLayer = startingNode;
        final Node[] lastAdded = {null};

        while (!queue.isEmpty()) {

            Node<N, E> current = queue.poll();
            tv.visitNode(current, layer);
            current.setActive(false);


            current.out().forEach(e -> {
                if (!e.to.isVisited()) {

                    final boolean[] allow = {true};
                    if (longestPath) {
                        e.to.in().allMatch(pred -> {
                            Node<N, E> p = pred.to;
                            if ((!p.isVisited() || p.isActive()) && p.isReachable()) {
                                allow[0] = false;
                                return false;
                            }
                            return true;
                        });
                    }

                    if (allow[0]) {
                        queue.offer(e.to);
                        lastAdded[0] = e.to;
                        e.to.setVisited(true);
                        e.to.setActive(true);
                    }
                }
            });

            if (current == lastOfLayer && !queue.isEmpty()) {
                lastOfLayer = lastAdded[0];
                layer++;
            }
        }
    }

    public void traverseDFS(DFSTraverser<N, E> tv, Edge<N, E> e, boolean in, boolean out, FasterList<BooleanObjectPair<Edge<N, E>>> path, Edge<N, E> neEdge) {
        traverseDFSNodes(nodes(), in, out, tv);
    }

    public void traverseDFS(N startingNode, boolean in, boolean out, DFSTraverser<N, E> tv) {
        traverseDFS(List.of(startingNode), in, out, tv);
    }

    public void traverseDFS(Iterable<N> startingNodes, boolean in, boolean out, DFSTraverser<N, E> tv) {
        traverseDFSNodes(Iterables.transform(startingNodes, this::add), in, out, tv);
    }

    public synchronized void traverseDFSNodes(Iterable<Node<N, E>> startingNodes, boolean in, boolean out, DFSTraverser<N, E> tv) {

        for (Node<N, E> n : nodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        boolean result = false;
        FasterList<BooleanObjectPair<Edge<N, E>>> path = new FasterList();
        for (Node n : startingNodes) {
            if (!traverse(tv, in, out, n, path))
                break;
            assert (path.isEmpty());
        }
    }

    private boolean traverse(DFSTraverser tv, boolean in, boolean out, Node<N, E> n, FasterList<BooleanObjectPair<Edge<N, E>>> path) {

        assert (in || out);

        if (n.activate()) {

            if (!tv.visit(n, path))
                return false;

            if (out && !traverseDFS(tv, tv.edges(n, true), true, in, out, path))
                return false;

            if (in && !traverseDFS(tv, tv.edges(n, false), false, in, out, path))
                return false;

            n.setActive(false);
        }

        return true;

    }

    /**
     * TODO allow the traverser to decide to cut the search entirely, or just proceed no further past the current node
     */
    private boolean traverseDFS(DFSTraverser tv, Stream<Edge<N, E>> edges, boolean outOrIn, boolean in, boolean out, FasterList<BooleanObjectPair<Edge<N, E>>> path) {

        return edges.allMatch(e -> {

            Node<N, E> next = outOrIn ? e.to : e.from;
            if (next.isVisited())
                return true;

            path.add(pair(outOrIn, e));

            boolean kontinue;
            if (next.isActive()) {

                kontinue = tv.visitAgain(e, outOrIn);

                //but dont go there, it would be a cycle

            } else {

                if (kontinue = tv.visit(e, outOrIn)) {

                    kontinue = traverse(tv, in, out, next, path);

                }

            }

            path.removeLast();

            return kontinue;
        });
    }

    public boolean hasCycles() {

        for (Node<N, E> n : nodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        for (Node<N, E> n : nodes()) {
            if (checkCycles(n))
                return true;
        }
        return false;
    }

    private boolean checkCycles(Node<N, E> n) {

        if (n.isActive()) {
            return true;
        }

        if (!n.isVisited()) {

            n.setVisited(true);
            n.setActive(true);

            if (n.out().anyMatch(succ -> checkCycles(succ.to)))
                return true;

            n.setActive(false);

        }

        return false;
    }

    public Stream<Edge<N,E>> edges() {
        throw new TODO();
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N, E> n : nodes()) {
            s.append(n.toString()).append("\n");
        }

        s.append("Edges: ");

        edges().forEach(e -> {
            s.append(e.toString()).append("\n");
        });

        return s.toString();
    }
}
