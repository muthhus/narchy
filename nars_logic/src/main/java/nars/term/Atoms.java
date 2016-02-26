package nars.term;

import com.googlecode.concurrenttrees.common.*;
import com.googlecode.concurrenttrees.radix.MyConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.NodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.bytearray.*;
import com.googlecode.concurrenttrees.radix.node.concrete.chararray.*;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;
import com.googlecode.concurrenttrees.radix.node.util.NodeCharacterComparator;
import com.googlecode.concurrenttrees.radix.node.util.NodeUtil;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import nars.$;
import nars.Op;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicString;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class Atoms extends MyConcurrentRadixTree<AtomConcept> {

    private static volatile int serial;
    private final Function<Term, Concept> conceptBuilder;

    public Atoms(Function<Term, Concept> conceptBuilder) {
        super(new AtomNodeFactory());
        this.conceptBuilder = conceptBuilder;
    }


    public int getLastSerial() {
        return serial;
    }

    public final AtomConcept resolve(CharSequence id) {
        return getValueForExactKey(id);
    }

    public final AtomConcept resolve(AtomicString a) {
        return getValueForExactKey(a.toString());
    }

    public final AtomConcept resolveOrAdd(String s) {
        return resolveOrAdd($.the(s));
    }

    public final AtomConcept resolveOrAdd(Atom a) {

        return putIfAbsent(a.id, () -> {
            int s = (serial++);
            return (AtomConcept) conceptBuilder.apply(a);
        }); //new AtomicString(++serial));

    }

    public void print(Appendable out) {
        System.out.println("Tree structure:");
        // PrettyPrintable is a non-public API for testing, prints semi-graphical representations of trees...
        PrettyPrinter.prettyPrint((PrettyPrintable) this, out);
    }


    private static final class AtomNodeFactory implements NodeFactory {

        public static final boolean DEBUG = false;

        @Override
        public Node createNode(CharSequence edgeCharacters, Object value, List<Node> childNodes, boolean isRoot) {
            if (DEBUG) {
                assert edgeCharacters != null : "The edgeCharacters argument was null";
                assert !(!isRoot && edgeCharacters.length() == 0) : "Invalid edge characters for non-root node: " + CharSequences.toString(edgeCharacters);
                assert childNodes != null : "The childNodes argument was null";
                NodeUtil.ensureNoDuplicateEdges(childNodes);
            }

            //try {

            if (childNodes.isEmpty()) {
                // Leaf node...
                if (value instanceof VoidValue) {
                    return new ByteArrayNodeLeafVoidValue(edgeCharacters);
                } else if (value != null) {
                    return new ByteArrayNodeLeafWithValue(edgeCharacters, value);
                } else {
                    return new ByteArrayNodeLeafNullValue(edgeCharacters);
                }
            } else {
                // Non-leaf node...
                if (value instanceof VoidValue) {
                    return new ByteArrayNodeNonLeafVoidValue(edgeCharacters, childNodes);
//                    else if (value == null) {
//                        return new ByteArrayNodeNonLeafNullValue(edgeCharacters, childNodes);
                } else {
                    return new ByteArrayNodeDefault(edgeCharacters, value, childNodes);
                }
            }
        }
//            catch (ByteArrayCharSequence.IncompatibleCharacterException e) {
//
//                if (childNodes.isEmpty()) {
//                    // Leaf node...
////                    if (value instanceof VoidValue) {
////                        return new CharArrayNodeLeafVoidValue(edgeCharacters);
////                    } else if (value != null) {
//                        return new CharArrayNodeLeafWithValue(edgeCharacters, value);
////                    } else {
////                        return new CharArrayNodeLeafNullValue(edgeCharacters);
////                    }
//                } else {
//                    // Non-leaf node...
//                    if (value instanceof VoidValue) {
//                        return new CharArrayNodeNonLeafVoidValue(edgeCharacters, childNodes);
////                    }
////                    else if (value == null) {
////                        return new CharArrayNodeNonLeafNullValue(edgeCharacters, childNodes);
//                    } else {
//                        return new CharArrayNodeDefault(edgeCharacters, value, childNodes);
//                    }
//                }
//            }
    }
}

//    final class InternedAtom extends Atomic implements Node /* implements Concept */ {
//
//        private final int id;
//
//        InternedAtom(int id) {
//            this.id = id;
//
//        }
//
//        @Override
//        public Character getIncomingEdgeFirstCharacter() {
//            return null;
//        }
//
//        @Override
//        public CharSequence getIncomingEdge() {
//            return null;
//        }
//
//        @Override
//        public Object getValue() {
//            return this;
//        }
//
//        @Override
//        public Node getOutgoingEdge(Character edgeFirstCharacter) {
//            return null;
//        }
//
//        @Override
//        public void updateOutgoingEdge(Node childNode) {
//
//        }
//
//        @Override
//        public List<Node> getOutgoingEdges() {
//            return null;
//        }
//
//        @Override
//        public
//        @Nullable
//        String toString() {
//            return Integer.toString(id);
//        }
//
//        @Override
//        public
//        @Nullable
//        Op op() {
//            return null;
//        }
//
//        @Override
//        public int complexity() {
//            return 0;
//        }
//
//        @Override
//        public int varIndep() {
//            return 0;
//        }
//
//        @Override
//        public int varDep() {
//            return 0;
//        }
//
//        @Override
//        public int varQuery() {
//            return 0;
//        }
//
//        @Override
//        public int varPattern() {
//            return 0;
//        }
//
//        @Override
//        public int vars() {
//            return 0;
//        }
//
//        @Override
//        public int compareTo(Object o) {
//            return 0;
//        }
//    }

