package org.happy.collections.lists.decorators;

import org.happy.collections.lists.CollectionDecorator_1x0;
import org.happy.collections.lists.List_1x0;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Decorator_1x0Impl which can be used to decorate any Lists
 * 
 * @author Wjatscheslaw Stoljarski, Andreas Hollmann, Eugen Lofing
 * 
 * @param <E>
 *            type of the element
 * @param <D>
 *            type of the decorated list
 */
public abstract class ListDecorator_1x0<E, D extends List<E>> extends CollectionDecorator_1x0<E, D> implements List_1x0<E> {
	public final D list;


	/**
	 * constructor
	 * 
	 * @param list
	 *            list which you want to decorate
	 */

	/**
	 * constructor
	 * 
	 * @param list
	 *            list you want to decorate
	 */
	public ListDecorator_1x0(final D list) {
		this.list = list;
	}

	@Override
	public D decorated() {
		return list;
	}

	@Override
	public boolean add(final E arg0) {
		return list.add(arg0);
	}

	@Override
	public void add(final int arg0, final E arg1) {
		list.add(arg0, arg1);
	}

	@Override
	public boolean addAll(final Collection<? extends E> arg0) {
		return list.addAll(arg0);
	}

	@Override
	public boolean addAll(final int arg0, final Collection<? extends E> arg1) {
		return list.addAll(arg0, arg1);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean contains(final Object arg0) {
		return list.contains(arg0);
	}

	@Override
	public boolean containsAll(final Collection<?> arg0) {
		return list.containsAll(arg0);
	}

	@Override
	public E get(final int arg0) {
		return list.get(arg0);
	}

	@Override
	public int indexOf(final Object arg0) {
		return list.indexOf(arg0);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public int lastIndexOf(final Object arg0) {
		return list.lastIndexOf(arg0);
	}


	@NotNull
	@Override
	public ListIterator<E> listIterator() {
		return list.listIterator();
	}

	@NotNull
	@Override
	public ListIterator<E> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public ListIterator<E> listIterator(E elem) {
		return list.listIterator(indexOf(elem));
	}
	//	/**
//	 * don't override this method in inherited classes, to avoid twice
//	 * decoration of the same decorator often list uses
//	 * ListIterator.listIterator(int index) method to create the list-iterator
//	 * for ListIterator.listIterator(), thus if you would override this method
//	 * and create inside of it new ListDecorator the Iterator of decorated List
//	 * will be decorated twice this method calls the method
//	 * this.ListIterator.listIterator(int index), thus override that method if
//	 * you want to decorate the listiterator
//	 */
//	@Override
//	public ListIterator<E> listIterator() {
//		if (this.isDecorateIterators()) {
//			return this.listIterator(0);
//		} else {
//			return list.listIterator();
//		}
//	}

//	@Override
//	public ListIterator<E> listIterator(final int index) {
//		ListIterator<E> it = null;
//		if (this.isDecorateIterators() && !this.avoidLoop) {
//			this.avoidLoop = true;
//			it = this.listIteratorImpl(index);
//			this.avoidLoop = false;
//		} else {
//			it = list.listIterator(index);
//		}
//		return it;
//	}
//
//	@Override
//	public ListIterator<E> listIterator(final E elem) {
//		int i = 0;
//		final ListIterator<E> it = list.listIterator();
//		while (it.hasNext()) {
//			if (elem.equals(it.next())) {
//				return this.listIterator(i);// iterator for element found
//			}
//			i++;
//		}
//		return null;
//	}

//	/**
//	 * implement this method for a iterator-decorator
//	 *
//	 * @param index
//	 * @return
//	 */
//	protected abstract ListIterator<E> listIteratorImpl(int index);

	@Override
	public E remove(final int index) {
		return list.remove(index);
	}

	@Override
	public boolean remove(final Object o) {
		return list.remove(o);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public E set(final int index, final E element) {
		return list.set(index, element);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public List<E> subList(final int fromIndex, final int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return list.toArray(a);
	}

	@Override
	public String toString() {
		return this.list.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		// if(obj==null || !(obj instanceof List<?>)){
		// return false;
		// }
		// return Collections_1x2.isEqualLists(this, (List<?>)obj);
		return list.equals(obj);
	}

}
