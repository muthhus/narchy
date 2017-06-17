package jcog.tree.rtree.point;

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

import jcog.tree.rtree.HyperPoint;
import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.rect.RectDouble2D;

import java.util.function.Function;

/**
 * Created by jcovert on 6/15/15.
 */
public class Double2D implements HyperPoint {
    public final double x;
    public final double y;

    public Double2D(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int dim() {
        return 2;
    }

    @Override
    public Double coord(final int d) {
        if (d == 0) {
            return x;
        } else if (d == 1) {
            return y;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public double distance(final HyperPoint p) {
        final Double2D p2 = (Double2D) p;

        final double dx = p2.x - x;
        final double dy = p2.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public double distance(final HyperPoint p, final int d) {
        final Double2D p2 = (Double2D) p;
        if (d == 0) {
            return Math.abs(p2.x - x);
        } else if (d == 1) {
            return Math.abs(p2.y - y);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public String toString() {
        return "<" + x + ',' + y + '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Double2D)) return false;

        Double2D double2D = (Double2D) o;

        if (Double.compare(double2D.x, x) != 0) return false;
        return Double.compare(double2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public final static class Builder implements Function<Double2D, HyperRect> {

        @Override
        public HyperRect apply(final Double2D point) {
            return new RectDouble2D(point);
        }

//        @Override
//        public HyperRect getMbr(final HyperPoint p1, final HyperPoint p2) {
//            final Point2D point1 = (Point2D)p1;
//            final Point2D point2 = (Point2D)p2;
//            return new Rect2D(point1, point2);
//        }
    }
}