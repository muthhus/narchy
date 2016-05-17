package nars.util;

import java.util.Comparator;

/**
 * Maybe the fastest implementation of famous Quick-Sort
 * algorithm. It is even faster than Denisa Ahrensa implementation that
 * performs 7.5s for sorting million objects, this implementation
 * sorts for 6.8s. However, {@link FastMergeSort} is much faster.
 * http://www.java2s.com/Code/Java/Collections-Data-Structure/FastQuickSort.htm
 */
public class FastQuickSort {


    @SuppressWarnings({"unchecked"})
    public static void qsort(int[] stack, Object[] c, int start, int size, Comparator comparator) {
        int left = start, right = size - 1, stack_pointer = -1;
        while (true) {
            int i;
            int j;
            Object swap;
            if (right - left <= 7) {
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    while (i >= left && comparator.compare(c[i], swap) > 0) {
                        c[i + 1] = c[i--];
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer == -1) {
                    break;
                }
                right = stack[stack_pointer--];
                left = stack[stack_pointer--];
            } else {
                int median = (left + right) >> 1;
                i = left + 1;
                j = right;
                swap = c[median];
                c[median] = c[i];
                c[i] = swap;
                if (comparator.compare(c[left], c[right]) > 0) {
                    swap = c[left];
                    c[left] = c[right];
                    c[right] = swap;
                }
                if (comparator.compare(c[i], c[right]) > 0) {
                    swap = c[i];
                    c[i] = c[right];
                    c[right] = swap;
                }
                if (comparator.compare(c[left], c[i]) > 0) {
                    swap = c[left];
                    c[left] = c[i];
                    c[i] = swap;
                }
                Object temp = c[i];
                while (true) {
                    //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
                    while (comparator.compare(c[++i], temp) < 0) ;
                    //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
                    while (comparator.compare(c[--j], temp) > 0) ;
                    if (j < i) {
                        break;
                    }
                    swap = c[i];
                    c[i] = c[j];
                    c[j] = swap;
                }
                c[left + 1] = c[j];
                c[j] = temp;
                if (right - i + 1 >= j - left) {
                    stack[++stack_pointer] = i;
                    stack[++stack_pointer] = right;
                    right = j - 1;
                } else {
                    stack[++stack_pointer] = left;
                    stack[++stack_pointer] = j - 1;
                    left = i;
                }
            }
        }
    }

//  public void sort(Object a[], Comparator comparator) {
//    qsort(a, comparator);
//  }

//  public void sort(Comparable a[]) {
//    qsort(a, new ComparableComparator());
//  }

    // ---------------------------------------------------------------- static

//  public static void doSort(Object a[], int size, Comparator comparator) {
//    qsort(a, size, comparator);
//  }

//  public static void doSort(Comparable a[]) {
//    qsort(a, ComparableComparator.INSTANCE);
//  }

}

//// Copyright (c) 2003-2009, Jodd Team (jodd.org). All Rights Reserved.
//
///**
// * Comparator that adapts <code>Comparables</code> to the
// * <code>Comparator</code> interface.
// */
//class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
//
//  /**
//   * Cached instance.
//   */
//  public static final ComparableComparator INSTANCE = new ComparableComparator();
//
//  public int compare(T o1, T o2) {
//    return o1.compareTo(o2);
//  }
//
//}
//
//
//
    
   