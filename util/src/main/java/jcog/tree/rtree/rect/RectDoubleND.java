package jcog.tree.rtree.rect;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.point.DoubleND;

import java.io.Serializable;
import java.util.function.Function;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

/**
 * Created by jcovert on 6/15/15.
 */

public class RectDoubleND implements HyperRect<DoubleND>, Serializable {

    public static final HyperRect ALL_1 = RectDoubleND.all(1);
    public static final HyperRect ALL_2 = RectDoubleND.all(2);
    public static final HyperRect ALL_3 = RectDoubleND.all(3);
    public static final HyperRect ALL_4 = RectDoubleND.all(4);
    public static final DoubleND unbounded = new DoubleND() {
        @Override
        public String toString() {
            return "*";
        }
    };
    public final DoubleND min;
    public final DoubleND max;

    public RectDoubleND() {
        min = unbounded;
        max = unbounded;
    }

    public RectDoubleND(final DoubleND p) {
        min = p;
        max = p;
    }

    public RectDoubleND(double[] a, double[] b) {
        this(new DoubleND(a), new DoubleND(b));
    }


    public RectDoubleND(final DoubleND a, final DoubleND b) {
        int dim = a.dim();

        double[] min = new double[dim];
        double[] max = new double[dim];

        double[] ad = a.coord;
        double[] bd = b.coord;
        for (int i = 0; i < dim; i++) {
            double ai = ad[i];
            double bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new DoubleND(min);
        this.max = new DoubleND(max);
    }

    public static HyperRect all(int i) {
        return new RectDoubleND(DoubleND.fill(i, NEGATIVE_INFINITY), DoubleND.fill(i, POSITIVE_INFINITY));
    }

    @Override
    public boolean contains(final HyperRect _inner) {
        final RectDoubleND inner = (RectDoubleND) _inner;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            if (!(min.coord[i] <= inner.min.coord[i] && max.coord[i] >= inner.max.coord[i]))
                //if (min.coord[i] > inner.min.coord[i] || max.coord[i] < inner.max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public boolean intersects(final HyperRect r) {
        final RectDoubleND x = (RectDoubleND) r;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            /*return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                    (min.y > r2.max.y) || (r2.min.y > max.y));*/

            if (min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public double cost() {
        double sigma = 1f;
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            sigma *= getRangeFinite(i, 1 /* an infinite dimension can not be compared, so just ignore it */);
        }
        return sigma;
    }

    @Override
    public HyperRect mbr(final HyperRect r) {
        final RectDoubleND x = (RectDoubleND) r;

        int dim = dim();
        double[] newMin = new double[dim];
        double[] newMax = new double[dim];
        for (int i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectDoubleND(newMin, newMax);
    }


    @Override
    public double center(int dim) {
        return centerF(dim);
    }

    public double centerF(int dim) {
        double min = this.min.coord[dim];
        double max = this.max.coord[dim];
        if ((min == NEGATIVE_INFINITY) && (max == Double.POSITIVE_INFINITY))
            return 0;
        if (min == NEGATIVE_INFINITY)
            return max;
        if (max == Double.POSITIVE_INFINITY)
            return min;

        return (max + min) / 2f;
    }

    @Override
    public DoubleND center() {
        int dim = dim();
        double[] c = new double[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = centerF(i);
        }
        return new DoubleND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public DoubleND min() {
        return min;
    }

    @Override
    public DoubleND max() {
        return max;
    }

    @Override
    public double getRange(final int i) {
        double min = this.min.coord[i];
        double max = this.max.coord[i];
        if (min == max)
            return 0;
        if ((min == NEGATIVE_INFINITY) || (max == Double.POSITIVE_INFINITY))
            return Double.POSITIVE_INFINITY;
        return (max - min);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        RectDoubleND r = (RectDoubleND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        if (min.equals(max)) {
            return min.toString();
        } else {
            return new StringBuilder().append('(').append(min).append(',').append(max).append(')').toString();
        }
    }


    public final static class Builder<X extends RectDoubleND> implements Function<X, HyperRect> {

        @Override
        public X apply(final X rect2D) {
            return rect2D;
        }

    }


}