package nars.guifx;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;

/**
 * Created by me on 9/13/15.
 */
public class ResizableCanvas extends Canvas {




    public ResizableCanvas() {
        super();

        parentProperty().addListener((z,p,n) -> {
            if (n==null) {
                setWidth(0);
                setHeight(0);
            } else {
                Region parent = (Region) n;
                widthProperty().unbind();
                heightProperty().unbind();
                widthProperty().bind(parent.widthProperty());
                heightProperty().bind(parent.heightProperty());
            }
        });


        //        boolean bindRedraw = true; //TODO parameter to make this optional to avoid unnecessary event being attached
//        if (bindRedraw) {
//            // Redraw canvas when size changes.
//
//            widthProperty().addListener(drawEvent);
//            heightProperty().addListener(drawEvent);
//        }



    }



//    final InvalidationListener drawEvent = evt -> draw();

    //    protected void draw() {
//        /*double width = getWidth();
//        double height = getHeight();
//
//        GraphicsContext gc = getGraphicsContext2D();
//        gc.clearRect(0, 0, width, height);
//
//        gc.setStroke(Color.RED);
//        gc.strokeLine(0, 0, width, height);
//        gc.strokeLine(0, height, width, 0);*/
//    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }


    @Override
    public double prefHeight(double width) {
        return getHeight();
    }
}
