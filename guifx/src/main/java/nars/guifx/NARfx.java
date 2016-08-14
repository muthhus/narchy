package nars.guifx;

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nars.$;
import nars.NAR;
import nars.guifx.concept.AbstractConceptPane;
import nars.guifx.concept.DetailedConceptPane;
import nars.guifx.demo.NARide;
import nars.guifx.nars.TaskPane;
import nars.guifx.util.ColorMatrix;
import nars.task.Task;
import nars.term.Termed;
import nars.util.FX;
import nars.util.Util;

import java.io.IOException;
import java.util.Map;


/**
 *
 * @author me
 */
public final class NARfx extends FX {



    public static String css;
    static {
        try {
            css = Util.inputToString(NARfx.class.getClassLoader().getResourceAsStream("narfx.css"));
        } catch (IOException e) {
            css = "";
        }
    }

    /** retarded hack because this stylesheet stuff in JavaFX is designed pretty bad */
    public static void theme(Scene scene) {
        NARide.FXCSSUpdater.applyCssToParent(scene);
    }

    public static void newWindow(NAR nar, Termed c) {
        //TODO //ConceptPane wn = new ConceptPane(nar, c);
        AbstractConceptPane wn = new DetailedConceptPane(nar, c);

        Stage st = newWindow(c.toString(), wn);
        Stage removed = window.put(c, st);
        st.setAlwaysOnTop(true); //? does this work
        theme(st.getScene());
        wn.autosize();

        if (removed!=null)
            removed.close();
    }



    public static void newWindow(NAR nar, Task c) {
        TaskPane wn = new TaskPane(nar, c);

        Stage st;
        Stage removed = window.put(c, st = newWindow(
                c.toString(), wn));

        st.setAlwaysOnTop(true); //? does this work

        if (removed!=null)
            removed.close();
    }

    //final static public Font monospace = new Font("Monospace", 14);

    static final int fontPrecision = 4; //how many intervals per 1.0 to round to
    static final IntObjectHashMap<Font> monoFonts = new IntObjectHashMap();

    public static Font mono(double v) {
        //[Dialog, SansSerif, Serif, Monospaced, DialogInput]
        if (v < 1) v = 1;

        int i = (int)Math.round(v * fontPrecision);

        double finalV = v;

        return monoFonts.getIfAbsentPut(i, () -> Font.font("Monospaced", finalV));
    }

//   static void popup(Core core, Parent n) {
//        Stage st = new Stage();
//
//        st.setScene(new Scene(n));
//        st.show();
//    }
//   static void popup(Core core, Application a) {
//        Stage st = new Stage();
//
//        BorderPane root = new BorderPane();
//        st.setScene(new Scene(root));
//        try {
//            a.start(st);
//        } catch (Exception ex) {
//            Logger.getLogger(NARfx.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        st.show();
//    }
//
//    static void popupObjectView(Core core, NObject n) {
//        Stage st = new Stage();
//
//        BorderPane root = new BorderPane();
//
//        WebView v = new WebView();
//        v.getEngine().loadContent(ObjectEditPane.toHTML(n));
//
//        root.setCenter(v);
//
//        st.setTitle(n.id);
//        st.setScene(new Scene(root));
//        st.show();
//    }


    /** Object instances -> GUI windows */
    public static final Map<Object, Stage> window = $.newHashMap();


    public static final ColorMatrix colors = new ColorMatrix(24, 24,
            (priority, conf) -> {
//                if (priority > 1) priority = 1f;
//                if (priority < 0) priority = 0;
                return Color.hsb(150.0 + 75.0 * (conf),
                        0.10f + 0.85f * priority,
                        0.10f + 0.5f * priority);
            }
    );


    /*TODO
    public static final Color hashColor(Object op, float intensity, ColorMatrix ca) {

    }*/
    public static Color hashColor(Object op, ColorMatrix ca) {
        return hashColor(op.hashCode(), ca);
    }

    public static Color hashColor(int h, ColorMatrix ca) {
        return hashColor(h, 0, ca);
    }

    public static Color hashColor(int h, double b, ColorMatrix ca) {
        int cl = ca.cc.length;
        int i = (h % cl);
        if (i < 0) i = -i;
        return ca.get( ((double)i) / cl, b);
    }

    public static void popup(Parent n) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        //dialog.initOwner(n.
        Scene dialogScene = new Scene(n, 500, 500);
        dialog.setScene(dialogScene);
        dialog.show();

    }

//    public static void newConceptWindow(NAR nar, Pane container, String... concepts) {
//        newConceptWindow(nar, container, (List<Concept>) of(concepts).map(
//                nar::concept).collect(toList())
//        );
//    }
//
//    private static void newConceptWindow(NAR nar, Pane v, List<? extends Termed> cc) {
//
//        for (Termed c : cc) {
//            AbstractConceptPane wn =
//                    //new DetailedConceptPane(nar, c);
//                    new SimpleConceptPane(nar, c);
//            wn.setMaxWidth(250);
//            wn.setPrefWidth(250);
//            v.getChildren().add(wn);
//        }
//
//        v.layout();
//
//        Stage st;
//        Stage removed = window.put(cc, st = newWindow("Concepts", scrolled(v)));
//
//        if (removed!=null)
//            removed.close();
//
//    }

    static final NARide.FXCSSUpdater updater;
    static {
        StringProperty cssProp = new SimpleStringProperty("");
        updater = new NARide.FXCSSUpdater();
        updater.bindCss(cssProp);
        cssProp.setValue(NARfx.css);
    }



}
