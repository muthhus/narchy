/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp;

import org.oakgp.node.Node;

import java.util.Arrays;
import java.util.List;

import static org.oakgp.util.Utils.copyOf;

/**
 * Represents the arguments of a function.
 * <p>
 * Immutable.
 */
public final class Arguments {
    private final Node[] args;
    private final int hashCode;

    /**
     * @see #createArguments(Node...)
     */
    public Arguments(Node[] args) {
        this.args = args;
        this.hashCode = Arrays.hashCode(args);
    }

    public Arguments(List<? extends Node> args) {
        this(args.toArray(new Node[args.size()]));
    }

    /**
     * Returns the {@code Node} at the specified position in this {@code Arguments}.
     *
     * @param index index of the element to return
     * @return the {@code Node} at the specified position in this {@code Arguments}
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= getArgCount()</tt>)
     */
    public Node arg(int index) {
        return args[index];
    }

    /**
     * Returns the first argument in this {@code Arguments}.
     */
    public Node firstArg() {
        return args[0];
    }

    /**
     * Returns the second argument in this {@code Arguments}.
     */
    public Node secondArg() {
        return args[1];
    }

    /**
     * Returns the third argument in this {@code Arguments}.
     */
    public Node thirdArg() {
        return args[2];
    }

    /**
     * Returns the number of elements in this {@code Arguments}.
     *
     * @return the number of elements in this {@code Arguments}
     */
    public int args() {
        return args.length;
    }

    /**
     * Returns a new {@code Arguments} resulting from replacing the existing {@code Node} at position {@code index} with {@code replacement}.
     *
     * @param index       the index of the {@code Node} that needs to be replaced.
     * @param replacement the new {@code Node} that needs to be store.
     * @return A new {@code Arguments} derived from this {@code Arguments} by replacing the element at position {@code index} with {@code replacement}.
     */
    public Arguments replaceAt(int index, Node replacement) {
        Node[] clone = copyOf(args);
        clone[index] = replacement;
        return new Arguments(clone);
    }

    /**
     * Returns a new {@code Arguments} resulting from switching the node located at index {@code index1} with the node located at index {@code index2}.
     *
     * @param index1 the index in this {@code Arguments} of the first {@code Node} to be swapped.
     * @param index2 the index in this {@code Arguments} of the second {@code Node} to be swapped.
     * @return A new {@code Arguments} resulting from switching the node located at index {@code index1} with the node located at index {@code index2}.
     */
    public Arguments swap(int index1, int index2) {
        Node[] clone = copyOf(args);
        clone[index1] = args[index2];
        clone[index2] = args[index1];
        return new Arguments(clone);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        return this ==o || (o instanceof Arguments && Arrays.equals(this.args, ((Arguments) o).args));
    }

    @Override
    public String toString() {
        return Arrays.toString(args);
    }
}
