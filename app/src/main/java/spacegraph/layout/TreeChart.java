package spacegraph.layout;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.util.data.list.FasterList;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static spacegraph.layout.TreeChart.WeightedString.w;

/**
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class TreeChart<X> extends Surface {


	protected int limit = -1;

	public static void main(String[] args) {
		SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
		s.show(800, 800);
		TreeChart<WeightedString> tc = new TreeChart<>(500, 400,
				(w, v) -> {
					v.update(w.weight);
				},
				w("z", 0.25f),
				w("x", 1f),
				w("y", 0.5f),
				w("a", 0.1f),
				w("b", 0.08f),
				w("c", 0.07f)
		);
		System.out.println(tc.children);
		s.add(new Facial(tc));
	}

	enum LayoutOrient {

		VERTICAL, HORIZONTAL
	}

	protected double height;
	protected double width;
	private double heightLeft;
	private double widthLeft;
	private double left;
	private double top;
	private LayoutOrient layoutOrient = LayoutOrient.HORIZONTAL;
	private Collection<ItemVis<X>> children;
	final Cache<X,ItemVis<X>> cache;// = new WeakHashMap();

	public TreeChart() {
		this(0,0, null);
	}


	public TreeChart(double width, double height, BiConsumer<X,ItemVis<X>> apply, X... i ) {
		cache = Caffeine.newBuilder().maximumSize(1024).build();
		update(width, height, Lists.newArrayList(i), apply );
	}

	@Override
	protected void paint(GL2 gl) {
		super.paint(gl);


		double totalArea = width * height;
		for (ItemVis v : children) {
			v.paint(gl, v.area / totalArea);
		}
	}

	public void update(double width, double height, Iterable<X> children, BiConsumer<X, ItemVis<X>> update) {
		update(width, height, 0, children, update, i -> new ItemVis<>(i, i.toString()));
	}


	public void update(double width, double height, int estimatedSize, Iterable<X> nextChildren, BiConsumer<X, ItemVis<X>> update, Function<X, ItemVis<X>> itemBuilder) {
		this.width = width;
		this.height = height;
		left = 0.0;
		top = 0.0;

		CircularFifoQueue<ItemVis<X>> newChildren = new CircularFifoQueue<>(1+estimatedSize);

		int i = limit < 0 ? Integer.MAX_VALUE : limit;

		for (X item : nextChildren) {
			if (i-- <= 0)
				break;
			if (item==null)
				continue;
			ItemVis<X> e = cache.get(item, itemBuilder);
			if (e!=null) {
				update.accept(item, e);
				newChildren.add(e);
			}
		}

		if (!newChildren.isEmpty()) {
			layoutOrient = width > height ? LayoutOrient.VERTICAL : LayoutOrient.HORIZONTAL;
			scaleArea(newChildren);
			heightLeft = this.height;
			widthLeft = this.width;

			squarify(newChildren, new CircularFifoQueue<>(), minimumSide());
		}
		this.children = newChildren;

	}


	private void squarify(CircularFifoQueue<ItemVis<X>> children, Collection<ItemVis> row, double w) {
		CircularFifoQueue<ItemVis<X>> remainPoped = new CircularFifoQueue<>(children);
		ItemVis c = remainPoped.poll();//.pop();

		FasterList<ItemVis> concatRow = concat(row, c);

		double worstConcat = worst(concatRow, w);
		double worstRow = worst(row, w);

		if (row.isEmpty() || (worstRow > worstConcat || isDoubleEqual(worstRow, worstConcat))) {

			if (remainPoped.isEmpty()) {
				layoutrow(concatRow, w);
			} else {
				squarify(remainPoped, concatRow, w);
			}
		} else {
			layoutrow(row, w);
			squarify(children, Collections.emptyList(), minimumSide());
		}
	}

	private static FasterList<ItemVis> concat(Collection<ItemVis> row, ItemVis c) {
		FasterList<ItemVis> concatRow = new FasterList<>(row.size()+1);
		for (ItemVis i : row)
			concatRow.add(i);
		concatRow.add(c);
		return concatRow;
	}

	private static double worst(Collection<ItemVis> ch, double w) {
		if (ch.isEmpty()) {
			return Double.MAX_VALUE;
		}
		double areaSum = 0.0, maxArea = 0.0, minArea = Double.MAX_VALUE;
		for (ItemVis item : ch) {
			double area = item.area;
			areaSum += area;
			minArea = minArea < area ? minArea : area;
			maxArea = maxArea > area ? maxArea : area;
		}
		double sqw = w * w;
		double sqAreaSum = areaSum * areaSum;
		return Math.max(sqw * maxArea / sqAreaSum,
				sqAreaSum / (sqw * minArea));
	}

	private void layoutrow(Iterable<ItemVis> row, double w) {

		double totalArea = 0.0;
		for (ItemVis item : row) {
			totalArea += item.area;
		}

		if (layoutOrient == LayoutOrient.VERTICAL) {


			double rowWidth = totalArea / w;
			double topItem = 0;

			for (ItemVis item : row) {
				double area = item.area;
				item.top = top + topItem;
				item.left = left;
				item.width = rowWidth;
				double h = (area / rowWidth);
				item.height = h;

				topItem += h;
			}
			widthLeft -= rowWidth;
			//this.heightLeft -= w;
			left += rowWidth;
			double minimumSide = minimumSide();
			if (!isDoubleEqual(minimumSide, heightLeft)) {
				changeLayout();
			}
		} else {

			double rowHeight = totalArea / w;
			double rowLeft = 0;

			for (ItemVis item : row) {
				double area = item.area;
				item.top = top;
				item.left = left + rowLeft;
				item.height = rowHeight;
				double wi = (area / rowHeight);
				item.width = wi;

				rowLeft += wi;
			}
			//this.widthLeft -= rowHeight;
			heightLeft -= rowHeight;
			top += rowHeight;

			double minimumSide = minimumSide();
			if (!isDoubleEqual(minimumSide, widthLeft)) {
				changeLayout();
			}
		}

	}

	private void changeLayout() {
		layoutOrient = layoutOrient == LayoutOrient.HORIZONTAL ? LayoutOrient.VERTICAL : LayoutOrient.HORIZONTAL;
	}

	private static boolean isDoubleEqual(double one, double two) {
		double eps = 0.00001;
		return Math.abs(one - two) < eps;
	}

	private double minimumSide() {
		return Math.min(heightLeft, widthLeft);
	}

	private void scaleArea(Collection<ItemVis<X>> children) {
		double areaGiven = width * height;
		double areaTotalTaken = 0.0;
		for (ItemVis child : children) {
			double area = child.area;
			areaTotalTaken += area;
		}
		double ratio = areaTotalTaken / areaGiven;
		for (ItemVis child : children) {
			child.setArea(child.area / ratio );
		}
	}

	public static class WeightedString {
		public final String label;
		public final float weight;

		public WeightedString(String label, float weight) {
			this.label = label;
			this.weight = weight;
		}

		@Override
		public String toString() {
			return label;
		}

		public static WeightedString w(String s, float w) {
			return new WeightedString(s, w);
		}
	}

//	// private final ColorBucket colorBucket = ColorBucket.createBucket();
//	public final Item root;
//
//	public final DoubleProperty width = new SimpleDoubleProperty(640.0);
//	public final DoubleProperty height = new SimpleDoubleProperty(280.0);
//
//	private final TreemapLayout treemapLayouter;
//
//	public TreemapChart(Item root) {
//        this.root = root;
//        SortedSet<Item> items = root.content();
//        treemapLayouter = elementFactory.createTreemapLayout(width.doubleValue(), height.doubleValue(), items);
//        ChangeListener<Number> changeListener = (observableValue, number, number2) -> treemapLayouter.update(width.doubleValue(), height.doubleValue(), items);
//        width.addListener(changeListener);
//        height.addListener(changeListener);
//        getChildren().add(treemapLayouter);
//
//    }
//	public void update() {
//		treemapLayouter.update(width.doubleValue(), height.doubleValue(),
//				root.content());
//		autosize();
//	}
//
//	public DoubleProperty getWidth() {
//		return width;
//	}
//
//	public DoubleProperty getHeight() {
//		return height;
//	}

	/**
     * @author Tadas Subonis <tadas.subonis@gmail.com>
     */
    public static final class ItemVis<X> {

        public final String label;
        public final X item;
        public double left;
        public double top;
        public double width;
        public double height;
        public double area;
        private float r;
        private float g;
        private float b;

        public ItemVis(X item, String label) {
            this.item = item;
            this.label = label;
        }

        public void update(float weight) {
            this.area = weight;
            this.r = -1;
        }

    //    public void update(X item, String label, float weight) {
    //        this.item = item;
    //        this.label = label;
    //        this.area= weight;
    //        this.r = -1; //auto
    //    }

        public void update(float weight, float r, float g, float b) {
            this.area= weight;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public String toString() {
            return "TreemapDtoElement{" +
                    "label='" + label + '\'' +
                    ", area=" + area +
                    ", top=" + top +
                    ", left=" + left +
                    '}';
        }

        void setArea(double area) {
            this.area = area;
        }

    //    boolean isContainer() {
    //        return item.isContainer();
    //    }

        @Override
        public boolean equals(Object o) {
            return this == o;

            //if (o == null || getClass() != o.getClass()) return false;

    //        ItemVis that = (ItemVis) o;
    //
    //        if (item != null ? !item.equals(that.item) : that.item != null) return false;
    //        return !(label != null ? !label.equals(that.label) : that.label != null);

        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
    //        int result = label != null ? label.hashCode() : 0;
    //        result = 31 * result + (item != null ? item.hashCode() : 0);
    //        return result;
        }

        public void paint(GL2 gl, double percent) {
            float i = 0.25f + 0.75f * (float)percent;

            if (r < 0) {
                r = i;
                g = 0.1f;
                b = 0.1f;
            }

            gl.glColor3f(r, g, b);

            Draw.rect(gl,
                (float)left, (float)top,
                (float)width, (float)height
            );

            gl.glColor3f(1,1,1);
            float labelSize = (float) (height * percent * 20f ); /// 4f * Math.min(0.5f,percent));

			if ((labelSize > 0.003f) && (labelSize < 0.1f)) {
				Draw.text(gl, label,
						labelSize, //label size
						(float) (left + width / 2f), (float) (top + height / 2f), 0);
			}

        }
    }
}
