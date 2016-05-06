//package org.happy.collections.lists.decorators;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Map;
//
//import org.happy.collections.decorators.CacheCollection_1x0;
//import org.happy.collections.lists.decorators.iterators.ListIteratorDecorator_1x0;
//
///**
// *
// * @author Andreas Hollmann, Wjatscheslaw Stoljarski, Eugen Lofing
// * @param <E>
// */
//public class CacheList_1x0<E> extends ListDecorator_1x0<E, List<E>> {
//
//	/**
//	 * decorates the list and returns the decorator back
//	 *
//	 * @param <E>
//	 *            type of the element in the collection
//	 * @param cacheSize
//	 *            percentage size of the cache
//	 * @param c
//	 *            collection which should be decorated
//	 * @return the cache decorator
//	 */
//	public static <E> CacheList_1x0<E> of(final float cacheSize, final List<E> c) {
//		return new CacheList_1x0<E>(cacheSize, c);
//	}
//
//	/**
//	 * delegate all available methods of the collection to this field
//	 */
//	CacheCollection_1x0<E> cacheCollection;
//
//	public CacheList_1x0(final float cacheSize, final List<E> list) {
//		super(list);
//		this.cacheCollection = new CacheCollection_1x0<E>(cacheSize, list);
//	}
//
//	@Override
//	public boolean add(final E e) {
//		return cacheCollection.add(e);
//	}
//
//	@Override
//	public boolean addAll(final Collection<? extends E> c) {
//		return cacheCollection.addAll(c);
//	}
//
//	@Override
//	public void clear() {
//		cacheCollection.clear();
//	}
//
//	@Override
//	public boolean contains(final Object o) {
//		return cacheCollection.contains(o);
//	}
//
//	@Override
//	public boolean containsAll(final Collection<?> c) {
//		return cacheCollection.containsAll(c);
//	}
//
//	public Map<E, Integer> getCache() {
//		return cacheCollection.getCache();
//	}
//
//	public float getCacheSize() {
//		return cacheCollection.getCacheSize();
//	}
//
//	@Override
//	public boolean remove(final Object o) {
//		return cacheCollection.remove(o);
//	}
//
//	@Override
//	public boolean removeAll(final Collection<?> c) {
//		return cacheCollection.removeAll(c);
//	}
//
//	@Override
//	public boolean retainAll(final Collection<?> c) {
//		return cacheCollection.retainAll(c);
//	}
//
//	public void setCacheSize(final float cacheSize) {
//		cacheCollection.setCacheSize(cacheSize);
//	}
//
//	@Override
//	public void setDecorated(final List<E> decorateable) {
//		this.cacheCollection.setDecorated(decorateable);
//		super.setDecorated(decorateable);
//	}
//
//	@Override
//	public E remove(final int index) {
//		final E removed = super.remove(index);
//		this.cacheCollection.getCache().remove(removed);
//		return removed;
//	}
//
//	@Override
//	public E set(final int index, final E element) {
//		final E removed = super.set(index, element);
//		this.cacheCollection.getCache().remove(removed);
//		return removed;
//	}
//
//	@Override
//	protected ListIterator<E> listIteratorImpl(final int index) {
//		final ListIterator<E> it = this.getDecorated().listIterator(index);
//		return new ListIteratorDecorator_1x0<E>(it) {
//
//			private E current = null;
//
//			@Override
//			public E next() {
//				this.current = this.getDecorated().next();
//				return this.current;
//			}
//
//			@Override
//			public E previous() {
//				this.current = this.getDecorated().previous();
//				return this.current;
//			}
//
//			@Override
//			public void remove() {
//				CacheList_1x0.this.cacheCollection.getCache().remove(
//						this.current);
//				super.remove();
//			}
//
//			@Override
//			public void set(final E e) {
//				CacheList_1x0.this.cacheCollection.getCache().remove(
//						this.current);
//				super.set(e);
//			}
//
//		};
//	}
//
//	@Override
//	protected Iterator<E> iteratorImpl() {
//		return this.cacheCollection.iterator();
//	}
//
//}
