//package org.happy.collections.lists.decorators;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Set;
//
//import org.happy.commons.patterns.observer.event.ActionEventBefore_1x0;
//import org.happy.commons.patterns.observer.listener.ActionListener_1x0;
//
///**
// * decorates the list to be a set, this class use set to proof for duplicates in
// * the list. This class can be used as an ArraySet if you decorate an ArrayList.
// * This class requires more memory because of using additional Set for checking
// * duplicates. If you don't want to use the set you can create the decorator
// * without set, use for that the defined constructor. in such case you don't
// * misuse memory and call directtly the contains method of the List. Ensure that
// * this method is fast enough. Also ensure by using the set your list don't use
// * any null-object or use the set which support null objects.
// *
// * @author Andreas Hollmann
// *
// * @param <E>
// *            type of the element
// */
//public class SetList_1x0<E> extends ListDecorator_1x0<E, List<E>> implements
//		Set<E> {
//
//	/**
//	 * decorates a list with SetList decorator
//	 *
//	 * @param <T>
//	 *            generic type of the list
//	 * @param list
//	 *            the list which should be decorated
//	 * @param isSet
//	 *            if true then the list behaves like a set
//	 * @return new created decorator
//	 */
//	public static <T> SetList_1x0<T> of(List<T> list, boolean isSet) {
//		return new SetList_1x0<T>(list, isSet);
//	}
//
//	/**
//	 * if true then the set will be used for proofing of duplicates, else the
//	 * contains method of the lsit will be called!
//	 */
//	private boolean isSet;
//	private EventList_1x0<E> eventList;
//
//	/**
//	 * constructor
//	 *
//	 * @param list
//	 *            list which should be decorated
//	 * @param isSet
//	 *            if true the decorator use the set to proof the existense of
//	 *            the element
//	 */
//	public SetList_1x0(List<E> list, boolean isSet) {
//		super(list, true);
//		if (!list.isEmpty() && isSet) {
//			removeDuplicates(list);
//		}
//		this.isSet = isSet;
//		this.eventList = new EventList_1x0<E>(list);
//		// register event
//		this.eventList.getOnBeforeAddEvent().add(
//				new ActionListener_1x0<ActionEventBefore_1x0<E>>() {
//					@Override
//					public void actionPerformedImpl(
//							ActionEventBefore_1x0<E> event) {
//						if (SetList_1x0.this.isSet) {
//							E elem = event.getData();
//							if (SetList_1x0.this.eventList.contains(elem)) {
//								event.setCanceled(true);// cancel the ading of
//														// the element
//							}
//						}
//					}
//				});
//		super.setDecorated(eventList);
//	}
//
//	private void removeDuplicates(List<E> list) {
//		List<E> set = new ArrayList<E>();// set don't work at this state
//		Iterator<E> it = list.iterator();
//		while (it.hasNext()) {
//			E e = it.next();
//			if (!set.contains(e)) {
//				set.add(e);
//			} else {
//				// remove duplicate from the list
//				it.remove();
//			}
//		}
//	}
//
//	public boolean isSet() {
//		return isSet;
//	}
//
//	public void setSet(boolean isSet) {
//		boolean changed = false;
//		if (isSet != this.isSet) {
//			changed = true;
//		}
//		this.isSet = isSet;
//		if (changed) {
//			// remove duplicates
//			this.removeDuplicates(this.eventList);
//		}
//	}
//
//	@Override
//	public boolean add(E o) {
//		return eventList.add(o);
//	}
//
//	@Override
//	public void add(int index, E o) {
//		eventList.add(index, o);
//	}
//
//	@Override
//	public boolean addAll(Collection<? extends E> c) {
//		return eventList.addAll(c);
//	}
//
//	@Override
//	public boolean addAll(int index, Collection<? extends E> c) {
//		return eventList.addAll(index, c);
//	}
//
//	@Override
//	public void clear() {
//		eventList.clear();
//	}
//
//	@Override
//	public boolean contains(Object o) {
//		return eventList.contains(o);
//	}
//
//	@Override
//	public boolean containsAll(Collection<?> c) {
//		return eventList.containsAll(c);
//	}
//
//	@Override
//	public E get(int index) {
//		return eventList.get(index);
//	}
//
//	@Override
//	public List<E> getDecorated() {
//		return (List<E>) eventList.getDecorated();
//	}
//
//	@Override
//	public Float getVersion() {
//		return eventList.getVersion();
//	}
//
//	@Override
//	public int indexOf(Object o) {
//		return eventList.indexOf(o);
//	}
//
//	@Override
//	public boolean isDecorateIterators() {
//		return eventList.isDecorateIterators();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return eventList.isEmpty();
//	}
//
//	@Override
//	public Iterator<E> iterator() {
//		return eventList.iterator();
//	}
//
//	@Override
//	public int lastIndexOf(Object index) {
//		return eventList.lastIndexOf(index);
//	}
//
//	@Override
//	public ListIterator<E> listIterator() {
//		return eventList.listIterator();
//	}
//
//	@Override
//	public ListIterator<E> listIterator(E elem) {
//		return eventList.listIterator(elem);
//	}
//
//	@Override
//	public ListIterator<E> listIterator(int index) {
//		return eventList.listIterator(index);
//	}
//
//	@Override
//	public E remove(int index) {
//		return eventList.remove(index);
//	}
//
//	@Override
//	public boolean remove(Object o) {
//		return eventList.remove(o);
//	}
//
//	@Override
//	public boolean removeAll(Collection<?> c) {
//		return eventList.removeAll(c);
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> c) {
//		return eventList.retainAll(c);
//	}
//
//	@Override
//	public E set(int index, E o) {
//		return eventList.set(index, o);
//	}
//
//	public void setDecorated(Collection<E> decorated) {
//		eventList.setDecorated(decorated);
//	}
//
//	@Override
//	public void setDecorateIterators(boolean decorateIterators) {
//		eventList.setDecorateIterators(decorateIterators);
//	}
//
//	@Override
//	public int size() {
//		return eventList.size();
//	}
//
//	@Override
//	public List<E> subList(int fromIndex, int toIndex) {
//		return eventList.subList(fromIndex, toIndex);
//	}
//
//	@Override
//	public Object[] toArray() {
//		return eventList.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(T[] a) {
//		return eventList.toArray(a);
//	}
//
//	@Override
//	public String toString() {
//		return eventList.toString();
//	}
//
//	@Override
//	protected ListIterator<E> listIteratorImpl(int index) {
//		throw new IllegalStateException("listIteratorImpl-method is not used");
//	}
//
//	@Override
//	protected Iterator<E> iteratorImpl() {
//		throw new IllegalStateException("iteratorImpl-method is not used");
//	}
//
//	@Override
//	public int hashCode() {
//		if (isSet()) {
//			Set<E> set = new HashSet<>(this);
//			return set.hashCode();
//		} else {
//			return eventList.hashCode();
//		}
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (isSet()) {
//			Set<E> set = new HashSet<>(this);
//			return set.equals(obj);
//		} else {
//			return eventList.equals(obj);
//		}
//	}
//
//}
