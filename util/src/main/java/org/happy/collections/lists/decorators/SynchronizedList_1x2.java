//package org.happy.collections.lists.decorators;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//
//import org.happy.collections.decorators.SynchronizedCollection_1x2;
//import org.happy.commons.patterns.Lockable_1x0;
//
///**
// * list decorator wich makes the decorated list multi-thread safe. It differs
// * from Collection.synchronizedList() method in the ability to share the lock
// * object. The lock object can be seted by the programmer. Iterators must be
// * manually synchronized:
// *
// * synchronized (coll) { Iterator it = coll.iterator(); // do stuff with
// * iterator }
// *
// * @author Andreas Hollmann
// * @param <E>
// */
//public class SynchronizedList_1x2<E> extends ListDecorator_1x0<E, List<E>>
//		implements Lockable_1x0 {
//
//	/**
//	 * adapted synchronized collection decorator
//	 */
//	private SynchronizedCollection_1x2<E> synchColl;
//
//	public SynchronizedList_1x2(final List<E> list) {
//		this(list, true);
//	}
//
//	/**
//	 * constructor
//	 *
//	 * @param list
//	 *            list which should be decorated
//	 * @param decorateIterators
//	 *            if false then the decorators will no be decorated
//	 */
//	public SynchronizedList_1x2(final List<E> list,
//			final boolean decorateIterators) {
//		super(list, decorateIterators);
//		this.synchColl = new SynchronizedCollection_1x2<E>(list, this,
//				decorateIterators);
//	}
//
//	@Override
//	protected ListIterator<E> listIteratorImpl(final int index) {
//		final ListIterator<E> it = getDecorated().listIterator(index);
//		return new ListIterator<E>() {
//			@Override
//			public boolean hasNext() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.hasNext();
//				}
//			}
//
//			@Override
//			public E next() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.next();
//				}
//			}
//
//			@Override
//			public boolean hasPrevious() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.hasPrevious();
//				}
//			}
//
//			@Override
//			public E previous() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.previous();
//				}
//			}
//
//			@Override
//			public int nextIndex() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.nextIndex();
//				}
//			}
//
//			@Override
//			public int previousIndex() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					return it.previousIndex();
//				}
//			}
//
//			@Override
//			public void remove() {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					it.remove();
//				}
//			}
//
//			@Override
//			public void set(final E e) {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					it.set(e);
//				}
//			}
//
//			@Override
//			public void add(final E e) {
//				synchronized (SynchronizedList_1x2.this.synchColl
//						.getLockObject()) {
//					it.add(e);
//				}
//			}
//
//		};
//	}
//
//	@Override
//	protected Iterator<E> iteratorImpl() {
//		return synchColl.iterator();
//	}
//
//	@Override
//	public void setDecorated(final List<E> decorated) {
//		synchColl.setDecorated(decorated);
//		super.setDecorated(decorated);
//	}
//
//	@Override
//	public Object getLockObject() {
//		return synchColl.getLockObject();
//	}
//
//	@Override
//	public void setLockObject(final Object lockObject) {
//		synchColl.setLockObject(lockObject);
//	}
//
//	@Override
//	public boolean add(final E e) {
//		return synchColl.add(e);
//	}
//
//	@Override
//	public boolean addAll(final Collection<? extends E> c) {
//		return synchColl.addAll(c);
//	}
//
//	@Override
//	public boolean isDecorateIterators() {
//		return synchColl.isDecorateIterators();
//	}
//
//	@Override
//	public void clear() {
//		synchColl.clear();
//	}
//
//	@Override
//	public void setDecorateIterators(final boolean decorateIterators) {
//		synchColl.setDecorateIterators(decorateIterators);
//	}
//
//	@Override
//	public boolean contains(final Object o) {
//		return synchColl.contains(o);
//	}
//
//	@Override
//	public boolean containsAll(final Collection<?> c) {
//		return synchColl.containsAll(c);
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return synchColl.isEmpty();
//	}
//
//	@Override
//	public Iterator<E> iterator() {
//		return synchColl.iterator();
//	}
//
//	@Override
//	public boolean remove(final Object o) {
//		return synchColl.remove(o);
//	}
//
//	@Override
//	public boolean removeAll(final Collection<?> c) {
//		return synchColl.removeAll(c);
//	}
//
//	@Override
//	public boolean retainAll(final Collection<?> c) {
//		return synchColl.retainAll(c);
//	}
//
//	@Override
//	public int size() {
//		return synchColl.size();
//	}
//
//	@Override
//	public Object[] toArray() {
//		return synchColl.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(final T[] a) {
//		return synchColl.toArray(a);
//	}
//
//	@Override
//	public void add(final int arg0, final E arg1) {
//		synchronized (this.getLockObject()) {
//			getDecorated().add(arg0, arg1);
//		}
//	}
//
//	@Override
//	public boolean addAll(final int arg0, final Collection<? extends E> arg1) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().addAll(arg0, arg1);
//		}
//	}
//
//	@Override
//	public E get(final int arg0) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().get(arg0);
//		}
//	}
//
//	@Override
//	public int indexOf(final Object arg0) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().indexOf(arg0);
//		}
//	}
//
//	@Override
//	public int lastIndexOf(final Object arg0) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().lastIndexOf(arg0);
//		}
//	}
//
//	@Override
//	public ListIterator<E> listIterator() {
//		synchronized (this.getLockObject()) {
//			return getDecorated().listIterator();
//		}
//	}
//
//	@Override
//	public ListIterator<E> listIterator(final int index) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().listIterator(index);
//		}
//	}
//
//	@Override
//	public E remove(final int index) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().remove(index);
//		}
//	}
//
//	@Override
//	public E set(final int index, final E element) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().set(index, element);
//		}
//	}
//
//	@Override
//	public List<E> subList(final int fromIndex, final int toIndex) {
//		synchronized (this.getLockObject()) {
//			return getDecorated().subList(fromIndex, toIndex);
//		}
//	}
//
//}
