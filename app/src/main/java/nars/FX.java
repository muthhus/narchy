package nars;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * JavaFX Util
 */
public class FX {

    //Prism Renderer options - http://adamish.com/blog/archives/320
    static {
        System.setProperty("javafx.animation.framerate", "30");
        System.setProperty("prism.verbose", "true");
        //System.setProperty("prism.dirtyopts", "false");
        //System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "30"); //by default this is 60
        //System.setProperty("prism.forceGPU", "true");
    }


    /** run in FX context in separate thread */
    public static void run(Runnable r) {
        Thread t = new Thread( () -> {
            run((a, b) -> r.run());
        });
        t.start();
    }

    /* https://macdevign.wordpress.com/2014/03/27/running-javafx-application-instance-in-the-main-method-of-a-class/ */
    public static void run(AppLaunch appLaunch, String... sArArgs) {
        DummyApplication.appLaunch = appLaunch;
        Application.launch(DummyApplication.class, sArArgs);
    }


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
        return newWindow(title, n, 0, 0);
    }

    public static Stage newWindow(String title, Region n, double w, double h) {

        if (w == 0) {
            n.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            n.autosize();
        } else {
            n.prefWidth(w);
            n.prefHeight(h);
        }

        Scene scene = new Scene(n);
        //theme(scene);

        Stage s = new Stage();
        s.setTitle(title);
        s.setScene(scene);

        //n.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        s.show();

        return s;
    }


    public static Stage newWindow(Scene scene, Stage stage) {
        stage.setScene(scene);
        //theme(scene);


        //scene.getRoot().maxWidth(Double.MAX_VALUE);
        //scene.getRoot().maxHeight(Double.MAX_VALUE);
        return stage;
    }


    public static Stage newWindow(String title, Scene scene) {
        Stage s = new Stage();
        s.setTitle(title);
        return newWindow(scene, s);
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

//    public static void newWindow(NAR nar) {
//        NARide.show(nar.loop(), (Consumer)null);
//    }

}
