/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package nars.video;

import com.airhacks.afterburner.injection.Injector;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.LEDataInputStream;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderPane;
import nars.FX;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.codec.encoder.KeyButtonEvent;
import spacegraph.net.vnc.rfb.codec.security.SecurityType;
import spacegraph.net.vnc.rfb.render.rect.ImageRect;
import spacegraph.net.vnc.ui.VncCanvas;
import spacegraph.net.vnc.ui.control.VncImageView;
import spacegraph.net.vnc.ui.service.VncRenderService;
import spacegraph.render.Draw;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static nars.FX.scrolled;

/**
 * /usr/bin/qemu-system-x86_64 -boot c  -m 512 -hda '/home/me/img/Linux.qcow' -cdrom  '/home/me/Downloads/cm-x86-13.0-rc1.iso' -net nic,vlan=0 -net user,vlan=0 -localtime -vnc :1 -monitor stdio
 */
public class VncClient {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncClient.class);
    private final VncCanvas renderer;
    private final VncRenderService vncService;


    private PixelReader vncReader;
    public WritableImage image;



    public static Texture tgaTexture(int width, int height, boolean alpha, byte[] data) {


        Texture tt;
        try {
            TGAImage tga = TGAImage.createFromData(width, height, alpha, true, ByteBuffer.wrap(data));
            tt = TextureIO.newTexture(new ByteArrayInputStream(tga.output().array()), true, TextureIO.TGA);
        } catch (IOException e) {
            e.printStackTrace();
            tt = null;
        }
        return tt;
    }

    static class MyByteArrayOutputStream extends ByteArrayOutputStream {
        public MyByteArrayOutputStream(int capacity) {
            super(capacity );
        }

        public byte[] array() { return buf; }
    }

        /**
     * Targa image reader and writer adapted from sources of the <a href =
     * "http://java.sun.com/products/jimi/">Jimi</a> image I/O class library.
     *
     * <P>
     *
     * Image decoder for image data stored in TGA file format.
     * Currently only the original TGA file format is supported. This is
     * because the new TGA format has data at the end of the file, getting
     * to the end of a file in an InputStream orient environment presents
     * several difficulties which are avoided at the moment.
     *
     * <P>
     *
     * This is a simple decoder and is only setup to load a single image
     * from the input stream
     *
     * <P>
     *
     * @author    Robin Luiten
     * @author    Kenneth Russell
     * @version    $Revision: 1768 $
     */
    public static class TGAImage {
        private final TGAImage.Header header;
        private int    format;
        private int    bpp;
        private ByteBuffer data;

        private TGAImage(final TGAImage.Header header) {
            this.header = header;
        }


        /**
         * This class reads in all of the TGA image header in addition it also
         * reads in the imageID field as it is convenient to handle that here.
         *
         * @author    Robin Luiten
         * @version   1.1
         */
        public static class Header {
            /** Set of possible file format TGA types */
            public final static int TYPE_NEW = 0;
            public final static int TYPE_OLD = 1;
            public final static int TYPE_UNK = 2;               // cant rewind stream so unknown for now.

            /**  Set of possible image types in TGA file */
            public final static int NO_IMAGE = 0;               // no image data
            public final static int UCOLORMAPPED = 1;           // uncompressed color mapped image
            public final static int UTRUECOLOR = 2;             // uncompressed true color image
            public final static int UBLACKWHITE = 3;            // uncompressed black and white image
            public final static int COLORMAPPED = 9;            // compressed color mapped image
            public final static int TRUECOLOR = 10;             // compressed true color image
            public final static int BLACKWHITE = 11;            // compressed black and white image

            /** Field image descriptor bitfield values definitions */
            public final static int ID_ATTRIBPERPIXEL = 0xF;
            public final static int ID_RIGHTTOLEFT = 0x10;
            public final static int ID_TOPTOBOTTOM = 0x20;
            public final static int ID_INTERLEAVE  = 0xC0;

            /** Field image descriptor / interleave values */
            public final static int I_NOTINTERLEAVED = 0;
            public final static int I_TWOWAY = 1;
            public final static int I_FOURWAY = 2;

            /** Type of this TGA file format */
            private final int tgaType;

            /** initial TGA image data fields */
            private int idLength;         // byte value
            private int colorMapType;     // byte value
            private int imageType;        // byte value

            /** TGA image colour map fields */
            private int firstEntryIndex;
            private int colorMapLength;
            private byte colorMapEntrySize;

            /** TGA image specification fields */
            private int xOrigin;
            private int yOrigin;
            private int width;
            private int height;
            private byte pixelDepth;
            private byte imageDescriptor;

            private byte[] imageIDbuf;
            private String imageID;

            // For construction from user data
            Header() {
                tgaType = TYPE_OLD; // dont try and get footer.
            }

            Header(final LEDataInputStream in) throws IOException {
                tgaType = TYPE_OLD; // dont try and get footer.

                // initial header fields
                idLength = in.readUnsignedByte();
                colorMapType = in.readUnsignedByte();
                imageType = in.readUnsignedByte();

                // color map header fields
                firstEntryIndex = in.readUnsignedShort();
                colorMapLength = in.readUnsignedShort();
                colorMapEntrySize = in.readByte();

                // TGA image specification fields
                xOrigin = in.readUnsignedShort();
                yOrigin = in.readUnsignedShort();
                width = in.readUnsignedShort();
                height = in.readUnsignedShort();
                pixelDepth = in.readByte();
                imageDescriptor = in.readByte();

                if (idLength > 0) {
                    imageIDbuf = new byte[idLength];
                    in.read(imageIDbuf, 0, idLength);
                    imageID = new String(imageIDbuf, "US-ASCII");
                }
            }

            public int tgaType()                 { return tgaType; }

            /** initial TGA image data fields */
            public int idLength()                { return idLength; }
            public int colorMapType()            { return colorMapType; }
            public int imageType()               { return imageType; }

            /** TGA image colour map fields */
            public int firstEntryIndex()         { return firstEntryIndex; }
            public int colorMapLength()          { return colorMapLength; }
            public byte colorMapEntrySize()      { return colorMapEntrySize; }

            /** TGA image specification fields */
            public int xOrigin()                 { return xOrigin; }
            public int yOrigin()                 { return yOrigin; }
            public int width()                   { return width; }
            public int height()                  { return height; }
            public byte pixelDepth()             { return pixelDepth; }
            public byte imageDescriptor()        { return imageDescriptor; }

            /** bitfields in imageDescriptor */
            public byte attribPerPixel()         { return (byte)(imageDescriptor & ID_ATTRIBPERPIXEL); }
            public boolean rightToLeft()         { return ((imageDescriptor & ID_RIGHTTOLEFT) != 0); }
            public boolean topToBottom()         { return ((imageDescriptor & ID_TOPTOBOTTOM) != 0); }
            public byte interleave()             { return (byte)((imageDescriptor & ID_INTERLEAVE) >> 6); }

            public byte[] imageIDbuf()           { return imageIDbuf; }
            public String imageID()              { return imageID; }

            @Override
            public String toString() {
                return "TGA Header " +
                        " id length: " + idLength +
                        " color map type: "+ colorMapType +
                        " image type: "+ imageType +
                        " first entry index: " + firstEntryIndex +
                        " color map length: " + colorMapLength +
                        " color map entry size: " + colorMapEntrySize +
                        " x Origin: " + xOrigin +
                        " y Origin: " + yOrigin +
                        " width: "+ width +
                        " height: "+ height +
                        " pixel depth: "+ pixelDepth +
                        " image descriptor: "+ imageDescriptor +
                        (imageIDbuf == null ? "" : (" ID String: " + imageID));
            }

            public int size() { return 18 + idLength; }

            // buf must be in little-endian byte order
            private void write(final ByteBuffer buf) {
                buf.put((byte) idLength);
                buf.put((byte) colorMapType);
                buf.put((byte) imageType);
                buf.putShort((short) firstEntryIndex);
                buf.putShort((short) colorMapLength);
                buf.put(colorMapEntrySize);
                buf.putShort((short) xOrigin);
                buf.putShort((short) yOrigin);
                buf.putShort((short) width);
                buf.putShort((short) height);
                buf.put(pixelDepth);
                buf.put(imageDescriptor);
                if (idLength > 0) {
                    try {
                        final byte[] chars = imageID.getBytes("US-ASCII");
                        buf.put(chars);
                    } catch (final UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }


        /**
         * Identifies the image type of the tga image data and loads
         * it into the JimiImage structure. This was taken from the
         * prototype and modified for the new Jimi structure
         */
        private void decodeImage(final GLProfile glp, final LEDataInputStream dIn) throws IOException {
            switch (header.imageType()) {
                case TGAImage.Header.UCOLORMAPPED:
                    throw new IOException("TGADecoder Uncompressed Colormapped images not supported");

                case TGAImage.Header.UTRUECOLOR:    // pixelDepth 15, 16, 24 and 32
                    switch (header.pixelDepth) {
                        case 16:
                            throw new IOException("TGADecoder Compressed 16-bit True Color images not supported");

                        case 24:
                        case 32:
                            decodeRGBImageU24_32(glp, dIn);
                            break;
                    }
                    break;

                case TGAImage.Header.UBLACKWHITE:
                    throw new IOException("TGADecoder Uncompressed Grayscale images not supported");

                case TGAImage.Header.COLORMAPPED:
                    throw new IOException("TGADecoder Compressed Colormapped images not supported");

                case TGAImage.Header.TRUECOLOR:
                    switch (header.pixelDepth) {
                        case 16:
                            throw new IOException("TGADecoder Compressed 16-bit True Color images not supported");

                        case 24:
                        case 32:
                            decodeRGBImageRLE24_32(glp, dIn);
                            break;
                    }
                    break;

                case TGAImage.Header.BLACKWHITE:
                    throw new IOException("TGADecoder Compressed Grayscale images not supported");
            }
        }

        /**
         * This assumes that the body is for a 24 bit or 32 bit for a
         * RGB or ARGB image respectively.
         */
        private void decodeRGBImageU24_32(final GLProfile glp, final LEDataInputStream dIn) throws IOException {
            setupImage24_32(glp);

            int i;    // row index
            int y;    // output row index
            final int rawWidth = header.width() * bpp;
            final byte[] rawBuf = new byte[rawWidth];
            final byte[] tmpData = new byte[rawWidth * header.height()];

            for (i = 0; i < header.height(); ++i) {
                dIn.readFully(rawBuf, 0, rawWidth);

                if (header.topToBottom())
                    y = header.height - i - 1; // range 0 to (header.height - 1)
                else
                    y = i;

                System.arraycopy(rawBuf, 0, tmpData, y * rawWidth, rawBuf.length);
            }

            if(format == GL.GL_RGB || format == GL.GL_RGBA)
                swapBGR(tmpData, rawWidth, header.height(), bpp);
            data = ByteBuffer.wrap(tmpData);
        }

        /**
         * This assumes that the body is for a 24 bit or 32 bit for a
         * RGB or ARGB image respectively.
         */
        private void decodeRGBImageRLE24_32(final GLProfile glp, final LEDataInputStream dIn) throws IOException {
            setupImage24_32(glp);

            final byte[] pixel = new byte[bpp];
            final int rawWidth = header.width() * bpp;
            final byte[] tmpData = new byte[rawWidth * header.height()];
            int i = 0, j;
            int packet, len;
            while (i < tmpData.length) {
                packet = dIn.readUnsignedByte();
                len = (packet & 0x7F) + 1;
                if ((packet & 0x80) != 0) {
                    dIn.read(pixel);
                    for (j = 0; j < len; ++j)
                        System.arraycopy(pixel, 0, tmpData, i + j * bpp, bpp);
                } else
                    dIn.read(tmpData, i, len * bpp);
                i += bpp * len;
            }

            if(format == GL.GL_RGB || format == GL.GL_RGBA)
                swapBGR(tmpData, rawWidth, header.height(), bpp);
            data = ByteBuffer.wrap(tmpData);
        }

        private void setupImage24_32(final GLProfile glp) {
            bpp = header.pixelDepth / 8;
            switch (header.pixelDepth) {
                case 24:
                    format = glp.isGL2GL3() ? GL.GL_BGR : GL.GL_RGB;
                    break;
                case 32:
                    boolean useBGRA = glp.isGL2GL3();
                    if(!useBGRA) {
                        final GLContext ctx = GLContext.getCurrent();
                        useBGRA = null != ctx && ctx.isTextureFormatBGRA8888Available();
                    }
                    format = useBGRA ? GL.GL_BGRA : GL.GL_RGBA;
                    break;
                default:
                    assert false;
            }
        }

        private static void swapBGR(final byte[] data, final int bWidth, final int height, final int bpp) {
            byte r,b;
            int k;
            for(int i=0; i<height; ++i) {
                for(int j=0; j<bWidth; j+=bpp) {
                    k=i*bWidth+j;
                    b=data[k+0];
                    r=data[k+2];
                    data[k+0]=r;
                    data[k+2]=b;
                }
            }
        }

        /** Returns the width of the image. */
        public int getWidth()    { return header.width(); }

        /** Returns the height of the image. */
        public int getHeight()   { return header.height(); }

        /** Returns the OpenGL format for this texture; e.g. GL.GL_BGR or GL.GL_BGRA. */
        public int getGLFormat() { return format; }

        /** Returns the bytes per pixel */
        public int getBytesPerPixel() { return bpp; }

        /** Returns the raw data for this texture in the correct
         (bottom-to-top) order for calls to glTexImage2D. */
        public ByteBuffer getData()  { return data; }

        /** Reads a Targa image from the specified file. */
        public static TGAImage read(final GLProfile glp, final String filename) throws IOException {
            return read(glp, new FileInputStream(filename));
        }

        /** Reads a Targa image from the specified InputStream. */
        public static TGAImage read(final GLProfile glp, final InputStream in) throws IOException {
            final LEDataInputStream dIn = new LEDataInputStream(new BufferedInputStream(in));

            final TGAImage.Header header = new TGAImage.Header(dIn);
            final TGAImage res = new TGAImage(header);
            res.decodeImage(glp, dIn);
            return res;
        }

        /** Writes the image in Targa format to the specified file name. */
        public void write(final String filename) throws IOException {
            write(new File(filename));
        }
        public MyByteArrayOutputStream output() {
            MyByteArrayOutputStream baos = new MyByteArrayOutputStream(16*1024);

            int hSize = header.size();
            final ByteBuffer buf = ByteBuffer.allocate(hSize);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.write(buf);
            buf.rewind();
            int os = buf.arrayOffset();
            baos.write(buf.array(), os, os + hSize);

            //data.rewind();

            int ps = data.arrayOffset();
            int pSize = data.limit();
            baos.write(data.array(), ps, ps + pSize);
            data.rewind();


            return baos;


        }

        /** Writes the image in Targa format to the specified file. */
        public void write(final File file) throws IOException {
            final FileOutputStream stream = IOUtil.getFileOutputStream(file, true);
            final FileChannel chan = stream.getChannel();
            final ByteBuffer buf = ByteBuffer.allocate(header.size());
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.write(buf);
            buf.rewind();
            chan.write(buf);
            chan.write(data);
            chan.force(true);
            chan.close();
            stream.close();
            data.rewind();
        }

        /** Creates a TGAImage from data supplied by the end user. Shares
         data with the passed ByteBuffer. Assumes the data is already in
         the correct byte order for writing to disk, i.e., BGR or
         BGRA. */
        public static TGAImage createFromData(final int width,
                                                                                 final int height,
                                                                                 final boolean hasAlpha,
                                                                                 final boolean topToBottom,
                                                                                 final ByteBuffer data) {
            final TGAImage.Header header = new TGAImage.Header();
            header.imageType = TGAImage.Header.UTRUECOLOR;
            header.width = width;
            header.height = height;
            header.pixelDepth = (byte) (hasAlpha ? 32 : 24);
            header.imageDescriptor = (byte) (topToBottom ? TGAImage.Header.ID_TOPTOBOTTOM : 0);
            // Note ID not supported
            final TGAImage ret = new TGAImage(header);
            ret.data = data;
            return ret;
        }
    }

    public VncClient(String host, int port /* TODO password, etc */ ) {

        Injector.setLogger(logger::trace);

        vncService = Injector.instantiateModelOrService(VncRenderService.class);
        vncService.getConfiguration().hostProperty().set(host);
        vncService.getConfiguration().portProperty().set(port);
        vncService.getConfiguration().securityProperty().set(SecurityType.NONE);
        //vncService.getConfiguration().securityProperty().set(SecurityType.VNC_Auth);
        //vncService.getConfiguration().passwordProperty().set("admin");
        vncService.connect();

        renderer = new VncCanvas() {

            @Override
            public void accept(ServerDecoderEvent event, ImageRect rect) {
                super.accept(event, rect);
                //System.out.println(event + " " + rect);
            }


            @Override
            protected void setImage(WritableImage vncImage) {
                super.setImage(vncImage);
                image = vncImage;
                vncReader = vncImage.getPixelReader();
            }
        };
        vncService.setEventConsumer(renderer);

//        SpaceGraph.window(new Surface() {
//
//
//
//            public GLTexture td;
//            int[] data = null;
//
//            @Override
//            protected void paint(GL2 gl) {
//                super.paint(gl);
//
//
//                if (view.vncImage != null) {
//                    int pw = (int) view.vncImage.getWidth();
//                    int ph = (int) view.vncImage.getHeight();
//                    if (pw == 0 || ph == 0)
//                        return;
//
//                    if (td == null) {
//                        td = new GLTexture(gl.getGL4(), UnsignedByte, 4, pw, ph, 1, true, 2);
//                    }
//
//                    int bufSize = 4 * pw * ph;
//                    if (data == null || data.length != bufSize/4)
//                        data = new int[bufSize/4];  // your byte array, 4 * int count
//                    view.vncImage.getPixelReader().getPixels(
//                        0, 0, pw, ph, WritablePixelFormat.getIntArgbInstance(), data,
//                        0, pw
//                    );
//
//
//
////                    td.clear();
//                    IntBuffer bdata = IntBuffer.wrap(data);
////                    td.copyFrom(bdata);
////                    td.updateMipMaps();
//
////                    td.bind();
////                                ) TextureIO.newTextureData(gl.getGLProfile(),
////                                new ByteArrayInputStream(data), GL2.GL_BGRA, GL2.GL_BGRA, false, (String)null);
////                    mGL.glActiveTexture(GL.GL_TEXTURE0);
////                    mGL.glBindTexture(mTextureTarget, getId());
//                    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, pw, ph, 0,
//                            GL2.GL_RGBA, GL2.GL_UNSIGNED_INT_8_8_8_8, bdata);
//
//                    Draw.rectTex(gl, 0, 0, 10, 10, 0);
//                    td.unbind();
//
//
//                }
//
//            }
//        }, 800, 800);


    }

    public void newFXWindow() {
        FX.run(()->{
            final VncImageView imageView = new VncImageView() {

                @Override
                protected void onFired(KeyButtonEvent msg) {
                    System.out.println("KEY: " + msg);
                }
            };

            //vncService.inputEventListenerProperty().addListener(l -> );


            renderer.vncImage.addListener((images, prev, next)->{
                if (next!=null)
                    imageView.setImage(next);
            });

            imageView.setImage(image);

//            imageView.setScaleX(2);
//            imageView.setScaleY(2);

            FX.newWindow("x", new BorderPane(scrolled((imageView))));

            //imageView.registerInputEventListener(vncService.inputEventListenerProperty().get());

        });
    }

    public Surface newSurface() {
//        //TODO
//        int pw = 200;
//        int ph = 200;
//        return new MatrixView(pw, ph, (x, y, gl) -> {
//            final PixelReader reader = this.vncReader;
//            if (reader !=null) {
//                int argb = reader.getArgb(x, y);
//                gl.glColor3f(decodeRed(argb), decodeGreen(argb), decodeBlue(argb));
//            }
//            return 0;
//        });

        return new Surface() {

            Texture texture;

            @Override
            protected void paint(GL2 gl) {
                super.paint(gl);

                //VideoBag.Frame f = ff.get();

                Texture tt = texture;
                if (tt!=null) {
                    tt.destroy(gl);
                    tt = null;
                }

                if (vncReader!=null) {



                    int ww = (int)renderer.vncImage.get().getWidth();
                    int hh = (int)renderer.vncImage.get().getHeight();
                    byte[] data = new byte[ww * hh * 4];
                    vncReader.getPixels(0,0,ww,hh, PixelFormat.getByteBgraInstance(), data, 0, ww*4);

                    tt = tgaTexture(ww, hh, true, data);

                    texture = tt;

                }

                float xx = 0, yy = 0;
                float sc = 1f;
                if (tt!=null) {
                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
                } else {
                    Draw.colorBipolar(gl, 0.5f);
                    Draw.rect(gl, xx, yy, sc, sc);
                }
            }
        };
    }

    public PixelBag newSensor(int pw, int ph) {
        return new PixelBag(pw, ph) {

            @Override public int sw() { return image==null ? 0 : (int) image.getWidth(); }

            @Override public int sh() { return image==null ? 0 : (int) image.getHeight(); }

            @Override public int rgb(int sx, int sy) { return vncReader.getArgb(sx, sy); }
        };
    }

    public static void main(String[] args) {
        VncClient v = new VncClient("localhost", 5901);

        //1.
        v.newFXWindow();

        //2.
        SpaceGraph.window(v.newSurface(), 800, 800);

//        Default nar = NAgents.newMultiThreadNAR(2, new RealTime.CS(true).dur(0.25f));
//
//        NAgents a = new NAgents(nar) {
//
//            {
//                PixelBag pb = v.newSensor(64, 64);
//                pb.addActions("vnc", this);
//
//                addCamera("vnc", pb, (v) -> t(v, alpha));
//            }
//
//            @Override
//            protected float act() {
//                return 0;
//            }
//
//
//        };
//        NAgents.chart(a);
//
//        a.runRT(55f);


    }


}
