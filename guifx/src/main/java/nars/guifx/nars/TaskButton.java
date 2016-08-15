package nars.guifx.nars;

import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import nars.NAR;
import nars.Task;
import nars.budget.Budgeted;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.guifx.util.NSlider;
import nars.link.BLink;
import nars.term.Termed;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by me on 12/13/15.
 */ //public static class TaskButton extends ToggleButton {
public class TaskButton<X> extends Label implements Runnable {

    private static final Pattern p0 = Pattern.compile("-->", Pattern.LITERAL);
    private static final Pattern p1 = Pattern.compile("<->", Pattern.LITERAL);
    private static final Pattern p2 = Pattern.compile("<=>", Pattern.LITERAL);
    private static final Pattern p3 = Pattern.compile("||", Pattern.LITERAL);
    private final X item;
    private final Scale scale;
    private float lastPri = -1f;
    private final static float priTolerance = 0.05f;


    public TaskButton(X t, NAR nar) {
        super();
        this.item = t;

        Object ref = t instanceof BLink ? ((BLink) t).get() : t;

        String s;
        if (ref instanceof Task) {
            s = labelize(((Task)ref).toStringWithoutBudget(null));
        } else if (ref instanceof Termed) {
            s = labelize( ((Termed)ref).term().toString() );
        } else if (ref!=null) {
            s = ref.toString();
        } else {
            s = "null";
        }
        setText(s);


        //getStyleClass().clear();
        getStyleClass().add("taskbutton");

        //setTextAlignment(TextAlignment.LEFT);
        //setAlignment(Pos.TOP_LEFT);

        scale = new Scale();
        scale.setPivotX(0);
        scale.setPivotY(0);
        this.getTransforms().add(scale);

        setCursor(Cursor.CROSSHAIR);
        setWrapText(true);

        setCacheHint(CacheHint.SCALE);
        setCache(true);

        if (ref instanceof Task) {
            setOnMouseClicked(c -> {
                //setSelected(false);
                setFocused(false);

                Popup p = new Popup();
                TaskPane tp = new TaskPane(nar, (Task)(ref));
                tp.setTop(new FlowPane(
                        new NSlider("pri", 100, 25, 0.5f),
                        new Button("+")
                ));
                tp.setBackground(Background.EMPTY);
                ((Pane) tp.getTop()).setBackground(Background.EMPTY);
                p.getContent().add(tp);

                //p.setOpacity(0.75f);
                p.setAutoHide(true);
                p.setAutoFix(true);

                p.show(this, c.getScreenX(), c.getScreenY());

            });
        } else {
            //show concept
        }

        //runLater(this);
        run();

    }

    private static String labelize(String s) {
        //https://en.wikipedia.org/wiki/List_of_logic_symbols
        return p3.matcher(p2.matcher(p1.matcher(p0.matcher(s)
                .replaceAll(Matcher.quoteReplacement("→")))
                .replaceAll(Matcher.quoteReplacement("↔")))
                .replaceAll(Matcher.quoteReplacement("⇄")))
                .replaceAll(Matcher.quoteReplacement("⇵"))
                ;
        //↔ ⇔ ⇒ ⇄ ⇾ ⇥ ⇵
    }

    @Override
    public void run() {
        float pri = (item instanceof Budgeted) ? ((Budgeted) item).pri() : 0;

        if (!Util.equals(lastPri,pri,priTolerance)) {


            double scale = getScale(pri);
            if (scale <= 0) {
                setVisible(false);
                return;
            } else {
                setVisible(true);
            }


            if (scalesText()) {
                Scale scale1 = this.scale;
                float minScale = 0.5f;
                double newScale =  scale + minScale;
                scale1.setX(newScale);
                scale1.setY(newScale);
                //setNeedsLayout(true);
            }


            Object item = this.item;
            if (item instanceof BLink) item = ((BLink)item).get(); //get what it refers to


            //            setBackground(new Background(
            //                    new BackgroundFill(
            //                        c,
            //                        CornerRadii.EMPTY,
            //                        Insets.EMPTY)));

            if (item instanceof Termed)
                color(getColor(pri));

            this.lastPri = pri;
        }
    }

    protected void color(Color c) {
        setTextFill(c);
    }

    public boolean scalesText() {
        return true;
    }

    public static float getScale(float pri) {
        return pri + 0.5f;
    }

    @NotNull
    private Color getColor(float pri) {
//        return hsb(
//                (o.ordinal() / 64f) * 360.0,
//                0.4, 0.7, 0.75f + pri * 0.25f
//        );

        Termed tt;
        if (item instanceof Termed) {
            tt = (Termed)item;
        } else if (item instanceof BLink) {
            tt = ((BLink<? extends Termed>)item).get();
        } else {
            throw new UnsupportedOperationException();
        }
        return TermNode.getTermColor(tt, DefaultNodeVis.colors, pri);
    }

}
