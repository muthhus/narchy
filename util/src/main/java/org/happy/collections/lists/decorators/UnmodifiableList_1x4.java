//package org.happy.collections.lists.decorators;
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//
//import org.happy.collections.decorators.UnmodifiableStrategy_1x4;
//import org.happy.commons.patterns.observer.Delegate_1x0Impl;
//import org.happy.commons.patterns.observer.event.ActionEventAfter_1x0;
//
///**
// * use this list to decorate other list to make them unmodifiable. In comparison
// * to standard JDK Collections.unmodifiableList(...) you can set different
// * behavior of this degrader. For example you can forbid to add elements but to
// * allow to remove them. If the operation is forbidden the
// * UnsupportedOperationException will be thrown.
// *
// * @author Andreas Hollmann
// * @param <E>
// */
//public class UnmodifiableList_1x4<E> extends ListDecorator_1x0<E, List<E>> {
//
//	/**
//	 * decorates the collection with unmodifiable decorator
//	 *
//	 * @param <E>
//	 * @param decorated
//	 *            list which should be decorated
//	 * @return
//	 */
//	public static <E> UnmodifiableList_1x4<E> of(List<E> decorateable) {
//		return new UnmodifiableList_1x4<E>(decorateable);
//	}
//
//	/**
//	 * decorates the collection with unmodifiable decorator
//	 *
//	 * @param <E>
//	 * @param decorated
//	 *            list which should be decorated
//	 * @param strategy
//	 *            the unmodifiable strategy
//	 * @return
//	 */
//	public static <E> UnmodifiableList_1x4<E> of(List<E> decorateable,
//			UnmodifiableStrategy_1x4 strategy) {
//		return new UnmodifiableList_1x4<E>(decorateable, strategy);
//	}
//
//	private EventList_1x0<E> eventList;
//	private Delegate_1x0Impl<ActionEventAfter_1x0<UnmodifiableStrategy_1x4>> onStrategyChanged = null;
//	private UnmodifiableStrategy_1x4 strategy;
//	/**
//	 * listener which watches for add-operations
//	 */
//	protected ActionListener beforeAddListener = new ActionListener() {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			throw new UnsupportedOperationException(
//					"The collection can't be modified! Choose another strategy as "
//							+ strategy.toString()
//							+ ", if you want to add elements");
//
//		}
//	};
//	/**
//	 * listener which watches for remove-operations
//	 */
//	protected ActionListener beforeRemoveListener = new ActionListener() {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			throw new UnsupportedOperationException(
//					"The collection can't be modified! Choose another strategy as "
//							+ strategy.toString()
//							+ ", if you want to reove elements");
//		}
//	};
//
//	/**
//	 * constructor
//	 *
//	 * @param list
//	 *            the list which should be decorated
//	 */
//	protected UnmodifiableList_1x4(List<E> list) {
//		this(list, UnmodifiableStrategy_1x4.UNMODIFIABLE);
//	}
//
//	/**
//	 * the list which should be decorated
//	 *
//	 * @param list
//	 *            the list which should be decorated
//	 * @param strategy
//	 *            strategy which defines the unmodifiable behavior
//	 */
//	protected UnmodifiableList_1x4(List<E> list,
//			UnmodifiableStrategy_1x4 strategy) {
//		super(list);
//		this.eventList = EventList_1x0.of(list);
//		this.setStrategy(strategy);
//	}
//
//	/**
//	 * removes all listeners from eventCollection
//	 */
//	protected void removeAllListeners() {
//		this.eventList.getOnBeforeAddEvent().remove(beforeAddListener);
//		this.eventList.getOnBeforeRemoveEvent().remove(beforeRemoveListener);
//		this.eventList.getOnBeforeClearEvent().remove(beforeRemoveListener);
//	}
//
//	public UnmodifiableStrategy_1x4 getStrategy() {
//		return strategy;
//	}
//
//	@SuppressWarnings("deprecation")
//	public void setStrategy(UnmodifiableStrategy_1x4 strategy) {
//		boolean strategyHasChanged = false;
//		if (this.strategy == null) {
//			strategyHasChanged = true;
//		} else {
//			strategyHasChanged = !this.strategy.equals(strategy);
//		}
//		this.strategy = strategy;
//		removeAllListeners();
//		if (UnmodifiableStrategy_1x4.REMOVE_ONLY_ALLOWED.equals(this.strategy)
//				|| UnmodifiableStrategy_1x4.UNMODIFIABLE.equals(this.strategy)) {
//			this.eventList.getOnBeforeAddEvent().add(this.beforeAddListener);
//		}
//		if (UnmodifiableStrategy_1x4.ADD_ONLY_ALLOWED.equals(this.strategy)
//				|| UnmodifiableStrategy_1x4.UNMODIFIABLE.equals(this.strategy)) {
//			this.eventList.getOnBeforeRemoveEvent().add(
//					this.beforeRemoveListener);
//			this.eventList.getOnBeforeClearEvent().add(
//					this.beforeRemoveListener);
//		}
//		if (strategyHasChanged)
//			fireOnStrategyChanged(strategy);
//	}
//
//	protected void fireOnStrategyChanged(UnmodifiableStrategy_1x4 newStragegy) {
//		if (onStrategyChanged == null)
//			return;
//		onStrategyChanged
//				.fire(new ActionEventAfter_1x0<UnmodifiableStrategy_1x4>(this,
//						0, "unmodifiable-strategy has changed", newStragegy));
//	}
//
//	/**
//	 * this event will be fired after the unmodifiable-strategy was changed
//	 *
//	 * @return
//	 */
//
//	public Delegate_1x0Impl<ActionEventAfter_1x0<UnmodifiableStrategy_1x4>> getOnStrategyChanged() {
//		if (onStrategyChanged == null)
//			onStrategyChanged = new Delegate_1x0Impl<ActionEventAfter_1x0<UnmodifiableStrategy_1x4>>();
//		return onStrategyChanged;
//	}
//
//	/**
//	 * delegate all methods to eventList
//	 */
//
//	@Override
//	protected ListIterator<E> listIteratorImpl(int index) {
//		// this code will be never called
//		throw new Error(
//				"this code shuold be never called, because the listIterator() - method is overriden");
//		// return this.eventList.listIterator(index);
//	}
//
//	@Override
//	protected Iterator<E> iteratorImpl() {
//		// this code will be never called
//		throw new Error(
//				"this code shuold be never called, because the listIterator() - method is overriden");
//		// return this.eventList.iterator();
//	}
//
//	@Override
//	public void setDecorated(List<E> decorated) {
//		this.eventList.setDecorated(decorated);
//		super.setDecorated(decorated);
//	}
//
//	public void add(int index, E o) {
//		eventList.add(index, o);
//	}
//
//	public boolean addAll(int index, Collection<? extends E> c) {
//		return eventList.addAll(index, c);
//	}
//
//	public ListIterator<E> listIterator() {
//		return eventList.listIterator();
//	}
//
//	public ListIterator<E> listIterator(int index) {
//		return eventList.listIterator(index);
//	}
//
//	public E remove(int index) {
//		return eventList.remove(index);
//	}
//
//	public E set(int index, E o) {
//		return eventList.set(index, o);
//	}
//
//	public List<E> subList(int fromIndex, int toIndex) {
//		return eventList.subList(fromIndex, toIndex);
//	}
//
//	public boolean add(E o) {
//		return eventList.add(o);
//	}
//
//	public boolean addAll(Collection<? extends E> c) {
//		return eventList.addAll(c);
//	}
//
//	public void clear() {
//		if (UnmodifiableStrategy_1x4.UNMODIFIABLE.equals(this.getStrategy())
//				|| UnmodifiableStrategy_1x4.ADD_ONLY_ALLOWED.equals(this
//						.getStrategy())) {
//			throw new UnsupportedOperationException();
//		}
//		eventList.clear();
//	}
//
//	public boolean remove(Object o) {
//		return eventList.remove(o);
//	}
//
//	public boolean removeAll(Collection<?> c) {
//		if (UnmodifiableStrategy_1x4.UNMODIFIABLE.equals(this.getStrategy())
//				|| UnmodifiableStrategy_1x4.ADD_ONLY_ALLOWED.equals(this
//						.getStrategy())) {
//			throw new UnsupportedOperationException();
//		}
//		return eventList.removeAll(c);
//	}
//
//	public boolean retainAll(Collection<?> c) {
//		if (UnmodifiableStrategy_1x4.UNMODIFIABLE.equals(this.getStrategy())
//				|| UnmodifiableStrategy_1x4.ADD_ONLY_ALLOWED.equals(this
//						.getStrategy())) {
//			throw new UnsupportedOperationException();
//		}
//		return eventList.retainAll(c);
//	}
//
//	public Iterator<E> iterator() {
//		return eventList.iterator();
//	}
//
//	@Override
//	public Float getVersion() {
//		return 1.4F;
//	}
//
//}
