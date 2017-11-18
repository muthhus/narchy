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

/**
 *
 * @author Thomas Wuerthinger
 */
public class Edge<N, E> {

    private E data;
    public final Node<N, E> from;
    public final Node<N, E> to;

    protected Edge(Node<N, E> from, Node<N, E> to, E data) {
        set(data);
        this.from = from;
        this.to = to;
        assert from != null;
        assert to != null;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    public E get() {
        return data;
    }

    public void set(E e) {
        data = e;
    }

    public boolean isSelfLoop() {
        return from == to;
    }

//    public void reverse() {
//
//        // Remove from current source / dest
//        from.outRemove(this);
//        to.inRemove(this);
//
//        Node<N, E> tmp = from;
//        from = to;
//        to = tmp;
//
//        // Add to new source / dest
//        from.outAdd(this);
//        to.inAdd(this);
//    }

    @Override
    public String toString() {
        return from + " => " + data + " => " + to;
    }
}
