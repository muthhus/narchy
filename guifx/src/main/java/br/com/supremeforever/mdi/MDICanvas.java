package br.com.supremeforever.mdi;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by brisatc171.minto on 12/11/2015.
 */
public class MDICanvas extends AnchorPane {


    /**
     * @param mainPane
     * @return
     */
    public static MDIWindow getMDIWindow(Node mainPane){
        MDIWindow mdiW = (MDIWindow) mainPane.getParent().getParent();
        return mdiW;
    }

    private static class WidthChangeListener implements ChangeListener {

        private final MDICanvas mdi;
        private final MDIWindow window;

        public WidthChangeListener(MDICanvas mdi, MDIWindow window) {
            this.mdi = mdi;
            this.window = window;
        }

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            this.mdi.centerMdiWindow(this.window);
            observable.removeListener(this);
        }
    }

    //public final HBox taskBar;


    /**
     * *********** CONSTRUICTOR *************
     */
    public MDICanvas() {
        super();

        setStyle(null);
        //setMouseTransparent(true);
        setPickOnBounds(false);

        //maxWidth(Double.MAX_VALUE);
        //maxHeight(Double.MAX_VALUE);


        //setAlignment(Pos.TOP_LEFT);


        //windows.setId("MDIContainer");
        //windows.getStyleClass().add("mdiCanvasContainer");
//        tbWindows = new HBox();
//        taskBar.setSpacing(3);
//        taskBar.setMaxHeight(taskbarHeightWithoutScrool);
//        taskBar.setMinHeight(taskbarHeightWithoutScrool);
//        taskBar.setAlignment(Pos.CENTER_LEFT);
        //setVgrow(container, Priority.ALWAYS);
//        taskBar = new ScrollPane(tbWindows);
        //taskBar = new HBox();
//        Platform.runLater(() -> {
//            Node viewport = taskBar.lookup(".viewport");
//            try {
//                viewport.setStyle(" -fx-background-color: transparent; ");
//            } catch (Exception e) {
//            }
//        });
//        taskBar.setMaxHeight(taskbarHeightWithoutScrool);
//        taskBar.setMinHeight(taskbarHeightWithoutScrool);
//        taskBar.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//        taskBar.setVmax(0);
//        taskBar.getStyleClass().add("taskBar");
        //taskBar.styleProperty().bind(StylesCSS.taskBarStyleProperty);

//        taskBar.widthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
//            Platform.runLater(() -> {
//
//                if ((double) newValue <= t.getWidth()) {
//                    t.setMaxHeight(taskbarHeightWithoutScrool);
//                    t.setPrefHeight(taskbarHeightWithoutScrool);
//                    t.setMinHeight(taskbarHeightWithoutScrool);
//                } else {
//                    t.setMaxHeight(taskbarHeightWithScrool);
//                    t.setPrefHeight(taskbarHeightWithScrool);
//                    t.setMinHeight(taskbarHeightWithScrool);
//                }
//            });
//        });

        addEventHandler(MDIEvent.EVENT_CLOSED, mdiCloseHandler);
        addEventHandler(MDIEvent.EVENT_MINIMIZED, mdiMinimizedHandler);

        //getChildren().addAll(taskBar);
    }

//    /**
//     * *************************REMOVE_WINDOW******************************
//     */
//    public void removeMDIWindow(String mdiWindowID) {
//        Node mdi = getItemFromMDIContainer(mdiWindowID);
//        Node iconBar = getItemFromToolBar(mdiWindowID);
//
//        if (mdi != null) {
//            getItemFromMDIContainer(mdiWindowID).isClosed(true);
//
//            this.getChildren().remove(mdi);
//        }
////        if (iconBar != null) {
////            taskBar.getChildren().remove(iconBar);
////        }
//    }

    /**
     * *****************************ADD_WINDOW*********************************
     */
    public void newWindow(MDIWindow mdiWindow) {
        if (getItemFromMDIContainer(mdiWindow.getId()) == null) {
            addNew(mdiWindow, null);
        } else {
            restoreExisting(mdiWindow);
        }
    }

    public void newWindow(MDIWindow mdiWindow, Point2D position) {
        if (getItemFromMDIContainer(mdiWindow.getId()) == null) {
            addNew(mdiWindow, position);
        } else {
            restoreExisting(mdiWindow);
        }
    }

    private void addNew(MDIWindow mdiWindow, Point2D position) {
        mdiWindow.setVisible(false);
        this.getChildren().add(mdiWindow);
        if (position == null) {
            //mdiWindow.layoutBoundsProperty().addListener(new WidthChangeListener(this, mdiWindow));
            position = new Point2D(10,10); //HACK
        }

        this.position(mdiWindow, position);

        mdiWindow.toFront();
    }

    private void restoreExisting(MDIWindow mdiWindow) {
        //if (getItemFromToolBar(mdiWindow.getId()) != null) {
            //taskBar.getChildren().remove(getItemFromToolBar(mdiWindow.getId()));
        //}
        for (int i = 0; i < this.getChildren().size(); i++) {
            Node node = this.getChildren().get(i);
            if (node.getId().equals(mdiWindow.getId())) {
                node.toFront();
                node.setVisible(true);
            }
        }
    }

    /**
     * *****************************MDI_EVENT_HANDLERS**************************
     */
    public final EventHandler<MDIEvent> mdiCloseHandler = new MDICloseHandler();
    public final EventHandler<MDIEvent> mdiMinimizedHandler = new MDIMinimizeHandler();

//    /**
//     * ***************** UTILITIES******************************************
//     */
//    public final MDIIcon getItemFromToolBar(String id) {
//        for (Node node : taskBar.getChildren()) {
//            if (node instanceof MDIIcon) {
//                MDIIcon icon = (MDIIcon) node;
//                //String key = icon.getLblName().getText();
//                String key = icon.getId();
//                if (key.equalsIgnoreCase(id)) {
//                    return icon;
//                }
//            }
//        }
//        return null;
//    }

    public final MDIWindow getItemFromMDIContainer(String id) {
        for (Node node : this.getChildren()) {
            if (node instanceof MDIWindow) {
                MDIWindow win = (MDIWindow) node;
                if (win.getId().equals(id)) {

                    return win;
                }
            }
        }
        return null;
    }

//    public static void setTheme(Scene scene) {
//        File f = null;
////        switch (theme) {
////            case DEFAULT:
//////                try {
////        try {
////            f = new File(resourceURI("mdi/DarkTheme.css").toURL().openStream());
////            scene.getStylesheets().clear();
////            scene.getStylesheets().add("file:///" + f.getAbsolutePath().replace("\\", "/"));
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//
////                break;
////            case DARK:
////                try {
////                    f = new File(resourceURI("mdi/DarkTheme.css"));
////                    scene.getStylesheets().clear();
////                    scene.getStylesheets().add(f.toString());
////                } catch (URISyntaxException e) {
////                    e.printStackTrace();
////                }
////                break;
////        }
//    }

    @NotNull
    public static URI resourceURI(String path) throws URISyntaxException {
        return resourceURL(path).toURI();
    }

    public static @NotNull URL resourceURL(String path) {
        return MDICanvas.class.getClassLoader().getResource(path);
    }

    public void position(MDIWindow mdiWindow, MDIWindow.AlignPosition alignPosition) {
        Bounds cb = this.getLayoutBounds();
        double canvasH = cb.getHeight();
        double canvasW = cb.getWidth();
        Bounds mb = mdiWindow.getLayoutBounds();
        double mdiH = mb.getHeight();
        double mdiW = mb.getWidth();

        switch (alignPosition) {
            case CENTER:
                centerMdiWindow(mdiWindow);
                break;
            case CENTER_LEFT:
                position(mdiWindow, new Point2D(0, (int) (canvasH / 2) - (int) (mdiH / 2)));
                break;
            case CENTER_RIGHT:
                position(mdiWindow, new Point2D((int) canvasW - (int) mdiW, (int) (canvasH / 2) - (int) (mdiH / 2)));
                break;
            case TOP_CENTER:
                position(mdiWindow, new Point2D((int) (canvasW / 2) - (int) (mdiW / 2), 0));
                break;
            case TOP_LEFT:
                position(mdiWindow, Point2D.ZERO);
                break;
            case TOP_RIGHT:
                position(mdiWindow, new Point2D((int) canvasW - (int) mdiW, 0));
                break;
            case BOTTOM_LEFT:
                position(mdiWindow, new Point2D(0, (int) canvasH - (int) mdiH));
                break;
            case BOTTOM_RIGHT:
                position(mdiWindow, new Point2D((int) canvasW - (int) mdiW, (int) canvasH - (int) mdiH));
                break;
            case BOTTOM_CENTER:
                position(mdiWindow, new Point2D((int) (canvasW / 2) - (int) (mdiW / 2), (int) canvasH - (int) mdiH));
                break;
        }
    }

    public void position(MDIWindow mdiWindow, Point2D point) {
        Bounds mb = mdiWindow.getLayoutBounds();
        double windowsWidth = mb.getWidth();
        double windowsHeight = mb.getHeight();
        mdiWindow.setPrefSize(windowsWidth, windowsHeight);

        Bounds cb = this.getLayoutBounds();
        double containerWidth = cb.getWidth();
        double containerHeight = cb.getHeight();
        if (containerWidth <= point.getX() || containerHeight <= point.getY()) {
            throw new PositionOutOfBoundsException(
                    "Tried to place MDI Window with ID " + mdiWindow.getId() +
                            " at a coordinate " + point.toString() +
                            " that is beyond current size of the MDI container " +
                            containerWidth + "px x " + containerHeight + "px."
            );
        }

        int borderSensitivityExclusionZone = 40;
        if ((Math.abs(containerWidth - point.getX()) < borderSensitivityExclusionZone) ||
                (Math.abs(containerHeight - point.getY()) < borderSensitivityExclusionZone)) {
            throw new PositionOutOfBoundsException(
                    "Tried to place MDI Window with ID " + mdiWindow.getId() +
                            " at a coordinate " + point.toString() +
                            " that is too close to the edge of the parent of size " +
                            containerWidth + "px x " + containerHeight + "px " +
                            " for user to comfortably grab the title bar with the mouse."
            );
        }

        mdiWindow.setLayoutX(point.getX());
        mdiWindow.setLayoutY(point.getY());
        mdiWindow.setVisible(true);
    }

    public void centerMdiWindow(MDIWindow mdiWindow) {
        double w = this.getLayoutBounds().getWidth();
        double h = this.getLayoutBounds().getHeight();

        Platform.runLater(() -> {
            double windowsWidth = mdiWindow.getLayoutBounds().getWidth();
            double windowsHeight = mdiWindow.getLayoutBounds().getHeight();

            Point2D centerCoordinate = new Point2D(
                    (int) (w / 2) - (int) (windowsWidth / 2),
                    (int) (h / 2) - (int) (windowsHeight / 2)
            );
            this.position(mdiWindow, centerCoordinate);
        });
    }

    public enum Theme {

        DEFAULT,
        DARK,
    }

    private static class MDIMinimizeHandler implements EventHandler<MDIEvent> {
        @Override
        public void handle(MDIEvent event) {
            MDIWindow win = (MDIWindow) event.getTarget();
            String id = win.getId();
            /*if (getItemFromToolBar(id) == null) {
                try {
                    MDIIcon icon = new MDIIcon(event.imgLogo, (MDICanvas) MDICanvas.this,
                            win.getWindowsTitle());
                    icon.setId(win.getId());
                    icon.getBtnClose().disableProperty().bind(win.getBtnClose().disableProperty());
                    //taskBar.getChildren().add(icon);
                } catch (Exception ex) {
                    Logger.getLogger(MDICanvas.class.getName()).log(Level.SEVERE, null, ex);
                }

            }*/
        }
    }

    private static class MDICloseHandler implements EventHandler<MDIEvent> {
        @Override
        public void handle(MDIEvent event) {
            MDIWindow win = (MDIWindow) event.getTarget();
            //taskBar.getChildren().remove(getItemFromToolBar(win.getId()));
        }
    }
}
