//package org.happy.collections.lists.decorators.iterators;
//
//import java.util.ListIterator;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.happy.collections.decorators.iterators.FailFastIterator_1x4;
//
//public class FailFastListIterator_1x4<E> extends FailFastIterator_1x4<E>
//		implements ListIterator<E> {
//
//	public static <E> FailFastListIterator_1x4<E> of(
//			final AtomicInteger modCount, final ListIterator<E> decorated) {
//		return new FailFastListIterator_1x4<>(modCount, decorated);
//	}
//
//	protected FailFastListIterator_1x4(final AtomicInteger modCount,
//			final ListIterator<E> decorated) {
//		super(modCount, decorated);
//	}
//
//	@Override
//	public ListIterator<E> getDecorated() {
//		return (ListIterator<E>) super.getDecorated();
//	}
//
//	@Override
//	public boolean hasPrevious() {
//		return getDecorated().hasPrevious();
//	}
//
//	@Override
//	public E previous() {
//		validateModification();
//		return getDecorated().previous();
//	}
//
//	@Override
//	public int nextIndex() {
//		return getDecorated().nextIndex();
//	}
//
//	@Override
//	public int previousIndex() {
//		return getDecorated().previousIndex();
//	}
//
//	@Override
//	public void set(E e) {
//		incrementModification();
//		getDecorated().set(e);
//	}
//
//	@Override
//	public void add(E e) {
//		incrementModification();
//		getDecorated().add(e);
//	}
//
//}
