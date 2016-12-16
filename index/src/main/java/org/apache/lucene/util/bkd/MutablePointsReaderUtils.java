/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util.bkd;

import org.apache.lucene.codecs.MutablePointValues;
import org.apache.lucene.util.*;
import org.apache.lucene.util.packed.PackedInts;

/** Utility APIs for sorting and partitioning buffered points.
 *
 * @lucene.internal */
public final class MutablePointsReaderUtils {

  MutablePointsReaderUtils() {}

  /** Sort the given {@link MutablePointValues} based on its packed value then doc ID. */
  public static void sort(int maxDoc, int packedBytesLength,
                          MutablePointValues reader, int from, int to) {
    final int bitsPerDocId = PackedInts.bitsRequired(maxDoc - 1);
    new MSBRadixSorter(packedBytesLength + (bitsPerDocId + 7) / 8) {

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected int byteAt(int i, int k) {
        if (k < packedBytesLength) {
          return Byte.toUnsignedInt(reader.getByteAt(i, k));
        } else {
          final int shift = bitsPerDocId - ((k - packedBytesLength + 1) << 3);
          return (reader.getDocID(i) >>> Math.max(0, shift)) & 0xff;
        }
      }

      @Override
      protected org.apache.lucene.util.Sorter getFallbackSorter(int k) {
        return new MyIntroSorter(reader, k, packedBytesLength);
      }

    }.sort(from, to);
  }

  /** Sort points on the given dimension. */
  public static void sortByDim(int sortedDim, int bytesPerDim, int[] commonPrefixLengths,
                               MutablePointValues reader, int from, int to,
                               BytesRef scratch1, BytesRef scratch2) {

    // No need for a fancy radix sort here, this is called on the leaves only so
    // there are not many values to sort
    final int offset = sortedDim * bytesPerDim + commonPrefixLengths[sortedDim];
    final int numBytesToCompare = bytesPerDim - commonPrefixLengths[sortedDim];
    new IntroSorter() {

      final BytesRef pivot = scratch1;
      int pivotDoc = -1;

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected void setPivot(int i) {
        reader.getValue(i, pivot);
        pivotDoc = reader.getDocID(i);
      }

      @Override
      protected int comparePivot(int j) {
        reader.getValue(j, scratch2);
        int cmp = StringHelper.compare(numBytesToCompare, pivot.bytes, pivot.offset + offset, scratch2.bytes, scratch2.offset + offset);
        if (cmp == 0) {
          cmp = pivotDoc - reader.getDocID(j);
        }
        return cmp;
      }
    }.sort(from, to);
  }

  /** Partition points around {@code mid}. All values on the left must be less
   *  than or equal to it and all values on the right must be greater than or
   *  equal to it. */
  public static void partition(int maxDoc, int splitDim, int bytesPerDim, int commonPrefixLen,
                               MutablePointValues reader, int from, int to, int mid,
                               BytesRef scratch1, BytesRef scratch2) {
    final int offset = splitDim * bytesPerDim + commonPrefixLen;
    final int cmpBytes = bytesPerDim - commonPrefixLen;
    final int bitsPerDocId = PackedInts.bitsRequired(maxDoc - 1);
    new RadixSelector(cmpBytes + (bitsPerDocId + 7) / 8) {

      @Override
      protected Selector getFallbackSelector(int k) {
        return new MyIntroSelector(scratch1, reader, k, cmpBytes, scratch2, offset);
      }

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected int byteAt(int i, int k) {
        if (k < cmpBytes) {
          return Byte.toUnsignedInt(reader.getByteAt(i, offset + k));
        } else {
          final int shift = bitsPerDocId - ((k - cmpBytes + 1) << 3);
          return (reader.getDocID(i) >>> Math.max(0, shift)) & 0xff;
        }
      }
    }.select(from, to, mid);
  }

    private static class MyIntroSorter extends IntroSorter {

        final BytesRef pivot;
        final BytesRef scratch;
        private final MutablePointValues reader;
        private final int k;
        private final int packedBytesLength;
        int pivotDoc;

        public MyIntroSorter(MutablePointValues reader, int k, int packedBytesLength) {
            this.reader = reader;
            this.k = k;
            this.packedBytesLength = packedBytesLength;
            pivot = new BytesRef();
            scratch = new BytesRef();
        }

        @Override
        protected void swap(int i, int j) {
          reader.swap(i, j);
        }

        @Override
        protected void setPivot(int i) {
          reader.getValue(i, pivot);
          pivotDoc = reader.getDocID(i);
        }

        @Override
        protected int comparePivot(int j) {
          if (k < packedBytesLength) {
            reader.getValue(j, scratch);
            int cmp = StringHelper.compare(packedBytesLength - k, pivot.bytes, pivot.offset + k, scratch.bytes, scratch.offset + k);
            if (cmp != 0) {
              return cmp;
            }
          }
          return pivotDoc - reader.getDocID(j);
        }
    }

    private static class MyIntroSelector extends IntroSelector {

        final BytesRef pivot;
        private final BytesRef scratch1;
        private final MutablePointValues reader;
        private final int k;
        private final int cmpBytes;
        private final BytesRef scratch2;
        private final int offset;
        int pivotDoc;

        public MyIntroSelector(BytesRef scratch1, MutablePointValues reader, int k, int cmpBytes, BytesRef scratch2, int offset) {
            this.scratch1 = scratch1;
            this.reader = reader;
            this.k = k;
            this.cmpBytes = cmpBytes;
            this.scratch2 = scratch2;
            this.offset = offset;
            pivot = scratch1;
        }

        @Override
        protected void swap(int i, int j) {
          reader.swap(i, j);
        }

        @Override
        protected void setPivot(int i) {
          reader.getValue(i, pivot);
          pivotDoc = reader.getDocID(i);
        }

        @Override
        protected int comparePivot(int j) {
          if (k < cmpBytes) {
            reader.getValue(j, scratch2);
            int cmp = StringHelper.compare(cmpBytes - k, pivot.bytes, pivot.offset + offset + k, scratch2.bytes, scratch2.offset + offset + k);
            if (cmp != 0) {
              return cmp;
            }
          }
          return pivotDoc - reader.getDocID(j);
        }
    }
}
