package nars.guifx.graph2.impl;

import com.gs.collections.impl.tuple.Tuples;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import nars.NAR;
import nars.guifx.JFX;
import nars.guifx.NARfx;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.term.Termed;
import nars.util.data.Util;

/**
 * Created by me on 12/27/15.
 */
public class HexButtonVis extends DefaultNodeVis {

    public static final Font labelFont = NARfx.mono(12f);
    private final NAR n;

    public HexButtonVis(NAR n) {
        this.n = n;
    }

    public static class HexButton<X> extends Group {

        public final X value;
        public final Node base;
        public Node label = null;

        static final float sizeRatio = 6;
        final int maxLabelLength = 64;

        public HexButton(X object) {
            super();

            //setManaged(false);
            //setAutoSizeChildren(false);

            this.value = object;

            this.base = getBase();

            setScaleX(1/sizeRatio);
            setScaleY(1/sizeRatio);

            getChildren().add(base);

            if (null!=(this.label = getLabel())) {
                //label.setCacheHint(CacheHint.SCALE_AND_ROTATE);
                label.setCache(true);
                getChildren().add(label);
            }



            //base.setCacheHint(CacheHint.SCALE_AND_ROTATE);
            //base.setCache(true);

            //setCacheHint(CacheHint.SCALE_AND_ROTATE);
            //setCache(true);
        }

        private Node getLabel() {
            String vs = value.toString();
            if (vs.length() > maxLabelLength)
                return null;

            Text s = new Text(vs);
            //s.setManaged(false);

            //s.setTextAlignment(TextAlignment.CENTER);
            s.setTextOrigin(VPos.CENTER);

            //s.setCenterShape(true);
            s.setTextAlignment(TextAlignment.CENTER);

            Bounds labelBounds = s.getBoundsInLocal();
            Bounds baseBounds = base.getBoundsInLocal();

            double ls = labelFont.getSize();
            s.setScaleX(1.0/ ls);
            s.setScaleY(1.0/ ls);
            double labelWidth = labelBounds.getWidth();
            double baseWidth = baseBounds.getWidth();
            if (labelWidth > baseWidth) {
                s.setLayoutX(-labelWidth /2 - baseWidth /2);
            } else {
                //s.setLayoutX( (baseWidth-labelWidth) / 2); //not quite right
            }

//            //manually align it
//            double textCenter = (labelBounds.getMaxX() + labelBounds.getMinX())/2f;
//            double baseCenter = (baseBounds.getMaxX() + baseBounds.getMinX())/2f;
//            System.out.println(textCenter + " " + baseCenter);
//            s.setTranslateX( -(textCenter - baseCenter)/2 ); //if text is wider than base, align center

            s.setFont(labelFont);
            s.setStroke(null);

            s.setSmooth(false);

            s.setFill(Color.WHITE);

            return s;
        }

        private Node getBase() {
            Polygon s = JFX.newPoly(6, sizeRatio);
            //s.setStrokeType(StrokeType.INSIDE);
            //s.setManaged(false);

            //s.setFill(Color.GRAY);
            s.setStroke(null);
            //s.setFill(Color.WHITE);
            //s.setOpacity(0.75f);
            //s.shade(1f);

            //s.setManaged(false);
            //s.setCenterShape(false);
            return s;
        }
    }

    @Override
    public TermNode newNode(Termed term, NAR nar) {
        return new HexNode(nar, term);
    }



    @Deprecated //TODO pass the HexButton builder as a Supplier lambda and remove the class
    private static class HexNode extends TermNode  {

        private final HexButton<Termed> button;

        public HexNode(NAR nar, Termed term) {
            super(term, 32);
            HexButton<Termed> h = new HexButton<>(this.term);
            this.button = h;
            //TODO HexButton.setFillColor
            ((Polygon)h.base).setFill(
                TermNode.getTermColor(this.term,
                    CanvasEdgeRenderer.colors,
                    1.0 / (double) this.term.term().volume()
                )
            );
            h.base.setUserData(Tuples.pair(nar, term));
            h.base.setOnMouseClicked(onDoubleClickTerm);
            h.base.setMouseTransparent(false);
            h.base.setPickOnBounds(true);

            getChildren().add(h);

            //super(nar, term, 32, null, null);

        }


        static final float EPSILON = 0.001f;

        @Override
        public boolean pri(float v) {
            float lastPri = pri();
            if (!Util.equals(v, lastPri, EPSILON)) {
                super.pri(v);
                return true;
            }
            return false;
        }


    }
}
