package nars.guifx.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;
import nars.$;
import nars.guifx.NARfx;
import nars.guifx.Spacegraph;
import nars.guifx.nars.NARNotes;
import nars.guifx.terminal.Console;
import nars.guifx.util.CodeInput;
import nars.guifx.util.Windget;
import nars.nar.Default;
import nars.util.FX;
import za.co.knonchalant.builder.POJONode;

import static javafx.application.Platform.runLater;
import static nars.util.FX.scrolled;


public class DemoNARNotes {

    public static void main(String[] args) {

        Default d = new Default();

        FX.run(()->{
            //DemoSpacegraph s = new DemoSpacegraph();
            NARNotes s = new NARNotes(d);

            Stage w = new Stage();
            w.setScene(new Scene(s));
            //Stage w = FX.newWindow("x", s, 800, 600);
            //w.setFullScreen(false);


            NARfx.theme(s.getScene());
            //s.getStylesheets().addAll(Spacegraph.spacegraphCSS);
            //s.getScene().getStylesheets().add(Spacegraph.spacegraphCSS);

            w.setWidth(900);
            w.setHeight(800);

            w.show();


            runLater(()->{
                d.believe("a:b");
                d.believe("b:c");
                d.believe("c:d");
            });
        });

    }

    //final Spacegraph space = new Spacegraph();

    public static class DemoSpacegraph extends Spacegraph {

        public DemoSpacegraph() {
            //BrowserWindow.createAndAddWindow(space, "http://www.google.com");


            ObservableList<PieChart.Data> pieChartData =
                    FXCollections.observableArrayList(
                            new PieChart.Data("Grapefruit", 13),
                            new PieChart.Data("Orange", 25),
                            new PieChart.Data("Human", 10),
                            new PieChart.Data("Pear", 22),
                            new PieChart.Data("Apple", 30));
            PieChart chart = new PieChart(pieChartData);
            chart.setTitle("Invasive Species");
            chart.setCacheHint(CacheHint.SPEED);

            //cc.addOverlay(new Windget.RectPort(cc, true, +1, -1, 30, 30));

            //wc.addOverlay(new Windget.RectPort(wc, true, -1, +1, 30, 30));


            //Region jps = new FXForm(new NAR(new Default()));  // create the FXForm node for your bean


//            TaggedParameters taggedParameters = new TaggedParameters();
//            List<String> range = new ArrayList<>();
//            range.add("Ay");
//            range.add("Bee");
//            range.add("See");
//            taggedParameters.addTag("range", range);
//            Pane jps = POJONode.build(new SampleClass(), taggedParameters);

//        Button button = new Button("Read in");
//        button.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent actionEvent) {
//                //SampleClass sample = POJONode.read(mainPane, SampleClass.class);
//                //System.out.println(sample.getTextString());
//            }
//        });
//
//            jps.setStyle("-fx-font-size: 75%");
//            Windget wd = new Windget("WTF",
//                    jps,
//                    //new Button("XYZ"),
//                    400, 400);
//            wd.addOverlay(new Windget.RectPort(wc, true, 0, +1, 10, 10));


            addNodes(
                    new Windget("Chart", chart, 400, 400),
                    new Windget("Edit", new CodeInput("ABC"), 300, 200)
                            .move(-100,-100),
                    new Windget("Console", new Console(), 300, 200)
                            .move(300,-100)
                    //new Windget("NAR",
                    //new IOPane(new Default2(512,8,4,2)), 200, 200).move(-200,300)
            );

            for (int i = 0; i < 4; i++) {
                addNodes( new Windget("x" + i,
                        POJONode.build(
                                //new POJOPane(
                                $.$("<a --> " + i + '>'))) );
            }

            ground.getChildren().add(new GridCanvas(true));

            //new HyperOrganicLayout().run(verts, 10);



        }
    }


}
