package nars.guifx.concept;

import com.gs.collections.impl.set.mutable.UnifiedSet;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import nars.guifx.graph3.SpaceNet;
import nars.guifx.graph3.Xform;
import nars.guifx.util.ColorArray;

import java.util.*;
import java.util.function.Consumer;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 3/18/16.
 */
public abstract class Scatter3D<X> extends SpaceNet {

    final ColorArray ca = new ColorArray(32, Color.BLUE, Color.RED);

    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public Scatter3D() {


        frame();
        setPickOnBounds(true);
        setMouseTransparent(false);
    }

    @Override
    public Xform getRoot() {
        return new Xform();
    }


    public class DataPoint extends Group {

        public final X x;
        private final Box shape;
        private final PhongMaterial mat;
        private Color color = Color.WHITE;

        public DataPoint(X tl) {

            shape = new Box(0.8, 0.8, 0.8);
            mat = new PhongMaterial(color);
            shape.setMaterial(mat);
            //shape.onMouseEnteredProperty()

            getChildren().add(shape);

            x = tl;

            frame();

            shape.setOnMouseEntered(e -> {
                X x = ((DataPoint) e.getSource()).x;
                System.out.println("enter " + x);
            });
            shape.setOnMouseClicked(e -> {
                X x = ((DataPoint) e.getSource()).x;
                System.out.println("click " + x);
            });
            shape.setOnMouseExited(e -> {
                X x = ((DataPoint) e.getSource()).x;
                System.out.println("exit " + x);
            });

        }

        public void setColor(Color nextColor) {
            color = nextColor;
        }

        public void frame() {
            mat.setDiffuseColor(color);
        }


    }


    abstract Iterable<X>[] get();

    protected abstract void update(X tl, double[] position, double[] size, Consumer<Color> color);

    //ca.get(x.getPriority())
    //concept.getTermLinks()

    final Map<X, DataPoint> linkShape = new LinkedHashMap();

    final Set<X> dead = new UnifiedSet();
    double n;

    final float spaceScale = 10;

    public void frame() {

        //if (!isVisible()) return;

        dead.addAll(linkShape.keySet());

        n = 0;

        List<DataPoint> toAdd = new ArrayList();


        Iterable<X>[] collects = get();
        if (collects != null) {
            double[] s = new double[3];
            double[] d = new double[3];
            for (Iterable<X> ii : collects) {
                if (ii == null) continue;
                ii.forEach(tl -> {

                    dead.remove(tl);

                    DataPoint b = linkShape.get(tl);
                    if (b == null) {
                        b = new DataPoint(tl);
                        linkShape.put(tl, b);

                        DataPoint _b = b;
                        update(tl, d, s, _b::setColor);
                        b.setTranslateX(d[0] * spaceScale);
                        b.setTranslateY(d[1] * spaceScale);
                        b.setTranslateZ(d[2] * spaceScale);
                        b.setScaleX(s[0]);
                        b.setScaleY(s[1]);
                        b.setScaleZ(s[2]);

                        toAdd.add(b);
                    }

                    b.frame();

                });
            }
        }

        linkShape.keySet().removeAll(dead);
        Object[] deads = dead.toArray(new Object[dead.size()]);

        dead.clear();

        runLater(() -> {
            getChildren().addAll(toAdd);
            toAdd.clear();

            for (Object x : deads) {
                DataPoint shape = linkShape.remove(x);
                if (shape != null && shape.getParent() != null) {
                    getChildren().remove(shape);
                }
            }
        });

    }
}
