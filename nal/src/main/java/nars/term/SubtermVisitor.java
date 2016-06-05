package nars.term;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * TODO make a lighter-weight version which supplies only the 't' argument
 */
@FunctionalInterface
public interface SubtermVisitor extends Consumer<Term>
{

}
