package org.happy.collections.lists.decorators;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.happy.collections.lists.CollectionDecorator_1x0;
import org.happy.collections.lists.List_1x0;

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

	private boolean avoidLoop;

	/**
	 * constructor
	 * 
	 * @param list
	 *            list which you want to decorate
	 */
	public ListDecorator_1x0(final D list) {
		this(list, true);// decorate iterators by default
	}

	/**
	 * constructor
	 * 
	 * @param list
	 *            list you want to decorate
	 * @param decorateIterators
	 *            if you want more performance in foreach-loop and you don't
	 *            really use iterator you can disable the decoration of
	 *            iterators
	 */
	public ListDecorator_1x0(final D list, final boolean decorateIterators) {
		super(list);
		// check arguments
		if (list == null) {
			throw new IllegalArgumentException(
					"the decorated list can't be null");
		}

	}

	@Override
	public D getDecorated() {
		return super.getDecorated();
	}



	@Override
	public boolean add(final E arg0) {
		return getDecorated().add(arg0);
	}

	@Override
	public void add(final int arg0, final E arg1) {
		getDecorated().add(arg0, arg1);
	}

	@Override
	public boolean addAll(final Collection<? extends E> arg0) {
		return getDecorated().addAll(arg0);
	}

	@Override
	public boolean addAll(final int arg0, final Collection<? extends E> arg1) {
		return getDecorated().addAll(arg0, arg1);
	}

	@Override
	public void clear() {
		getDecorated().clear();
	}

	@Override
	public boolean contains(final Object arg0) {
		return getDecorated().contains(arg0);
	}

	@Override
	public boolean containsAll(final Collection<?> arg0) {
		return getDecorated().containsAll(arg0);
	}

	@Override
	public E get(final int arg0) {
		return getDecorated().get(arg0);
	}

	@Override
	public int indexOf(final Object arg0) {
		return getDecorated().indexOf(arg0);
	}

	@Override
	public boolean isEmpty() {
		return getDecorated().isEmpty();
	}

	@Override
	public int lastIndexOf(final Object arg0) {
		return getDecorated().lastIndexOf(arg0);
	}

	/**
	 * don't override this method in inherited classes, to avoid twice
	 * decoration of the same decorator often list uses
	 * ListIterator.listIterator(int index) method to create the list-iterator
	 * for ListIterator.listIterator(), thus if you would override this method
	 * and create inside of it new ListDecorator the Iterator of decorated List
	 * will be decorated twice this method calls the method
	 * this.ListIterator.listIterator(int index), thus override that method if
	 * you want to decorate the listiterator
	 */
	@Override
	public ListIterator<E> listIterator() {
		if (this.isDecorateIterators()) {
			return this.listIterator(0);
		} else {
			return getDecorated().listIterator();
		}
	}

	@Override
	public ListIterator<E> listIterator(final int index) {
		ListIterator<E> it = null;
		if (this.isDecorateIterators() && !this.avoidLoop) {
			this.avoidLoop = true;
			it = this.listIteratorImpl(index);
			this.avoidLoop = false;
		} else {
			it = getDecorated().listIterator(index);
		}
		return it;
	}

	@Override
	public ListIterator<E> listIterator(final E elem) {
		int i = 0;
		final ListIterator<E> it = getDecorated().listIterator();
		while (it.hasNext()) {
			if (elem.equals(it.next())) {
				return this.listIterator(i);// iterator for element found
			}
			i++;
		}
		return null;
	}

	/**
	 * implement this method for a iterator-decorator
	 * 
	 * @param index
	 * @return
	 */
	protected abstract ListIterator<E> listIteratorImpl(int index);

	@Override
	public E remove(final int index) {
		return getDecorated().remove(index);
	}

	@Override
	public boolean remove(final Object o) {
		return getDecorated().remove(o);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return getDecorated().removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return getDecorated().retainAll(c);
	}

	@Override
	public E set(final int index, final E element) {
		return getDecorated().set(index, element);
	}

	@Override
	public int size() {
		return getDecorated().size();
	}

	@Override
	public List<E> subList(final int fromIndex, final int toIndex) {
		return getDecorated().subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return getDecorated().toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return getDecorated().toArray(a);
	}

	@Override
	public String toString() {
		return this.getDecorated().toString();
	}

	@Override
	public boolean equals(final Object obj) {
		// if(obj==null || !(obj instanceof List<?>)){
		// return false;
		// }
		// return Collections_1x2.isEqualLists(this, (List<?>)obj);
		return getDecorated().equals(obj);
	}

}
