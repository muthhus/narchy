/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.util;

import java.util.Arrays;

/**
 * push to end, pop from beginning
 * <p/>
 * @author Peter Karich
 */
public class SimpleIntDeque
{
    public static final int growthThreshold = 10;
    private int[] arr;
    private final int growFactor = 2;
    private int frontIndex;
    private int endIndexPlusOne;

    public SimpleIntDeque()    {
        this(10);
    }

    public SimpleIntDeque( int initSize ) {
//        if ((int) (initSize * growFactor) <= initSize)
//        {
//            throw new RuntimeException("initial size or increasing grow-factor too low!");
//        }

        //this.growFactor = growFactor;
        this.arr = new int[initSize];
    }

    int getCapacity()
    {
        return arr.length;
    }

    /*public void setGrowFactor( float factor )
    {
        this.growFactor = factor;
    }*/

    public boolean isEmpty()
    {
        return frontIndex >= endIndexPlusOne;
    }

    public int pop()     {
        int tmp = arr[frontIndex];
        frontIndex++;

        // removing the empty space of the front if too much is unused        
        int smallerSize = arr.length / growFactor;
        int frontIndex = this.frontIndex;
        if (frontIndex > smallerSize)
        {
            int endIndexPlusOne = this.endIndexPlusOne = getSize();
            // ensure that there are at least 10 entries
            int[] newArr = new int[endIndexPlusOne + growthThreshold];
            System.arraycopy(arr, frontIndex, newArr, 0, endIndexPlusOne);
            this.arr = newArr;
            this.frontIndex = 0;
        }

        return tmp;
    }

    public int getSize()
    {
        return endIndexPlusOne - frontIndex;
    }

    public void push( int v )     {
        int[] a = this.arr;
        if (endIndexPlusOne >= a.length)  {
            a = this.arr = Arrays.copyOf(a, a.length * growFactor);
        }
        a[endIndexPlusOne++] = v;
    }



    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int frontIndex = this.frontIndex;
        for (int i = frontIndex; i < endIndexPlusOne; i++)
        {

            if (i > frontIndex)
            {
                sb.append(", ");
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}