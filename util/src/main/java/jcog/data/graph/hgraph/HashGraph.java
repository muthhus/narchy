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

import java.io.PrintStream;
import java.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class HashGraph<N, E> {

    public Map<Object, Node<N, E>> nodes;
    protected Map<Object, Edge<N, E>> edges;

    public HashGraph() {
        this(new HashMap<>(), new HashMap());
    }

    public HashGraph(Map<Object, Node<N, E>> nodes, Map<Object, Edge<N, E>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        nodes().forEach((node)->{
            node.print(out);
        });
    }

    public Node<N, E> nodeAdd(N data) {
        return nodeAdd(data, data);
    }

    public Node<N, E> nodeAdd(N data, Object key) {
        int s = nodes.size();
        Node<N, E> r = nodes.computeIfAbsent(key, (x) -> new Node<>(data));
        if (nodes.size() > s) {
            onAdd(r);
        }
        return r;
    }

    protected void onAdd(Node<N, E> r) {

    }

    public Edge<N, E> edgeAdd(Node<N, E> source, E data, Node<N, E> dest) {
        return edgeAdd(source, data, data, dest);
    }

    public Edge<N, E> edgeAdd(Node<N, E> source, Object key, E data, Node<N, E> dest) {
        return edges.computeIfAbsent(key, (x) -> {
            Edge<N, E> e = new Edge<>(source, dest, data);
            source.outAdd(e);
            dest.inAdd(e);
            return e;
        });
    }

    public Node<N, E> node(Object key) {
        return nodes.get(key);
    }


    public Edge<N, E> edge(Object key) {
        return edges.get(key);
    }

    public Collection<Edge<N, E>> edges() {
        return edges.values();
    }

    public Collection<Node<N, E>> nodes() {
        return nodes.values();
    }

    public void edgeRemove(Edge<N, E> e, Object key) {
        assert key == null || edges.containsKey(key);
        if (key != null) {
            edges.remove(key);
        }
        e.from.outRemove(e);
        e.to.inRemove(e);
    }

    public class DFSTraversalVisitor {

        public void visitNode(Node<N, E> n) {
        }

        public boolean visitEdge(Edge<N, E> e, boolean backEdge) {
            return true;
        }
    }

    public class BFSTraversalVisitor {

        public void visitNode(Node<N, E> n, int depth) {
        }
    }

    public List<Node<N, E>> getNodesWithInDegree(int x) {
        return getNodesWithInDegree(x, true);
    }

    public List<Node<N, E>> getNodesWithInDegree(int x, boolean countSelfLoops) {

        List<Node<N, E>> result = new ArrayList<>();
        for (Node<N, E> n : nodes()) {
            if (n.ins(countSelfLoops) == x) {
                result.add(n);
            }
        }

        return result;

    }

    private void markReachable(Node<N, E> startingNode) {
        ArrayList<Node<N, E>> arr = new ArrayList<>();
        arr.add(startingNode);
        for (Node<N, E> n : nodes()) {
            n.setReachable(false);
        }
        traverseDFS(arr, new DFSTraversalVisitor() {

            @Override
            public void visitNode(Node<N, E> n) {
                n.setReachable(true);
            }
        });
    }

    public void traverseBFS(Node<N, E> startingNode, BFSTraversalVisitor tv, boolean longestPath) {

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
        Node<N, E> lastAdded = null;

        while (!queue.isEmpty()) {

            Node<N, E> current = queue.poll();
            tv.visitNode(current, layer);
            current.setActive(false);


            for (Edge<N, E> e : current.out()) {
                if (!e.to.isVisited()) {

                    boolean allow = true;
                    if (longestPath) {
                        for (Edge<N, E> pred : e.to.in()) {
                            Node<N, E> p = pred.to;
                            if ((!p.isVisited() || p.isActive()) && p.isReachable()) {
                                allow = false;
                                break;
                            }
                        }
                    }

                    if (allow) {
                        queue.offer(e.to);
                        lastAdded = e.to;
                        e.to.setVisited(true);
                        e.to.setActive(true);
                    }
                }
            }

            if (current == lastOfLayer && !queue.isEmpty()) {
                lastOfLayer = lastAdded;
                layer++;
            }
        }
    }

    public void traverseDFS(DFSTraversalVisitor tv) {
        traverseDFS(nodes(), tv);
    }

    public void traverseDFS(Collection<Node<N, E>> startingNodes, DFSTraversalVisitor tv) {

        for (Node<N, E> n : nodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        boolean result = false;
        for (Node<N, E> n : startingNodes) {
            traverse(tv, n);
        }
    }

    private void traverse(DFSTraversalVisitor tv, Node<N, E> n) {

        if (!n.isVisited()) {
            n.setVisited(true);
            n.setActive(true);
            tv.visitNode(n);

            for (Edge<N, E> e : n.out()) {

                Node<N, E> next = e.to;
                if (next.isActive()) {
                    tv.visitEdge(e, true);
                } else {
                    if (tv.visitEdge(e, false)) {
                        traverse(tv, next);
                    }
                }
            }

            n.setActive(false);
        }

    }

    public boolean hasCycles() {

        for (Node<N, E> n : nodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        boolean result = false;
        for (Node<N, E> n : nodes()) {
            result |= checkCycles(n);
            if (result) {
                break;
            }
        }
        return result;
    }

    private boolean checkCycles(Node<N, E> n) {

        if (n.isActive()) {
            return true;
        }

        if (!n.isVisited()) {

            n.setVisited(true);
            n.setActive(true);

            for (Edge<N, E> succ : n.out()) {
                Node<N, E> p = succ.to;
                if (checkCycles(p)) {
                    return true;
                }
            }

            n.setActive(false);

        }

        return false;
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N, E> n : nodes()) {
            s.append(n.toString());
            s.append("\n");
        }

        s.append("Edges: ");

        for (Edge<N, E> e : edges()) {
            s.append(e.toString());
            s.append("\n");
        }

        return s.toString();
    }
}
