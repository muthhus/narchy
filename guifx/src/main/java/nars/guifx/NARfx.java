package nars.guifx;

import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import com.sun.javafx.scene.control.inputmap.InputMap;
import impl.org.controlsfx.table.MappedList;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.concept.AbstractConceptPane;
import nars.guifx.concept.DetailedConceptPane;
import nars.guifx.concept.SimpleConceptPane;
import nars.guifx.demo.NARide;
import nars.guifx.util.ColorMatrix;
import nars.task.Task;
import nars.term.Termed;
import nars.util.data.Util;
import org.reactfx.collection.LiveArrayList;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;


/**
 *
 * @author me
 */
public enum NARfx  {
    ;

    //Prism Renderer options - http://adamish.com/blog/archives/320
    static {
        System.setProperty("javafx.animation.framerate", "30");
        System.setProperty("prism.verbose", "true");
        //System.setProperty("prism.dirtyopts", "false");
        //System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "30"); //by default this is 60
        //System.setProperty("prism.forceGPU", "true");
    }

    public static String css;
    static {
        try {
            css = Util.inputToString(NARfx.class.getClassLoader().getResourceAsStream("narfx.css"));
        } catch (IOException e) {
            css = "";
        }
    }

    public static final ColorMatrix colors = new ColorMatrix(24, 24,
            (priority, conf) -> {
//                if (priority > 1) priority = 1f;
//                if (priority < 0) priority = 0;
                return Color.hsb(150.0 + 75.0 * (conf),
                        0.10f + 0.85f * priority,
                        0.10f + 0.5f * priority);
            }
    );


    /** Object instances -> GUI windows */
    public static Map<Object, Stage> window = Global.newHashMap();

    public static ScrollPane scrolled(Node n) {
        return scrolled(n, true, true);
    }

    public static ScrollPane scrolled(Node n, boolean stretchwide, boolean stretchhigh) {
        return scrolled(n, stretchwide, stretchwide, stretchhigh, stretchhigh);
    }

    public static ScrollPane scrolled(Node n, boolean slideWide, boolean stretchwide, boolean slideHigh, boolean stretchhigh) {
        ScrollPane s = new ScrollPane();
        s.setHbarPolicy(slideWide ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
        s.setVbarPolicy(slideHigh ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);

        s.setContent(n);

        if (stretchhigh) {
            s.setMaxHeight(Double.MAX_VALUE);
        }
        s.setFitToHeight(true);

        if (stretchwide) {
            s.setMaxWidth(Double.MAX_VALUE);
        }
        s.setFitToWidth(true);

        //s.autosize();
        return s;
    }

//    public void start_(Stage primaryStage) {
//        primaryStage.setTitle("Tree View Sample");
//
//        CheckBoxTreeItem<String> rootItem =
//                new CheckBoxTreeItem<String>("View Source Files");
//        rootItem.setExpanded(true);
//
//        final TreeView tree = new TreeView(rootItem);
//        tree.setEditable(true);
//
//        tree.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
//        for (int i = 0; i < 8; i++) {
//            final CheckBoxTreeItem<String> checkBoxTreeItem =
//                    new CheckBoxTreeItem<String>("Sample" + (i+1));
//            rootItem.getChildren().add(checkBoxTreeItem);
//        }
//
//        tree.setRoot(rootItem);
//        tree.setShowRoot(true);
//
//        StackPane root = new StackPane();
//        root.getChildren().add(tree);
//        primaryStage.setScene(new Scene(root, 300, 250));
//        primaryStage.show();
//    }




//    static {
//        Toolkit.getToolkit().init();
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Application.launch(NARfx.class);
//            }
//        }).start();
//
//    }


//    public static void run(NAR n) {
//
//        //Default d = new Default();
//
//        /*Default d = new Equalized(1024,4,5);
//        d.setCyclesPerFrame(4);
//        d.setTermLinkBagSize(96);
//        d.setTaskLinkBagSize(96);*/
//
//        Default d = new Default(); //new Equalized(1024,1,3);
//        //Default d = new Default(1024,2,3);
//
//        NAR n = new NAR(d);
//
//        NARide w = NARfx.newWindow(n);
//
//
//        for (String s : getParameters().getRaw()) {
//            try {
//                n.input(new File(s));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        try {
//            n.input(new File("/tmp/h.nal")); //temporary
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        primaryStage.setOnCloseRequest(e -> {
//            n.stop();
//        });
//
//        {
//
//
//
//
//
////            final TilePane lp = new TilePane(4,4,
//////                        new LinePlot("Total Priority", () ->
//////                            nar.memory.getActivePrioritySum(true, true, true)
//////                        , 128),
////                    new LinePlot("Concept Priority", () -> {
////                        int c = nar.memory.getControl().size();
////                        if (c == 0) return 0;
////                        else return nar.memory.getActivePrioritySum(true, false, false) / (c);
////                    }, 128),
////                    new LinePlot("Link Priority", () ->
////                            nar.memory.getActivePrioritySum(false, true, false)
////                            , 128),
////                    new LinePlot("TaskLink Priority", () ->
////                            nar.memory.getActivePrioritySum(false, false, true)
////                            , 128)
////            );
////            lp.setPrefColumns(2);
////            lp.setPrefRows(2);
////
////            new CycleReaction(w.nar) {
////
////                @Override
////                public void onCycle() {
////                    for (Object o : lp.getChildren()) {
////                        if (o instanceof LinePlot)
////                            ((LinePlot)o).update();
////                    }
////                }
////            };
////
////            lp.setOpacity(0.5f);
////            lp.setPrefSize(200,200);
////            lp.maxWidth(Double.MAX_VALUE);
////            lp.maxHeight(Double.MAX_VALUE);
////            lp.setMouseTransparent(true);
////            lp.autosize();
//
//
////                StackPane s = new StackPane(lp);
////                s.maxWidth(Double.MAX_VALUE);
////                s.maxHeight(Double.MAX_VALUE);
//
//
//            w.content.getTabs().add(new TabX("Terminal", new TerminalPane(w.nar) ));
//
//
//
////              NARGraph1 g = new NARGraph1(w.nar);
////            SubScene gs = g.newSubScene(w.content.getWidth(), w.content.getHeight());
////            gs.widthProperty().bind(w.content.widthProperty());
////            gs.heightProperty().bind(w.content.heightProperty());
////
////            AnchorPane ags = new AnchorPane(gs);
////            w.content.getTabs().add(new TabX("Graph", ags ));
//
//        }
//        //startup defaults
//        w.console(true);
//
//
//        //JFX.popup(new NodeControlPane());
//
//        /*
//        WebBrowser w = new WebBrowser();
//
//
//        primaryStage.setTitle("title");
//        primaryStage.show();
//        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
//            @Override
//            public void handle(WindowEvent event) {
//                System.exit(0);
//            }
//        });
//
//        try {
//            w.start(primaryStage);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }*/
//
//    }


    public static Stage newWindow(String title, Region n) {

        n.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        n.autosize();

        Scene scene = new Scene(n);
        NARfx.theme(scene);

        Stage s = new Stage();
        s.setTitle(title);
        s.setScene(scene);

        //n.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        s.show();

        return s;
    }

    public static Stage newWindow(String title, Scene scene) {
        Stage s = new Stage();
        s.setTitle(title);
        return newWindow(title, scene, s);
    }

    public static Stage newWindow(String title, Scene scene, Stage stage) {
        stage.setScene(scene);
        NARfx.theme(scene);


        //scene.getRoot().maxWidth(Double.MAX_VALUE);
        //scene.getRoot().maxHeight(Double.MAX_VALUE);
        return stage;
    }

//    public static void newWindow(NAR nar) {
//        NARide.show(nar.loop(), (Consumer)null);
//    }

    public static void newWindow(NAR nar, Termed c) {
        //TODO //ConceptPane wn = new ConceptPane(nar, c);
        AbstractConceptPane wn = new DetailedConceptPane(nar, c);

        Stage st;
        Stage removed = window.put(c, st = newWindow(c.toString(), wn));
        st.setAlwaysOnTop(true); //? does this work
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

        return monoFonts.getIfAbsentPut(i, () -> {
            return Font.font("Monospaced", finalV);
        });
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


    /** run in FX context in separate thread */
    public static void run(Runnable r) {
        new Thread( () -> {
            run((a, b) -> r.run());
        }).start();
    }

    /* https://macdevign.wordpress.com/2014/03/27/running-javafx-application-instance-in-the-main-method-of-a-class/ */
    public static void run(AppLaunch appLaunch, String... sArArgs) {
        DummyApplication.appLaunch = appLaunch;
        Application.launch(DummyApplication.class, sArArgs);
    }

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

    public static void newConceptWindow(NAR nar, Pane container, String... concepts) {
        newConceptWindow(nar, container, (List<Concept>) of(concepts).map(
                nar::concept).collect(toList())
        );
    }

    private static void newConceptWindow(NAR nar, Pane v, List<? extends Termed> cc) {

        for (Termed c : cc) {
            AbstractConceptPane wn =
                    //new DetailedConceptPane(nar, c);
                    new SimpleConceptPane(nar, c);
            wn.setMaxWidth(250);
            wn.setPrefWidth(250);
            v.getChildren().add(wn);
        }

        v.layout();

        Stage st;
        Stage removed = window.put(cc, st = newWindow("Concepts", scrolled(v)));

        if (removed!=null)
            removed.close();

    }

    static NARide.FXCSSUpdater updater;
    static {
        StringProperty cssProp = new SimpleStringProperty("");
        updater = new NARide.FXCSSUpdater();
        updater.bindCss(cssProp);
        cssProp.setValue(NARfx.css);
    }

    /** retarded hack because this stylesheet stuff in JavaFX is designed pretty bad */
    public static void theme(Scene scene) {
        updater.applyCssToParent(scene);
    }

    // This must be public in order to instantiate successfully
    public static class DummyApplication extends Application {

        private static AppLaunch appLaunch;

        @Override
        public void start(Stage primaryStage) throws Exception {

            if (appLaunch != null) {
                appLaunch.start(this, primaryStage);
            }
        }

        @Override
        public void init() throws Exception {
            if (appLaunch != null) {
                appLaunch.init(this);
            }
        }

        @Override
        public void stop() throws Exception {
            if (appLaunch != null) {
                appLaunch.stop(this);
            }
        }
    }

    @FunctionalInterface
    public interface AppLaunch {
        void start(Application app, Stage stage) throws Exception;
        // Remove default keyword if you need to run in Java7 and below
        default void init(Application app) throws Exception {
        }

        default void stop(Application app) throws Exception {
        }
    }

}
