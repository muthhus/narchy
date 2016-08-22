package nars.guifx;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import nars.NAR;
import nars.guifx.chart.Plot2D;
import nars.guifx.util.NSlider;
import nars.op.in.NQuadsRDF;
import nars.time.RealtimeMSClock;
import nars.util.Texts;
import nars.util.event.Active;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;

import static javafx.application.Platform.runLater;

/**
 * small VBox vertically oriented component which can be attached
 * to the left or right of anything else, which contains a set of
 * buttons for controlling a nar
 */
public class NARMenu extends HBox {


    //private final NARWindow.FXReaction busyBackgroundColor;


    final NAR nar;

    public final Menu tool;
    public final Menu main;


    public NARMenu(NAR n) {

        nar = n;
        //Canvas canvas = new NARWindow.ResizableCanvas(this);
        //canvas.maxWidth(Double.MAX_VALUE);
        //canvas.maxHeight(Double.MAX_VALUE);


        //b.getChildren().add(new Separator(Orientation.HORIZONTAL));


        //b.getChildren().add(new Separator(Orientation.HORIZONTAL));

        NSlider fontSlider = new NSlider(25.0f, 25.0f, 0.5);
        //getChildren().add(0, fontSlider);
        fontSlider.value[0].addListener((a, b, c) -> runLater(() -> {
            double pointSize = 6 + 12 * c.doubleValue();
            getScene().getRoot().setStyle("-fx-font-size: " + pointSize + "pt;");
            //+ 100*(0.5 + c.doubleValue()) + "%");
        }));
        fontSlider.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2) {
                //double click
                System.out.println("double click fontSlider");
            }
        });

        Button iconButton = JFX.newIconButton(FontAwesomeIcon.GEAR);
        iconButton.setMouseTransparent(true);

        main = new Menu("", iconButton);

        main.getStyleClass().add("nar_main_menu");
        main.getItems().add(new MenuItem("", fontSlider));
        main.getItems().add(new MenuItem("New..."));

        Menu loadMenu;
        main.getItems().add(loadMenu = new Menu("Load..."));
        loadMenu.getItems().add(new AsyncMenuItem(n, ".n3 RDF") {
            @Override public void run(NAR n) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Load RDF File");
                fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("n3","n4","turtle","rdf" /* .. */));
                File f = fileChooser.showOpenDialog(null);
                if (f!=null) {
                    try {
                        NQuadsRDF.input(n, f);
                    } catch (Exception e) {
                        //n.eventError.emit(e);
                        e.printStackTrace();
                    }
                }
            }
        });

        main.getItems().add(new MenuItem("Save..."));
        main.getItems().add(new MenuItem("Fork..."));
        main.getItems().add(new MenuItem("Discard..."));
        main.getItems().add(new SeparatorMenuItem());
        main.getItems().add(new MenuItem("Exit..."));

        Button button2 = JFX.newIconButton(FontAwesomeIcon.NAVICON);
        button2.setMouseTransparent(true);
        tool = new Menu("", button2);

        getChildren().add(new MenuBar(main, tool));


        //getChildren().add(threadControl = new CycleClockPane(nar));

//        if (memoryButtons) {
//            Button b0 = JFX.newIconButton(FontAwesomeIcon.FOLDER);
//            b0.setTooltip(new Tooltip("Open"));
//            getChildren().add(b0);
//
//            Button b1 = JFX.newIconButton(FontAwesomeIcon.SAVE);
//            b1.setTooltip(new Tooltip("Save"));
//            getChildren().add(b1);
//
//            Button b2 = JFX.newIconButton(FontAwesomeIcon.CODE_FORK);
//            b2.setTooltip(new Tooltip("Clone"));
//            getChildren().add(b2);
//        }

//        if (guiButtons) {
//            consoleButton = JFX.newToggleButton(FontAwesomeIcon.CODE);
//            consoleButton.setTooltip(new Tooltip("I/O..."));
//            getChildren().add(consoleButton);
////            consoleButton.setOnAction(e -> {
////                onConsole(consoleButton.isSelected());
////            });
//
////            Button bo = newIconButton(FontAwesomeIcon.TACHOMETER);
////            bo.setTooltip(new Tooltip("Output..."));
////            v.getChildren().add(bo);
//        } else {
//            consoleButton = null;
//        }
//
//
//        getChildren().forEach(c -> {
//            if (c instanceof Control)
//                ((Control) c).setMaxWidth(Double.MAX_VALUE);
//        });
//        setMaxWidth(Double.MAX_VALUE);
        //setFillHeight(true);


//        this.busyBackgroundColor = new NARWindow.FXReaction(n, this, Events.FrameEnd.class) {
//
//            @Override
//            public void event(Class event, Object[] args) {
//
//                if (event == Events.FrameEnd.class) {
//                    Platform.runLater(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            float b = 0, h = 0;
//
//                            if (n.isRunning()) {
//                                b = n.memory.emotion.busy();
//                                h = n.memory.emotion.happy();
//                            }
//
//                            if ((canvas.getWidth()!=getWidth()) || (canvas.getHeight()!=getHeight()))
//                                canvas.resize(Double.MAX_VALUE, Double.MAX_VALUE);
//
//                            GraphicsContext g = canvas.getGraphicsContext2D();
//                            g.setFill(new javafx.scene.paint.Color(0.25 * b, 0.25 * h, 0, 1.0));
//                            g.fillRect(0, 0, getWidth(), getHeight());
//
//                        }
//                    });
//
//                }
//
//            }
//        };

        //threadControl.run();

    }


    public static class RTClockPane extends CycleClockPane {

        private final RealtimeMSClock clock;
        private final TextField clockTextField;
        private final Plot2D busyPlot;
        final long startTime;
        final static int WINDOW_SIZE = 32;

        final DescriptiveStatistics busyAvgShort = new DescriptiveStatistics(WINDOW_SIZE);
        final DescriptiveStatistics busyAvgLong = new DescriptiveStatistics(WINDOW_SIZE*8);

        public RTClockPane(NAR nar) {
            super(nar);


            this.clock = (RealtimeMSClock) nar.clock;
            this.startTime = nar.time();

            clockTextField = new TextField();
            clockTextField.setEditable(false);


            busyPlot = new Plot2D(Plot2D.Line, WINDOW_SIZE, 192);
            busyPlot.add("Busy", nar.emotion.busy::getSum);
            //busyPlot.add("avg short", ()->rollingAverage(busyAvgShort, (double) nar.emotion.busy()));
            //busyPlot.add("avg long", ()->rollingAverage(busyAvgLong, (double) nar.emotion.busy.()));

//            final Pane controls = new VBox(
//                    timeDisplay,
//                    new NSlider("Power", 32, 32, NSlider.BarSlider, 0.5),
//                    new NSlider("Duration", 32, 32, NSlider.BarSlider, 0.75),
//                    new NSlider("Focus", 32, 32, NSlider.BarSlider, 0.6),
//                    new FlowPane(new Button("Relax"), new Button("Random"))
//            );


            getChildren().setAll(
                    new VBox(clockTextField, busyPlot)
            );

            nar.onFrame(n -> {
                busyPlot.update();
                String s = clockText();
                runLater(()->display(s));
            });
            display(clockText());
        }

        public static double rollingAverage(DescriptiveStatistics var, double newValue) {
            var.addValue(newValue);
            return var.getMean();
        }

        private void display(String text) {
            clockTextField.setText(clockText());
        }

        private String clockText() {
            return Texts.n4((clock.time()-startTime)/1000f) + 's';
        }
    }


    public static class CycleClockPane extends VBox implements Runnable {


        private final NAR nar;
        private final Active regs;
        //final AtomicBoolean pendingClockUpdate = new AtomicBoolean(false);
        ////TODO: public final SimpleBooleanProperty pendingClockUpdate


        @Override
        public void run() {
//            if (pendingClockUpdate.compareAndSet(false, true))

//                runLater(() -> {
//                    pendingClockUpdate.set(false);
//                    boolean running = nar.running();
//                    if (running != wasRunning) {
//                        //bp.setGraphic(running ? stop : play);
//                        wasRunning = running;
//                    }
//
//
//                });

        }

        public CycleClockPane(NAR n) {

            getStyleClass().add("thread_control");

            setAlignment(Pos.CENTER_LEFT);
            //setColumnHalignment(HPos.RIGHT);

            nar = n;

            regs = new Active().add(
                    n.eventFrameStart.on(nn -> {
                        //System.out.println("frame: " + nn.time());
                        run();
                    }),
                    n.eventReset.on(nn -> run())
            );


            autosize();
        }

    }


    abstract static class AsyncMenuItem extends MenuItem {

        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public AsyncMenuItem(NAR n, String label) {
            super(label);
            setOnAction((e) -> run(n));
        }

        public abstract void run(NAR n);
    }
}
