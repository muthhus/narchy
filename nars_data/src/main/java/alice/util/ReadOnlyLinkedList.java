/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package alice.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * ReadOnlyLinkedList<E> encapsulate a {@link LinkedList<E>}
 * and ensures that the given list is navigated only.
 *
 * Even if ReadOnlyLinkedList<E> implements {@link List<E>} it doesn't
 * support modifiers methods, and throws {@link UnsupportedOperationException}
 * if invoked.
 *
 * @author Paolo Contessi
 * @since 2.2
 */
public class ReadOnlyLinkedList<E> implements List<E>{
    protected LinkedList<E> list;

    public ReadOnlyLinkedList(){
        list = new LinkedList<E>();
    }
    
    public ReadOnlyLinkedList(LinkedList<E> llist){
        if(llist != null){
            list = llist;
        } else {
            list = new LinkedList<E>();
        }
    }

    @Override
    public boolean add(E o){
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public void add(int index, E element){
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public boolean addAll(Collection<? extends E> c){
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c){
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public void clear(){
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new ListItr(list, 0);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("This is a read-only list");
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListItr(list, 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListItr(list, index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * Returns a copy of the wrapped list, useful for destructive navigation
     *
     * @return A copy of the wrapped list
     */
    public LinkedList<E> getEditableCopy(){
        return new LinkedList<E>(list);
    }


    private class ListItr implements ListIterator<E> {
        private final ListIterator<E> it;

        public ListItr(LinkedList<E> list, int index){
            it = list.listIterator(index);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public E next() {
            return it.next();
        }

        @Override
        public boolean hasPrevious() {
            return it.hasPrevious();
        }

        @Override
        public E previous() {
            return it.previous();
        }

        @Override
        public int nextIndex() {
            return it.nextIndex();
        }

        @Override
        public int previousIndex() {
            return it.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This is a read-only list");
        }

        @Override
        public void set(E o) {
            throw new UnsupportedOperationException("This is a read-only list");
        }

        @Override
        public void add(E o) {
            throw new UnsupportedOperationException("This is a read-only list");
        }
    }
}
