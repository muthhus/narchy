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
import jcog.list.FasterList;

import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Node<N, E> {

    private N data;
    private final List<Edge<N, E>> inEdges;
    private final List<Edge<N, E>> outEdges;
    private boolean visited;
    private boolean active;
    private boolean reachable;

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    protected boolean isVisited() {
        return visited;
    }

    protected void setVisited(boolean b) {
        visited = b;
    }

    protected boolean isReachable() {
        return reachable;
    }

    protected void setReachable(boolean b) {
        reachable = b;
    }

    protected boolean isActive() {
        return active;
    }

    protected void setActive(boolean b) {
        active = b;
    }

    public int ins() {
        return ins(true);
    }

    public int ins(boolean countSelfLoops) {
        if (countSelfLoops) {
            return inEdges.size();
        } else {
            int cnt = 0;
            for (Edge<N, E> e : inEdges) {
                if (e.from != this) {
                    cnt++;
                }
            }
            return cnt;
        }
    }

    public int outs() {
        return outEdges.size();
    }

    protected Node(N data) {
        set(data);
        inEdges = new FasterList<>();
        outEdges = new FasterList<>();
    }

    protected void inAdd(Edge<N, E> e) {
        inEdges.add(e);
    }

    protected void outAdd(Edge<N, E> e) {
        outEdges.add(e);
    }

    protected void inRemove(Edge<N, E> e) {
        //assert inEdges.contains(e);
        inEdges.remove(e);
    }

    protected void outRemove(Edge<N, E> e) {
        //assert outEdges.contains(e);
        outEdges.remove(e);
    }

    public List<Edge<N, E>> in() {
        return (inEdges);
    }

    public List<Edge<N, E>> out() {
        return (outEdges);
    }

    public Iterable<N> successors() {
        return Iterables.transform(out(), e -> e.to.data);
    }
    public Iterable<N> predecessors() {
        return Iterables.transform(in(), e -> e.from.data);
    }

    public N get() {
        return data;
    }

    public void set(N d) {
        data = d;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public void print(PrintStream out) {
        out.println(data);
        out().forEach(e -> {
           out.println("\t" + e);
        });
    }
}
