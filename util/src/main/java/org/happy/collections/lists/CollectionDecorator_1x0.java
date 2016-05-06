package org.happy.collections.lists;

import java.util.Collection;
import java.util.Iterator;


/**
 * decorates collections, delegates all methods to the collection
 * 
 * @author Eugen Lofing, Andreas Hollmann, Wjatscheslaw Stoljarski
 * 
 * @param <E>
 *            type of the element
 */
public abstract class CollectionDecorator_1x0<E, D extends Collection<E>> implements Collection<E> {



	abstract public D decorated();




//	/**
//	 * searches for the last decorated in the chain of many decorators
//	 *
//	 * @param <T>
//	 *            type of the decorated object
//	 * @param decorator
//	 *            the highest decorated, which will be searched downstairs
//	 * @return the last decorated object, which is itself not decorator
//	 */
//	@SuppressWarnings("unchecked")
//	public static <T> T getLastDecoratedObject(Decorator_1x0<T> decorator) {
//		if (decorator == null) {
//			throw new IllegalArgumentException("the parameter can't be null");
//		}
//		while (true) {
//			final T decorated = decorator.getDecorated();
//			if (decorated instanceof Decorator_1x0<?>) {
//				decorator = (Decorator_1x0<T>) decorated;
//			} else {
//				return decorated;
//			}
//		}
//	}

	/**
	 * this exception will be thrown if the decorated collection is null, but
	 * you can't decorate a null!
	 * 
	 * @author Andreas Hollmann
	 * 
	 */
	public static class NullDecoratedCollectionException extends
			RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3492691934290321954L;

		public NullDecoratedCollectionException() {
			super();
		}

		public NullDecoratedCollectionException(final String message,
				final Throwable cause) {
			super(message, cause);
		}

		public NullDecoratedCollectionException(final String message) {
			super(message);
		}

		public NullDecoratedCollectionException(final Throwable cause) {
			super(cause);
		}

	}

	private boolean decorateIterators = true;
	private boolean avoidLoop;



	@Override
	public boolean add(final E e) {
		return decorated().add(e);
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		return decorated().addAll(c);
	}

	@Override
	public void clear() {
		decorated().clear();
	}

	@Override
	public boolean contains(final Object o) {
		return decorated().contains(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return decorated().containsAll(c);
	}

	// public boolean equals(Object o) {
	// return this.equals(o);
	// }

	@Override
	public int hashCode() {
		return decorated().hashCode();
	}

	@Override
	public boolean isEmpty() {
		return decorated().isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return decorated().iterator();
//		Iterator<E> it = null;
//		if (this.decorateIterators && !this.avoidLoop) {
//			this.avoidLoop = true;
//			it = this.iteratorImpl();
//			this.avoidLoop = false;
//		} else {
//			it = getDecorated().iterator();
//		}
//		return it;
	}



	@Override
	public boolean remove(final Object o) {
		return decorated().remove(o);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return decorated().removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return decorated().retainAll(c);
	}

	@Override
	public int size() {
		return decorated().size();
	}

	@Override
	public Object[] toArray() {
		return decorated().toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return decorated().toArray(a);
	}

	public boolean isDecorateIterators() {
		return decorateIterators;
	}

	public void setDecorateIterators(final boolean decorateIterators) {
		this.decorateIterators = decorateIterators;
	}

	@Override
	public String toString() {
		return decorated().toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		return decorated().equals(obj);
	}

}