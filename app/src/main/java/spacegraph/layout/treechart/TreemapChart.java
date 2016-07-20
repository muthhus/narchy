package spacegraph.layout.treechart;

import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.util.data.list.FasterList;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static spacegraph.layout.treechart.TreemapChart.WeightedString.w;

/**
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class TreemapChart<X> extends Surface {


	protected int limit = -1;

	public static void main(String[] args) {
		SpaceGraph<VirtualTerminal> s = new SpaceGraph<VirtualTerminal>();
		s.show(800, 800);
		TreemapChart<WeightedString> tc = new TreemapChart<>(500, 400,
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

	public TreemapChart() {
		this(0,0);
	}


	public TreemapChart(double width, double height ) {
		update(width, height, Collections.emptyList(), null );
	}

	public TreemapChart(double width, double height, BiConsumer<X,ItemVis<X>> apply, X... i ) {
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

	final WeakHashMap<X,ItemVis<X>> cache = new WeakHashMap();

	public void update(double width, double height, int estimatedSize, Iterable<X> nextChildren, BiConsumer<X, ItemVis<X>> update, Function<X, ItemVis<X>> itemBuilder) {
		this.width = width;
		this.height = height;
		left = 0.0;
		top = 0.0;

		ArrayDeque<ItemVis<X>> newChildren = new ArrayDeque<>(estimatedSize);

		int i = limit < 0 ? Integer.MAX_VALUE : limit;

		for (X item : nextChildren) {
			if (i-- <= 0)
				break;
			if (item==null)
				continue;
			ItemVis<X> e = cache.computeIfAbsent(item, itemBuilder);
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

			squarify(newChildren, new ArrayDeque<>(), minimumSide());
		}
		this.children = newChildren;

	}


	private void squarify(ArrayDeque<ItemVis<X>> children, Collection<ItemVis> row, double w) {
		ArrayDeque<ItemVis<X>> remainPoped = new ArrayDeque<>(children);
		ItemVis c = remainPoped.pop();

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
}
