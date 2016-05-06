package org.happy.collections.lists;

import java.util.List;
import java.util.ListIterator;

public interface List_1x0<E> extends List<E> {
	/**
	 * gets the ListIterator for the first found element, else returns null
	 * 
	 * @param elem
	 * @return
	 */
	public ListIterator<E> listIterator(E elem);
}
