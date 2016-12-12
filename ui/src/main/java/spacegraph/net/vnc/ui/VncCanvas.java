package spacegraph.net.vnc.ui;

import io.netty.buffer.ByteBuf;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import spacegraph.net.vnc.rfb.codec.decoder.ColourMapEvent;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.render.ConnectInfoEvent;
import org.slf4j.LoggerFactory;
import spacegraph.net.vnc.rfb.render.rect.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Created by me on 12/5/16.
 */
public class VncCanvas implements BiConsumer<ServerDecoderEvent, ImageRect> {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncCanvas.class);

    public final ObjectProperty<WritableImage> vncImage = new SimpleObjectProperty<>(null);

//    private PointerEventHandler pointerHandler;
//    private CutTextEventHandler cutTextHandler;
//    private KeyButtonEventHandler keyHandler;

    private ImageCursor remoteCursor;

    private boolean useClientCursor;

    private final PixelFormat<ByteBuffer> DEFAULT_PIXELFORMAT = PixelFormat.getByteRgbInstance();

    private final AtomicReference<PixelFormat<ByteBuffer>> pixelFormat = new AtomicReference<>(DEFAULT_PIXELFORMAT);

    private SimpleDoubleProperty zoomLevel;

    public VncCanvas() {


//        setOnMouseEntered(event -> {
//            if (!isDisabled()) {
//                requestFocus();
//                setCursor(remoteCursor != null ? remoteCursor : Cursor.DEFAULT);
//            }
//        });
//
//        setOnMouseExited(event -> {
//            if (!isDisabled()) {
//                setCursor(Cursor.DEFAULT);
//            }
//        });
//
//        zoomLevelProperty().addListener(l -> {
//            if (getImage() != null) {
//                setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
//            }
//        });
    }

    public void setPixelFormat(ColourMapEvent event) {

        int[] colors = new int[event.getNumberOfColor()];
        ByteBuf c = event.getColors();
        for (int i = event.getFirstColor(); i < colors.length; i++) {
            colors[i] =
                    (0xff << 24) |
                            ((c.readUnsignedShort() >> 8) << 16) //R
                            | ((c.readUnsignedShort() >> 8) << 8) //G
                            | (c.readUnsignedShort() >> 8); //B
        }

        pixelFormat.set(PixelFormat.createByteIndexedInstance(colors));
    }

    @Override
    public void accept(ServerDecoderEvent event, ImageRect rect) {
        if (event instanceof ConnectInfoEvent) {
            setConnectInfoEvent((ConnectInfoEvent) event);
        } else if (event instanceof ColourMapEvent) {
            setPixelFormat((ColourMapEvent) event);
        }
        if (rect != null) {
            render(rect);
        }
    }

    private void render(ImageRect rect) {
        try {
            final WritableImage img = vncImage.get();

            if (img == null) {
                logger.error("canvas image has not been initialized");
                return;
            }
            switch (rect.getEncoding()) {
                case HEXTILE:
                    HextileImageRect hextileRect = (HextileImageRect) rect;
                    //PixelWriter writer = vncImage.getPixelWriter();
                    for (RawImageRect rawRect : hextileRect.getRects()) {
                        img.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                                rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    }
                    break;
                case RAW:
                case ZLIB:
                    RawImageRect rawRect = (RawImageRect) rect;
                    img.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                            rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    break;
                case COPY_RECT:
                    CopyImageRect copyImageRect = (CopyImageRect) rect;
                    PixelReader reader = img.getPixelReader();
                    WritableImage copyRect = new WritableImage(copyImageRect.width, copyImageRect.height);
                    copyRect.getPixelWriter().setPixels(0, 0, copyImageRect.width, copyImageRect.height, reader, copyImageRect.getSrcX(),
                            copyImageRect.getSrcY());
                    img.getPixelWriter().setPixels(copyImageRect.x, copyImageRect.y, copyImageRect.width, copyImageRect.height,
                            copyRect.getPixelReader(), 0, 0);
                    break;
                case CURSOR:
                    if (!useClientCursor) {
                        logger.warn("ignore cursor encoding");
                        return;
                    }
                    final CursorImageRect cRect = (CursorImageRect) rect;

                    if (cRect.height < 2 && cRect.width < 2) {
                        setCursor(Cursor.NONE);
                        return;
                    }

                    Dimension2D dim = ImageCursor.getBestSize(cRect.width, cRect.height);
                    WritableImage cImage = new WritableImage((int) dim.getWidth(), (int) dim.getHeight());
                    cImage.getPixelWriter().setPixels(0, 0, (int) Math.min(dim.getWidth(), cRect.width), (int) Math.min(dim.getHeight(), cRect.height),
                            PixelFormat.getIntArgbInstance(), cRect.getPixels().nioBuffer().asIntBuffer(), cRect.width);
                    remoteCursor = new ImageCursor(cImage, cRect.getHotspotX(), cRect.getHotspotY());
                    setCursor(remoteCursor);
                    break;
                case DESKTOP_SIZE:
                    logger.debug("resize image: {}", rect);
                    setImage(new WritableImage(rect.width, rect.height));
                    break;
                default:
                    logger.error("unsupported encoding {}", rect);
                    break;
            }
        } catch (Exception e) {
            logger.error("rect: {} {}", rect, e);
        } finally {
            rect.release();
        }
    }


    private void setCursor(ImageCursor remoteCursor) {


    }

    protected void setImage(WritableImage vncImage) {
        this.vncImage.set( vncImage );
    }

//    public void registerInputEventListener(InputEventListener listener) {
//        Objects.requireNonNull(listener, "input listener must not be null");
//        if (pointerHandler == null) {
//
//            pointerHandler = new PointerEventHandler();
//            pointerHandler.register(this);
//            pointerHandler.registerZoomLevel(zoomLevelProperty());
//            pointerHandler.enabledProperty().bind(disabledProperty().not());
//        }
//        pointerHandler.setInputEventListener(listener);
//
//        if (keyHandler == null) {
//            keyHandler = new KeyButtonEventHandler();
//            keyHandler.register(getScene());
//            keyHandler.enabledProperty().bind(disabledProperty().not());
//        }
//        keyHandler.setInputEventListener(listener);
//
//        if (cutTextHandler == null) {
//            cutTextHandler = new CutTextEventHandler();
//            cutTextHandler.enabledProperty().bind(disabledProperty().not());
//        }
//        cutTextHandler.setInputEventListener(listener);
//    }

//    public void unregisterInputEventListener() {
//        if (pointerHandler != null) {
//            pointerHandler.unregister(this);
//            pointerHandler = null;
//        }
//
//        if (keyHandler != null) {
//            keyHandler.unregister(getScene());
//            keyHandler = null;
//        }
//
//        if (cutTextHandler != null) {
//            cutTextHandler.setInputEventListener(null);
//            cutTextHandler = null;
//        }
//    }

    public DoubleProperty zoomLevelProperty() {
        if (zoomLevel == null) {
            zoomLevel = new SimpleDoubleProperty(1.0);
        }
        return zoomLevel;
    }

    public boolean isUseClientCursor() {
        return useClientCursor;
    }

    public void setUseClientCursor(boolean flag) {
        this.useClientCursor = flag;
        if (!useClientCursor) {
            setCursor(Cursor.DEFAULT);
        }
    }

    private void setCursor(Cursor aDefault) {

    }

//    public boolean addClipboardText(String text) {
//        if (cutTextHandler != null) {
//            cutTextHandler.addClipboardText(text);
//            return true;
//        }
//        return false;
//    }

    public void setConnectInfoEvent(ConnectInfoEvent e) {
        setImage(new WritableImage(e.getFrameWidth(), e.getFrameHeight()));
        //setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
        pixelFormat.set(DEFAULT_PIXELFORMAT);
    }

}


