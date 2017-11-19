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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Node<N, E> {

    private final N data;
    private final Collection<Edge<N, E>> inEdges;
    private final Collection<Edge<N, E>> outEdges;
    private boolean visited;
    private boolean active;
    private boolean reachable;

    protected Node(N data) {
        this.data = data;
        inEdges = new HashSet<>();
        outEdges = new HashSet<>();
    }

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

    protected void setReachable() { reachable = true; }
    protected void setUnreachable() { reachable = false; }

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
            return (int) in().count();
        } else {
            return (int) in().filter(e -> e.from!=this).count();
        }
    }

    public int outs() {
        return (int) out().count();
    }


    protected boolean inAdd(Edge<N, E> e) {
        return inEdges.add(e);
    }

    protected boolean outAdd(Edge<N, E> e) {
        return outEdges.add(e);
    }

    protected void inRemove(Edge<N, E> e) {
        //assert inEdges.contains(e);
        inEdges.remove(e);
    }

    protected void outRemove(Edge<N, E> e) {
        //assert outEdges.contains(e);
        outEdges.remove(e);
    }

    public Stream<Edge<N, E>> in() {
        return (inEdges.stream());
    }

    public Stream<Edge<N, E>> out() {
        return (outEdges.stream());
    }

    public Stream<N> successors() {
        return out().map(e -> e.to.data);
    }
    public Stream<N> predecessors() {
        return in().map(e -> e.from.data);
    }

    public N get() {
        return data;
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

    public boolean activate() {
        if (!isVisited()) {
            setVisited(true);
            setActive(true);
            return true;
        }
        return false;
    }
}
