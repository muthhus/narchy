//package org.happy.collections.lists.decorators;
//
//import java.util.Collection;
//import java.util.ConcurrentModificationException;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.happy.collections.decorators.FailFastCollection_1x4;
//import org.happy.collections.lists.decorators.iterators.FailFastListIterator_1x4;
//
///**
// * {@link FailFastList_1x4} decorator can be used to decorate any {@link List}
// * to be fail-fast.<br>
// * Fail-Fast strategy in Lists lets any Iterators fail as soon as they realized
// * that structure of {@link List} has been changed since iteration has begun.
// * Structural changes means adding, removing or updating any element from list
// * while one thread is Iterating over that collection. Usually fail-fast
// * behavior is implemented by keeping a modification count and if iteration
// * thread realizes the change in modification count it throws
// * {@link ConcurrentModificationException}.<br>
// *
// * <br>
// * Read more: in java doc of {@link ConcurrentModificationException}<br>
// * you can decoroate your collection as in example below:<br>
// * <dir> List<Integer> list = ...;<br>
// * FailFastList_1x4<Integer> ffList = FailFastList_1x4.of(list);<br>
// * //use ffList here </dir>
// *
// * @author Andreas Hollmann
// *
// * @param <E>
// */
//public class FailFastList_1x4<E> extends FailFastCollection_1x4<E> implements
//		List<E> {
//
//	public static <E> FailFastList_1x4<E> of(List<E> decorateable) {
//		return new FailFastList_1x4<E>(decorateable, new AtomicInteger(0));
//	}
//
//	public static <E> FailFastList_1x4<E> of(List<E> decorateable,
//			AtomicInteger modCount) {
//		return new FailFastList_1x4<E>(decorateable, modCount);
//	}
//
//	protected FailFastList_1x4(List<E> decorateable, AtomicInteger modCount) {
//		super(decorateable, modCount);
//	}
//
//	@Override
//	public List<E> getDecorated() {
//		return (List<E>) super.getDecorated();
//	}
//
//	@Override
//	public boolean addAll(int index, Collection<? extends E> c) {
//		getModCount().incrementAndGet();
//		return this.getDecorated().addAll(index, c);
//	}
//
//	@Override
//	public E get(int index) {
//		return this.getDecorated().get(index);
//	}
//
//	@Override
//	public E set(int index, E element) {
//		getModCount().incrementAndGet();
//		return this.getDecorated().set(index, element);
//	}
//
//	@Override
//	public void add(int index, E element) {
//		getModCount().incrementAndGet();
//		this.getDecorated().add(index, element);
//	}
//
//	@Override
//	public E remove(int index) {
//		getModCount().incrementAndGet();
//		return this.getDecorated().remove(index);
//	}
//
//	@Override
//	public int indexOf(Object o) {
//		return this.getDecorated().indexOf(o);
//	}
//
//	@Override
//	public int lastIndexOf(Object o) {
//		return this.getDecorated().lastIndexOf(o);
//	}
//
//	@Override
//	public ListIterator<E> listIterator() {
//		final ListIterator<E> it = getDecorated().listIterator();
//		return decorateListITerator(it);
//	}
//
//	@Override
//	public ListIterator<E> listIterator(int index) {
//		final ListIterator<E> it = getDecorated().listIterator(index);
//		return decorateListITerator(it);
//	}
//
//	private ListIterator<E> decorateListITerator(final ListIterator<E> it) {
//		final AtomicInteger modCount = this.getModCount();
//		return FailFastListIterator_1x4.of(modCount, it);
//	}
//
//	@Override
//	public List<E> subList(int fromIndex, int toIndex) {
//		final List<E> subList = this.getDecorated().subList(fromIndex, toIndex);
//		return new FailFastList_1x4<E>(subList, this.getModCount());
//	}
//
//}
