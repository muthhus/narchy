//package org.happy.collections.lists.decorators;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.NoSuchElementException;
//
//import org.happy.collections.decorators.EventCollection_1x0;
//import org.happy.collections.lists.List_1x0;
//import org.happy.collections.lists.decorators.iterators.ListIteratorDecorator_1x0;
//import org.happy.commons.util.ObjectPointer_1x0;
//
///**
// * EventList is a decorator, which can be used to decorate java.util.List. Thus
// * you can extend every List implementation with the event-based functionality,
// * so you can register events if the collection is modified, like BeforeAddEvent
// * or AfterAddEvent.
// *
// * @author Andreas Hollmann, Eugen Lofing, Wjatscheslaw Stoljarski
// *
// * @param <E>
// */
//public class EventList_1x0<E> extends EventCollection_1x0<E> implements
//		List_1x0<E> {
//
//	/**
//	 * decorates the list and returns the decorater back
//	 *
//	 * @param <E>
//	 *            type of the element in the list
//	 * @param list
//	 *            list which should be decorated
//	 * @return the eventdecorator
//	 */
//	public static <E> EventList_1x0<E> of(final List<E> list) {
//		return new EventList_1x0<E>(list);
//	}
//
//	/**
//	 * use per default the ArrayList as decorated List, if you like to use other
//	 * list like LinkedList, please use other constructor
//	 */
//	public static <E> EventList_1x0<E> of() {
//		return new EventList_1x0<E>();
//	}
//
//	ListDecorator_1x0<E, List<E>> listDecoratoror;
//
//	/**
//	 * this variable is true if the iterator want access the list
//	 */
//	private boolean iteratorLock = false;
//
//	// [nunmberOfTasks]
//
//	// [start]
//	// --------------------------------------------------------------------------constructors
//	/**
//	 * decorates the list with a
//	 */
//	public EventList_1x0(final List<E> list) {
//		super(list);
//		// delegate all methods to this delegate-class
//		this.listDecoratoror = new ListDecorator_1x0<E, List<E>>(list) {
//
//			@Override
//			public E get(final int index) {
//				E obj = super.get(index);
//				final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(obj);
//				// fire before-get event
//				if (fireOnBeforeGetEvent(p)) {
//					throw new NoSuchElementException(
//							"EventList.get(int index) method caused the execption, because the get-operation for element at this index was canceled in an registered ActionListener on the BeforeGetEvent");
//				}
//				// fire after-get event
//				obj = p.getObject();// maybe object was changed
//				fireOnAfterGetEvent(obj);
//				return obj;
//			}
//
//			@Override
//			protected ListIterator<E> listIteratorImpl(final int index) {
//				final ListIterator<E> it = new ListIteratorDecorator_1x0<E>(
//						super.listIterator(index)) {
//
//					// current element
//					private E obj;
//
//					@Override
//					public E next() {
//						final ObjectPointer_1x0<E> p = ObjectPointer_1x0
//								.of(null);
//						// fire before get event
//						while (true) {
//							this.obj = super.next();
//							p.setObject(this.obj);
//							if (fireOnBeforeGetEvent(p)) {
//								if (super.hasNext()) {
//									continue;// continue until the get-operation
//												// is not canceled any more
//								} else {
//									throw new NoSuchElementException(
//											"the EventList.ListIterator.next() caused runntime exception, because it reached the nunmberOfTasks of the List, but couldn't return the last element because the get-operation for it was canceled in BeforeGetEvent!");
//								}
//							} else {
//								break;
//							}
//						}
//						this.obj = p.getObject();// maybe the object was changed
//
//						// fire after get event
//						fireOnAfterGetEvent(this.obj);
//						return this.obj;
//						// this.obj = super.next();
//						// return this.obj;
//					}
//
//					@Override
//					public E previous() {
//						final ObjectPointer_1x0<E> p = ObjectPointer_1x0
//								.of(null);
//						// fire before get event
//						while (true) {
//							this.obj = super.previous();
//							p.setObject(this.obj);
//							if (fireOnBeforeGetEvent(p)) {
//								if (super.hasPrevious()) {
//									continue;// continue until the get-operation
//												// is not canceled any more
//								} else {
//									throw new NoSuchElementException(
//											"the EventList.ListIterator.previous() caused runntime exception, because it reached the start of the List, but couldn't return first element because the get-operation for it was canceled in BeforeGetEvent!");
//								}
//							} else {
//								break;
//							}
//
//						}
//						this.obj = p.getObject();// maybe the object was changed
//						// fire after get event
//						fireOnAfterGetEvent(this.obj);
//						return this.obj;
//					}
//
//					@Override
//					public void add(E e) {
//						final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(e);
//						// fire onBeforeAddEvent
//						if (EventList_1x0.this.fireOnBeforeAddEvent(p)) {
//							return;// event was canceled
//						}
//						e = p.getObject();// maybe the object was changed
//						EventList_1x0.this.iteratorLock = true;// activate
//																// iterator
//																// events
//						super.add(e);
//						EventList_1x0.this.iteratorLock = false;// deactivate
//																// iterator
//																// events
//						// fire OnAfterAddEvent
//						EventList_1x0.this.fireOnAfterAddEvent(e);
//					}
//
//					@Override
//					public void remove() {
//						final ObjectPointer_1x0<E> p = ObjectPointer_1x0
//								.of(this.obj);
//						// fire onBeforeRemoveEvent
//						if (EventList_1x0.this.fireOnBeforeRemoveEvent(p)) {
//							return;// event was canceled
//						}
//
//						if (this.obj != p.getObject()) {
//							if (this.obj == null
//									|| !this.obj.equals(p.getObject())) {
//								throw new IllegalStateException(
//										"the object which should be removed was changed in the beforeRemoveEvent and can't be removed, because the iterator points to other object!!");
//							}
//						}
//
//						this.obj = p.getObject();// maybe the object was changed
//						EventList_1x0.this.iteratorLock = true;// activate
//																// iterator
//																// events
//						super.remove();
//						EventList_1x0.this.iteratorLock = false;// deactivate
//																// iterator
//																// events
//						// fire OnAfterRemoveEvent
//						EventList_1x0.this.fireOnAfterRemoveEvent(obj);
//					}
//
//					@Override
//					public void set(E e) {
//						final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(e);
//						// fire onBeforeRemoveEvent
//						if (EventList_1x0.this.fireOnBeforeRemoveEvent(p)) {
//							return;// event was canceled
//						}
//
//						// fire onBeforeAddEvent
//						if (EventList_1x0.this.fireOnBeforeAddEvent(p)) {
//							return;// event was canceled
//						}
//
//						e = p.getObject();// maybe it was modified
//
//						EventList_1x0.this.iteratorLock = true;// activate
//																// iterator
//																// events
//						super.set(e);
//						EventList_1x0.this.iteratorLock = false;// activate
//																// iterator
//																// events
//
//						// fire OnAfterRemoveEvent
//						EventList_1x0.this.fireOnAfterRemoveEvent(e);
//
//						// fire OnAfterAddEvent
//						EventList_1x0.this.fireOnAfterAddEvent(e);
//
//					}
//
//				};
//
//				return it;
//			}
//
//			@Override
//			protected Iterator<E> iteratorImpl() {
//				// get iterator from event-collection
//				return EventList_1x0.super.iterator();
//			}
//
//			@Override
//			public List<E> subList(final int fromIndex, final int toIndex) {
//				return new EventList_1x0<E>(super.subList(fromIndex, toIndex)) {
//
//					@Override
//					protected boolean fireOnBeforeAddEvent(
//							final ObjectPointer_1x0<E> p) {
//						return EventList_1x0.this.fireOnBeforeAddEvent(p);
//					}
//
//					@Override
//					protected void fireOnAfterAddEvent(final E obj) {
//						EventList_1x0.this.fireOnAfterAddEvent(obj);
//					}
//
//					@Override
//					protected boolean fireOnBeforeClearEvent() {
//						return EventList_1x0.this.fireOnBeforeClearEvent();
//					}
//
//					@Override
//					protected void fireOnAfterClearEvent() {
//						EventList_1x0.this.fireOnAfterClearEvent();
//					}
//
//					@Override
//					protected boolean fireOnBeforeRemoveEvent(
//							final ObjectPointer_1x0<E> p) {
//						return EventList_1x0.this.fireOnBeforeRemoveEvent(p);
//					}
//
//					@Override
//					protected void fireOnAfterRemoveEvent(final E obj) {
//						EventList_1x0.this.fireOnAfterRemoveEvent(obj);
//					}
//
//					@Override
//					protected boolean fireOnBeforeGetEvent(
//							final ObjectPointer_1x0<E> p) {
//						return EventList_1x0.this.fireOnBeforeGetEvent(p);
//					}
//
//					@Override
//					protected void fireOnAfterGetEvent(final E obj) {
//						EventList_1x0.this.fireOnAfterGetEvent(obj);
//					}
//
//					@Override
//					protected boolean doFireOnBeforeRemoveEvent() {
//						return EventList_1x0.this.doFireOnBeforeRemoveEvent();
//					}
//
//					@Override
//					protected boolean doFireOnAfterRemoveEvent() {
//						return EventList_1x0.this.doFireOnAfterRemoveEvent();
//					}
//
//					@Override
//					protected boolean doFireOnBeforeAddEvent() {
//						return EventList_1x0.this.doFireOnBeforeAddEvent();
//					}
//
//					@Override
//					protected boolean doFireOnAfterAddEvent() {
//						return EventList_1x0.this.doFireOnAfterAddEvent();
//					}
//
//					@Override
//					protected boolean doFireOnBeforeGetEvent() {
//						return EventList_1x0.this.doFireOnBeforeGetEvent();
//					}
//
//					@Override
//					protected boolean doFireOnAfterGetEvent() {
//						return EventList_1x0.this.doFireOnAfterGetEvent();
//					}
//
//				};
//			}
//
//		};
//
//	}
//
//	/**
//	 * use per default the ArrayList as decorated List, if you like to use other
//	 * list like LinkedList, please use other constructor
//	 */
//	public EventList_1x0() {
//		this(new ArrayList<E>());
//	}
//
//	@Override
//	public List<E> getDecorated() {
//		return (List<E>) super.getDecorated();
//	}
//
//	// [nunmberOfTasks]
//
//	// [start]
//	// --------------------------------------------------------------------------operations
//
//	@Override
//	public void add(final int index, E o) {
//
//		// don't fire any events because iterator do this
//		if (this.iteratorLock) {
//			this.listDecoratoror.add(index, o);
//			return;
//		}
//		final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(o);
//		if (fireOnBeforeAddEvent(p)) {
//			return;// event was canceled
//		}
//
//		o = p.getObject();// maybe it was changed
//
//		// add object to the list
//		this.listDecoratoror.add(index, o);
//
//		fireOnAfterAddEvent(o);
//		return;// operation successful
//
//	}
//
//	@Override
//	public boolean addAll(final int index, final Collection<? extends E> c) {
//		// create new Collection
//		final ArrayList<E> list = new ArrayList<E>();
//		boolean ret = true;// return value
//
//		final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(null);
//		// iterate throw the collection and add element, witch are not canceled
//		for (E obj : c) {
//
//			// generate before add event
//			if (this.onBeforeAddEvent != null) {
//				p.setObject(obj);
//				if (this.fireOnBeforeAddEvent(p)) {
//					continue;// event was canceled, continue, don't add element
//								// to the new collection
//				}
//				obj = p.getObject();// maybe the object was changed
//			}
//
//			// add object to the collection
//			list.add(obj);
//		}
//
//		// // if the new collection is not empty call super operation
//		// if (list == null)
//		// return false;
//		ret = this.listDecoratoror.addAll(index, list) && ret;
//
//		// fire after add a
//		for (final E obj : list) {
//			fireOnAfterAddEvent(obj);
//		}
//
//		return ret;// operation successful
//	}
//
//	@Override
//	public E remove(final int index) {
//
//		// don't fire any events because iterator do this
//		if (this.iteratorLock) {
//			return this.listDecoratoror.remove(index);
//		}
//
//		if (this.onBeforeRemoveEvent != null) {
//			final E obj = this.get(index);
//			final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(obj);
//			if (fireOnBeforeRemoveEvent(p)) {
//				return null;// event was canceled
//			}
//			if (obj != p.getObject()) {
//				super.remove(obj);// remove changed object from the list
//			}
//		}
//		final E obj = this.listDecoratoror.remove(index);
//
//		fireOnAfterRemoveEvent(obj);
//		return obj;
//	}
//
//	@Override
//	public E set(final int index, final E o) {
//
//		// don't fire any events because iterator do this
//		if (this.iteratorLock) {
//			this.listDecoratoror.set(index, o);
//		}
//
//		final IndexOutOfBoundsException ex = this.checkIndex(index);
//		if (ex != null) {
//			throw ex;
//		}
//
//		// get object for index
//		E obj = this.get(index);
//		if (obj != null) {
//			final ObjectPointer_1x0<E> p = ObjectPointer_1x0.of(obj);
//			// generate before remove event
//			if (this.fireOnBeforeRemoveEvent(p)) {
//				return null;
//			}
//
//			// generate before add event
//			if (this.fireOnBeforeAddEvent(p)) {
//				return null;
//			}
//
//			obj = p.getObject();// maybe the object has changed
//
//		}
//		final E ret = this.listDecoratoror.set(index, o);
//
//		fireOnAfterRemoveEvent(ret);
//
//		fireOnAfterAddEvent(o);
//		return ret;
//
//	}
//
//	@Override
//	protected IndexOutOfBoundsException checkIndex(final int index) {
//		final int length = this.getDecorated().size();
//		if (index < 0) {
//			return new IndexOutOfBoundsException("index must be grater then 0");
//		}
//		if (length < index) {
//			return new IndexOutOfBoundsException(
//					"index must be smaler then list.size()");
//		}
//		return null;
//	}
//
//	/**
//	 * Returns the element at the specified position in this list.
//	 *
//	 * @param index
//	 *            index of element to return.
//	 * @return the element at the specified position in this list.
//	 *
//	 * @throws IndexOutOfBoundsException
//	 *             if the index is out of range (index &lt; 0 || index &gt;=
//	 *             size()).
//	 * @exception NoSuchElementException
//	 *                iteration has no more elements.
//	 */
//	@Override
//	public E get(final int index) {
//		return this.listDecoratoror.get(index);
//
//	}
//
//	@Override
//	public int indexOf(final Object o) {
//		return this.listDecoratoror.indexOf(o);
//	}
//
//	@Override
//	public int lastIndexOf(final Object index) {
//		return this.listDecoratoror.lastIndexOf(index);
//	}
//
//	@Override
//	public ListIterator<E> listIterator() {
//		return this.listDecoratoror.listIterator();
//	}
//
//	@Override
//	public ListIterator<E> listIterator(final int index) {
//		return this.listDecoratoror.listIterator(index);
//	}
//
//	@Override
//	public List<E> subList(final int fromIndex, final int toIndex) {
//		return this.listDecoratoror.subList(fromIndex, toIndex);
//	}
//
//	/**
//	 * Returns a list iterator of the elements in this list (in proper
//	 * sequence).
//	 *
//	 * @return a list iterator of the elements in this list (in proper
//	 *         sequence).
//	 * @exception NoSuchElementException
//	 *                iteration has no more elements.
//	 */
//	@Override
//	public ListIterator<E> listIterator(final E elem) {
//		return this.listDecoratoror.listIterator(elem);
//	}
//
//	@Override
//	public boolean equals(final Object obj) {
//		if (obj == null || !(obj instanceof List<?>)) {
//			return false;
//		}
//		return this.getDecorated().equals(obj);
//	}
//
//}
