package spacegraph.net.vnc.ui.control;

import io.netty.buffer.ByteBuf;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.slf4j.LoggerFactory;
import spacegraph.net.vnc.rfb.codec.decoder.ColourMapEvent;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.codec.encoder.InputEventListener;
import spacegraph.net.vnc.rfb.codec.encoder.KeyButtonEvent;
import spacegraph.net.vnc.rfb.render.ConnectInfoEvent;
import spacegraph.net.vnc.rfb.render.rect.*;
import spacegraph.net.vnc.ui.CutTextEventHandler;
import spacegraph.net.vnc.ui.KeyButtonEventHandler;
import spacegraph.net.vnc.ui.PointerEventHandler;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class VncImageView extends ImageView implements BiConsumer<ServerDecoderEvent, ImageRect> {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncImageView.class);

    private WritableImage image;

    public PointerEventHandler pointer;
    public CutTextEventHandler cutText;
    public KeyButtonEventHandler keys;

    private ImageCursor remoteCursor;

    private boolean useClientCursor;

    private final PixelFormat<ByteBuffer> DEFAULT_PIXELFORMAT = PixelFormat.getByteRgbInstance();

    private final AtomicReference<PixelFormat<ByteBuffer>> pixelFormat = new AtomicReference<>(DEFAULT_PIXELFORMAT);

    private SimpleDoubleProperty zoomLevel;

    public VncImageView() {
        setPreserveRatio(true);
        registerListener();
    }


    public void registerListener() {

        setOnMouseEntered(event -> {
            if (!isDisabled()) {
                requestFocus();
                setCursor(remoteCursor != null ? remoteCursor : Cursor.DEFAULT);
            }
        });

        setOnMouseExited(event -> {
            if (!isDisabled()) {
                setCursor(Cursor.DEFAULT);
            }
        });

        zoomLevelProperty().addListener(l -> {
            if (getImage() != null) {
                setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
            }
        });
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
            Platform.runLater(() -> setConnectInfoEvent((ConnectInfoEvent) event));
        } else if (event instanceof ColourMapEvent) {
            Platform.runLater(() -> setPixelFormat((ColourMapEvent) event));
        }
        if (rect != null) {
            Platform.runLater(() -> render(rect));
        }
    }

    private void render(ImageRect rect) {
        try {
            if (image == null) {
                logger.error("canvas image has not been initialized");
                return;
            }
            switch (rect.getEncoding()) {
                case HEXTILE:
                    HextileImageRect hextileRect = (HextileImageRect) rect;
                    //PixelWriter writer = vncImage.getPixelWriter();
                    for (RawImageRect rawRect : hextileRect.getRects()) {
                        image.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                                rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    }
                    break;
                case RAW:
                case ZLIB:
                    RawImageRect rawRect = (RawImageRect) rect;
                    image.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                            rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    break;
                case COPY_RECT:
                    CopyImageRect copyImageRect = (CopyImageRect) rect;
                    PixelReader reader = image.getPixelReader();
                    WritableImage copyRect = new WritableImage(copyImageRect.width, copyImageRect.height);
                    copyRect.getPixelWriter().setPixels(0, 0, copyImageRect.width, copyImageRect.height, reader, copyImageRect.getSrcX(),
                            copyImageRect.getSrcY());
                    image.getPixelWriter().setPixels(copyImageRect.x, copyImageRect.y, copyImageRect.width, copyImageRect.height,
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
                    image = new WritableImage(rect.width, rect.height);
                    setImage(image);
                    break;
                default:
                    logger.error("not supported encoding rect: {}", rect);
                    break;
            }
        } catch (Exception e) {
            logger.error("rect: {} {}", rect, e);
        } finally {
            rect.release();
        }

        renderOverlay(image);
    }


    private void renderOverlay(WritableImage image) {
        //TODO
    }

    public void registerInputEventListener(InputEventListener listener) {
        Objects.requireNonNull(listener, "input listener must not be null");
        if (pointer == null) {

            pointer = new PointerEventHandler();
            pointer.register(this);
            pointer.registerZoomLevel(zoomLevelProperty());
            pointer.enabledProperty().bind(disabledProperty().not());
        }
        pointer.setInputEventListener(listener);

        if (keys == null) {
            keys = new KeyButtonEventHandler() {
                @Override
                protected void fire(KeyButtonEvent msg) {
                    super.fire(msg);
                    onFired(msg);
                }
            };
            keys.register(getScene());
            keys.enabledProperty().bind(disabledProperty().not());
        }
        keys.setInputEventListener(listener);

        if (cutText == null) {
            cutText = new CutTextEventHandler();
            cutText.enabledProperty().bind(disabledProperty().not());
        }
        cutText.setInputEventListener(listener);
    }

    protected void onFired(KeyButtonEvent msg) {

    }

    public void unregisterInputEventListener() {
        if (pointer != null) {
            pointer.unregister(this);
            pointer = null;
        }

        if (keys != null) {
            keys.unregister(getScene());
            keys = null;
        }

        if (cutText != null) {
            cutText.setInputEventListener(null);
            cutText = null;
        }
    }

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

    public boolean addClipboardText(String text) {
        if (cutText != null) {
            cutText.addClipboardText(text);
            return true;
        }
        return false;
    }

    public void setConnectInfoEvent(ConnectInfoEvent e) {
        setImage(image = new WritableImage(e.getFrameWidth(), e.getFrameHeight()));
        setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
        pixelFormat.set(DEFAULT_PIXELFORMAT);
    }

}

