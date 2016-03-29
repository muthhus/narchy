package mdi;

import br.com.supremeforever.mdi.MDICanvas;
import br.com.supremeforever.mdi.MDIWindow;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import nars.guifx.NARfx;

import static br.com.supremeforever.mdi.MDICanvas.resourceURL;

/**
 * Created by brisatc171.minto on 12/11/2015.
 */
public class ExampleMDI extends Application {
    int count;
    public static HostServices hostServices;

    @Override
    public void start(Stage primaryStage) throws Exception {
        hostServices = getHostServices();
        //Creat main Pane Layout
        AnchorPane mainPane = new AnchorPane();
        mainPane.setPrefSize(300, 200);
        //Creat MDI Canvas Container
        MDICanvas mdiCanvas = new MDICanvas();
        //Fit it to the main Pane
        AnchorPane.setBottomAnchor(mdiCanvas, 0d);
        AnchorPane.setLeftAnchor(mdiCanvas, 0d);
        AnchorPane.setTopAnchor(mdiCanvas, 25d);//Button place
        AnchorPane.setRightAnchor(mdiCanvas, 0d);
        //Put the container Into the main pane
        mainPane.getChildren().add(mdiCanvas);
        //Create a 'New MDI Window' Button
        Button btnDefaultMdi = new Button("New Window");
        //set the button action

        btnDefaultMdi.setOnAction(event -> {
            Node content = null;
            try {
                content = FXMLLoader.load(resourceURL("mdi/MyContent.fxml"));
                count++;
                //Create a Default MDI Withou Icon
                MDIWindow mdiWindow = new MDIWindow("UniqueID" + count,
                        new ImageView(resourceURL("mdi/WindowIcon.png").toString()),
                        "Title " + count,
                        content);
                        //scrolled(content, true, false, true, false));
                //Set MDI Size
                //Add it to the container
                mdiCanvas.newWindow(mdiWindow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        //Put it into the main pane
        mainPane.getChildren().add(btnDefaultMdi);

        primaryStage.setScene(new Scene(mainPane));
        NARfx.theme(primaryStage.getScene());
        primaryStage.show();
    }

    public static void main(String[] arg) {
        Application.launch(ExampleMDI.class);
    }
}
