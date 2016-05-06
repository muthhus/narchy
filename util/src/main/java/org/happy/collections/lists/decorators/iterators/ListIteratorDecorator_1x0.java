//package org.happy.collections.lists.decorators.iterators;
//
//import java.util.ListIterator;
//
//
//
///**
// * decorator for decorating custom ListIterators, extend it to implement your
// * own ListITerator decorator
// *
// * @author Eugen Lofing, Andreas Hollmann, Wjatscheslaw Stoljarski
// *
// * @param <E>
// */
//public abstract class ListIteratorDecorator_1x0<E> extends
//		Decorator_1x0Impl<ListIterator<E>> implements ListIterator<E>,
//		 {
//
//	public ListIteratorDecorator_1x0(final ListIterator<E> decorateable) {
//		super(decorateable);
//	}
//
//	@Override
//	public void add(final E e) {
//		getDecorated().add(e);
//	}
//
//	@Override
//	public boolean hasNext() {
//		return getDecorated().hasNext();
//	}
//
//	@Override
//	public boolean hasPrevious() {
//		return getDecorated().hasPrevious();
//	}
//
//	@Override
//	public E next() {
//		return getDecorated().next();
//	}
//
//	@Override
//	public int nextIndex() {
//		return getDecorated().nextIndex();
//	}
//
//	@Override
//	public E previous() {
//		return getDecorated().previous();
//	}
//
//	@Override
//	public int previousIndex() {
//		return getDecorated().previousIndex();
//	}
//
//	@Override
//	public void remove() {
//		getDecorated().remove();
//	}
//
//	@Override
//	public void set(final E e) {
//		getDecorated().set(e);
//	}
//
//}
