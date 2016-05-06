//package org.happy.collections.lists.decorators.iterators;
//
//import java.util.ListIterator;
//
///**
// * this decorator exchanges all previous and next methods, thus if you want to
// * get next() you get previous element and if you check for hasNext() you check
// * for hasPrevies element
// *
// * @author Eugen Lofing, Andreas Hollmann, Wjatscheslaw Stoljarski
// *
// * @param <E>
// */
//public class InversedListIterator_1x0<E> extends ListIteratorDecorator_1x0<E> {
//
//	/**
//	 * decorates the list-iterator to be inverse
//	 *
//	 * @param <T>
//	 *            type of elements
//	 * @param it
//	 *            iterator, which should be decorated
//	 * @return decorator for the list-iterator
//	 */
//	public static <T> InversedListIterator_1x0<T> of(final ListIterator<T> it) {
//		return new InversedListIterator_1x0<T>(it);
//	}
//
//	public InversedListIterator_1x0(final ListIterator<E> decorateable) {
//		super(decorateable);
//	}
//
//	@Override
//	public boolean hasNext() {
//		return getDecorated().hasPrevious();
//	}
//
//	@Override
//	public boolean hasPrevious() {
//		return getDecorated().hasNext();
//	}
//
//	@Override
//	public E next() {
//		return getDecorated().previous();
//
//	}
//
//	@Override
//	public int nextIndex() {
//		return getDecorated().previousIndex();
//
//	}
//
//	@Override
//	public E previous() {
//		return getDecorated().next();
//	}
//
//	@Override
//	public int previousIndex() {
//		return getDecorated().nextIndex();
//	}
//
//}
