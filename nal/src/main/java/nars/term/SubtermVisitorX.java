package nars.term;

import java.util.function.BiConsumer;

/**
 * Extends SubtermVisitor with awareness of the superterm being traversed
 */
@FunctionalInterface
public interface SubtermVisitorX extends BiConsumer<Term,Compound>
{

}
