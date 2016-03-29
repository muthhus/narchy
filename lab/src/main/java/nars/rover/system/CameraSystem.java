//package nars.rover.system;
//
//import com.artemis.BaseSystem;
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.OrthographicCamera;
//
///**
// * @author Daan van Yperen
// */
//public class CameraSystem extends BaseSystem {
//
//    public final OrthographicCamera camera;
//    public final OrthographicCamera guiCamera;
//
//    private static final float ZOOM = 0.5f;
//
//    public CameraSystem() {
//        camera = new OrthographicCamera(Gdx.graphics.getWidth() * ZOOM, Gdx.graphics.getHeight() * ZOOM);
//        camera.setToOrtho(false, Gdx.graphics.getWidth() * ZOOM, Gdx.graphics.getHeight() * ZOOM);
//        camera.update();
//
//        guiCamera = new OrthographicCamera(Gdx.graphics.getWidth() * ZOOM, Gdx.graphics.getHeight() * ZOOM);
//        guiCamera.setToOrtho(false, Gdx.graphics.getWidth() * ZOOM, Gdx.graphics.getHeight() * ZOOM);
//        guiCamera.update();
//    }
//
//    @Override
//    protected void processSystem() {
//
//    }
//}
