package spacegraph.layout.treechart;

import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.gs.collections.impl.factory.SortedSets;
import com.jogamp.opengl.GL2;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.ConsoleSurface;

import java.util.*;

/**
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class TreemapChart extends Surface {

	public static void main(String[] args) {
		SpaceGraph<VirtualTerminal> s = new SpaceGraph<VirtualTerminal>();
		s.show(800, 800);
		TreemapChart tc = new TreemapChart(500, 400,
				Item.get("x", 1f),
				Item.get("y", 0.5f),
				Item.get("z", 0.25f)
		);
		System.out.println(tc.children);
		s.add(new Facial(tc));
	}

	enum LayoutOrient {

		VERTICAL, HORIZONTAL
	}

	private double height;
	private double width;
	private double heightLeft;
	private double widthLeft;
	private double left;
	private double top;
	private LayoutOrient layoutOrient = LayoutOrient.HORIZONTAL;
	private final List<ItemVis> children = new ArrayList<>();

	public TreemapChart(double width, double height ) {
		update(width, height, Collections.emptyList() );
	}
	public TreemapChart(double width, double height, Item... i ) {
		update(width, height, Lists.newArrayList(i) );
	}

	@Override
	protected void paint(GL2 gl) {
		super.paint(gl);


		double totalArea = width * height;
		for (ItemVis v : children) {
			v.paint(gl, v.getArea() / totalArea);
		}
	}

	public void update(double width, double height, Collection<Item> children) {
		this.width = width;
		this.height = height;
		left = 0.0;
		top = 0.0;

		this.children.clear();
		for (Item item : children) {
			ItemVis treemapElement = new ItemVis(item);
			this.children.add(treemapElement);
		}
		layoutOrient = width > height ? LayoutOrient.VERTICAL : LayoutOrient.HORIZONTAL;
		scaleArea(this.children);
//        Collections.sort(this.children, new ChildComparator());
//        LOG.log(Level.INFO, "Initial children: {0}", this.children);
		heightLeft = this.height;
		widthLeft = this.width;
		squarify(new ArrayDeque<>(this.children), new ArrayDeque<>(), minimumSide());
//        for (ItemVis child : children) {
//            Node treeElementItem = elementFactory.createElement(child);
//            if (child.getTop() > height) {
//                throw new IllegalStateException("Top is bigger than height");
//            }
//            if (child.getLeft() > width) {
//                throw new IllegalStateException("Left is bigger than width");
//            }
//            AnchorPane.setTopAnchor(treeElementItem, child.getTop());
//        }
	}


	private void squarify(Deque<ItemVis> children, Deque<ItemVis> row, double w) {
		ArrayDeque<ItemVis> remainPoped = new ArrayDeque<>(children);
		ItemVis c = remainPoped.pop();
		Deque<ItemVis> concatRow = new ArrayDeque<>(row);
		concatRow.add(c);


		double worstConcat = worst(concatRow, w);
		double worstRow = worst(row, w);

		if (row.isEmpty() || (worstRow > worstConcat || isDoubleEqual(worstRow, worstConcat))) {
			Deque<ItemVis> remaining = new ArrayDeque<>(remainPoped);
			if (remaining.isEmpty()) {
				layoutrow(concatRow, w);
			} else {
				squarify(remaining, concatRow, w);
			}
		} else {
			layoutrow(row, w);
			squarify(children, new ArrayDeque<>(), minimumSide());
		}
	}

	private static double worst(Deque<ItemVis> ch, double w) {
		if (ch.isEmpty()) {
			return Double.MAX_VALUE;
		}
		double areaSum = 0.0, maxArea = 0.0, minArea = Double.MAX_VALUE;
		for (ItemVis item : ch) {
			double area = item.getArea();
			areaSum += area;
			minArea = minArea < area ? minArea : area;
			maxArea = maxArea > area ? maxArea : area;
		}
		double sqw = w * w;
		double sqAreaSum = areaSum * areaSum;
		return Math.max(sqw * maxArea / sqAreaSum,
				sqAreaSum / (sqw * minArea));
	}

	private void layoutrow(Deque<ItemVis> row, double w) {

		double totalArea = 0.0;
		for (ItemVis item : row) {
			double area = item.getArea();
			totalArea += area;
		}

		if (layoutOrient == LayoutOrient.VERTICAL) {


			double rowWidth = totalArea / w;
			double topItem = 0;

			for (ItemVis item : row) {
				double area = item.getArea();
				item.setTop(top + topItem);
				item.setLeft(left);
				item.setWidth(rowWidth);
				double h = (area / rowWidth);
				item.setHeight(h);

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
				double area = item.getArea();
				item.setTop(top);
				item.setLeft(left + rowLeft);
				item.setHeight(rowHeight);
				double wi = (area / rowHeight);
				item.setWidth(wi);

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

	private void scaleArea(List<ItemVis> children) {
		double areaGiven = width * height;
		double areaTotalTaken = 0.0;
		for (ItemVis child : children) {
			double area = child.getArea();
			areaTotalTaken += area;
		}
		double ratio = areaTotalTaken / areaGiven;
		for (ItemVis child : children) {
			double area = child.getArea() / ratio;
			child.setArea(area);
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
