package nars.video;


import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.alg.color.ColorRgb;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.InterleavedU8;
import com.github.sarxos.webcam.WebcamEvent;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.Util;
import nars.$;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.gui.BagChart;
import nars.link.BLink;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.index.RTree;
import spacegraph.index.Rect1D;
import spacegraph.render.Draw;
import spacegraph.video.WebCam;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static jcog.Util.unitize;

/**
 * Created by me on 12/12/16.
 */
public class VideoBag {

    final static Logger logger = LoggerFactory.getLogger(VideoBag.class);

    final RTree<Frame> index;

    final ArrayBag<Frame> bag;

    private final Queue<Frame> textureTrash = new ConcurrentLinkedQueue<>();

    public class Frame extends Rect1D {

        public final long t;
        public final ImageBase image;
        public final int width, height;
        public Texture texture;

        public Frame(long when, @NotNull ImageBase i) {
            this(when, i, i.getWidth(), i.getHeight());
        }

        public Frame(long when, @NotNull ImageBase i, int width, int height) {
            this.t = when;
            this.image = i;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return new Date(t).toString() + ": " + image.toString();
        }

        @Override
        public double from() {
            return t;
        }

        @Override
        public double to() {
            return t;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void delete() {
            textureTrash.add(this);
        }
    }

    public VideoBag(int initialCapacity) {
        bag = new ArrayBag<Frame>(initialCapacity, BudgetMerge.avgBlend, new ConcurrentHashMap<>()) {
            @Override
            public void onAdded(BLink<Frame> v) {
                index.add(v.get());
            }

            @Override
            public void onRemoved(BLink<Frame> v) {
                Frame f = v.get();
                index.remove(f);
                f.delete();
            }
        };
        index = new RTree<>((l) -> l, 2, 8, RTree.Split.LINEAR);


    }

    InterleavedU8 prev = null;
    final AtomicBoolean busy = new AtomicBoolean(false);
    private GrayU8 prevU, nextU;
    private ImageFlow ff;

    private final DenseOpticalFlow<GrayU8> flow = FactoryDenseOpticalFlow.
            //hornSchunck(null, GrayU8.class);
                    broxWarping(null, GrayU8.class);

    public boolean put(@NotNull InterleavedU8 current) {

        boolean added;
        if (busy.compareAndSet(false, true)) {

            synchronized (bag) {
                long when = System.currentTimeMillis();
                //                    ImageBase di =
                //                            //        null;
                //                            ImageBase.createFromData(c.width, c.height, false, true, ByteBuffer.wrap(current.data));

                final Frame frame = new Frame(when, current);
                //System.out.println(current.data + " " + bag.rtree.collectStats());

                float scale;
                InterleavedU8 prev = this.prev;
                if (prev != null) {

                    final int cw = current.width;
                    final int ch = current.height;
                    if (prevU == null || prevU.width != cw || prevU.height != ch) {
                        prevU = new GrayU8(cw, ch);
                        nextU = new GrayU8(cw, ch);
                        ff = new ImageFlow(cw, ch);
                    }

                    ColorRgb.rgbToGray_Weighted(prev, prevU);

                    ColorRgb.rgbToGray_Weighted(current, nextU);

                    flow.process(prevU, nextU, ff);

                    float totalFlow = 0;
                    for (ImageFlow.D d : ff.data) {
                        if (d.isValid()) {
                            totalFlow += Math.sqrt(Util.sqr(d.x) + Util.sqr(d.y));
                        }
                    }
                    float avgFlow = unitize(totalFlow / (cw * ch));
                    logger.info("avg flow per pixel: {} ", avgFlow);


                    //                    BufferedImage visualized = new BufferedImage(current.width,current.height, BufferedImage.TYPE_INT_RGB);
                    //VisualizeOpticalFlow.colorized(ff, 10, visualized);
                    //
                    //                    gui.setContentPane(new ImagePanel(visualized));
                    //                    gui.doLayout();
                    //                    gui.invalidate();
                    //                    gui.repaint();


                    scale = unitize(avgFlow);


                } else {

                    scale = 1f;
                }

                bag.commit();
                bag.put(frame, $.b(0.1f + 0.9f * scale, 0.1f + 0.9f * scale), 1f, null);
                busy.set(false);

                added = true;
            }

        } else {

            logger.info("frame dropped");

            added = false;

        }

        this.prev = current;

        return added;

    }

    public static void main(String[] args) {
        VideoBag bag = new VideoBag(16);
        final BagChart s = new BagChart(bag.bag, 16);


//        JFrame gui = new JFrame();
//        gui.setSize(400,400);
//        gui.setVisible(true);


        final WebCam cam;
        cam = new WebCam(320, 200);
        cam.eventChange.on(new Consumer<WebcamEvent>() {


            //final ExecutorService exe = Executors.newSingleThreadExecutor();

            @Override
            public void accept(WebcamEvent c) {

                //DDSImage di = DDSImage.createFromData(DDSImage.D3DFMT_R8G8B8, width, height, new ByteBuffer[]{image.asReadOnlyBuffer()});
                //JPEGImage di = JPEGImage.read(new ByteBufferInputStream(image.asReadOnlyBuffer()));


                s.update();

//                InterleavedU8 current = new InterleavedU8(c.width, c.height, 3);
//                current.data = c.image.clone();

                //exe.execute(() -> {
                if (cam.iimage!=null)
                    bag.put(cam.iimage);
                //});


            }
        });

        SpaceGraph.window(s, 800, 800);
        SpaceGraph.window(new EventTimeline(bag), 800, 800);
    }

    static class EventTimeline extends Surface {

        public final VideoBag bag;

        float scale = 0.25f; //seconds to visual units


        EventTimeline(VideoBag bag) {
            this.bag = bag;
        }

        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);


//            //reclaim deleted textures
            while (!bag.textureTrash.isEmpty()) {
                Frame ff = bag.textureTrash.remove();
//                ((InterleavedU8)ff.image).data = null;
//                System.out.println("destroying: " + ff.texture.getTextureObject());
//                ff.texture.disable(gl);
//                ff.texture.destroy(gl);
//                ff.texture = null;
            }

            long now = System.currentTimeMillis();

            bag.bag.forEach((ff) -> {
                Frame f = ff.get();

                Texture tt = f.texture;
                if (tt == null) {


                    byte[] data = ((InterleavedU8)f.image).data;


                    tt = tgaTexture(f.width, f.height, false, data);

                    System.out.println("creating: " + tt.getTextureObject());

                    tt.enable(gl);
                    tt.bind(gl);


                    f.texture = tt;

                }

                float pp = ff.priSafe(0);
                float xx = scale * (f.t - now) / 1000f ;
                float yy = (float) Math.sin(f.t);
                //if (tt!=null) {
                    float sc = scale * (1f + pp);
                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
//                } else {
//                    Draw.colorPolarized(gl, 0.25f + 0.75f * pp);
//                    Draw.rect(gl, xx, yy, scale, scale);
//                }

                tt.disable(gl);
                tt.destroy(gl);
                f.texture = null;
            });
        }

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

    public  static class TGAImage {
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
    public static class LEDataInputStream extends FilterInputStream implements DataInput
    {
        /**
         * To reuse    some of    the    non    endian dependent methods from
         * DataInputStreams    methods.
         */
        DataInputStream    dataIn;

        public LEDataInputStream(final InputStream in)
        {
            super(in);
            dataIn = new DataInputStream(in);
        }

        @Override
        public void close() throws IOException
        {
            dataIn.close();        // better close as we create it.
            // this will close underlying as well.
        }

        @Override
        public synchronized    final int read(final byte    b[]) throws    IOException
        {
            return dataIn.read(b, 0, b.length);
        }

        @Override
        public synchronized    final int read(final byte    b[], final int off, final int len) throws IOException
        {
            final int    rl = dataIn.read(b,    off, len);
            return rl;
        }

        @Override
        public final void readFully(final byte b[]) throws IOException
        {
            dataIn.readFully(b,    0, b.length);
        }

        @Override
        public final void readFully(final byte b[], final int off, final int len)    throws IOException
        {
            dataIn.readFully(b,    off, len);
        }

        @Override
        public final int skipBytes(final int n) throws IOException
        {
            return dataIn.skipBytes(n);
        }

        @Override
        public final boolean readBoolean() throws IOException
        {
            final int    ch = dataIn.read();
            if (ch < 0)
                throw new EOFException();
            return (ch != 0);
        }

        @Override
        public final byte readByte() throws    IOException
        {
            final int    ch = dataIn.read();
            if (ch < 0)
                throw new EOFException();
            return (byte)(ch);
        }

        @Override
        public final int readUnsignedByte()    throws IOException
        {
            final int    ch = dataIn.read();
            if (ch < 0)
                throw new EOFException();
            return ch;
        }

        @Override
        public final short readShort() throws IOException
        {
            final int    ch1    = dataIn.read();
            final int    ch2    = dataIn.read();
            if ((ch1 | ch2)    < 0)
                throw new EOFException();
            return (short)((ch1    << 0) +    (ch2 <<    8));
        }

        @Override
        public final int readUnsignedShort() throws    IOException
        {
            final int    ch1    = dataIn.read();
            final int    ch2    = dataIn.read();
            if ((ch1 | ch2)    < 0)
                throw new EOFException();
            return (ch1    << 0) +    (ch2 <<    8);
        }

        @Override
        public final char readChar() throws    IOException
        {
            final int    ch1    = dataIn.read();
            final int    ch2    = dataIn.read();
            if ((ch1 | ch2)    < 0)
                throw new EOFException();
            return (char)((ch1 << 0) + (ch2    << 8));
        }

        @Override
        public final int readInt() throws IOException
        {
            final int    ch1    = dataIn.read();
            final int    ch2    = dataIn.read();
            final int    ch3    = dataIn.read();
            final int    ch4    = dataIn.read();
            if ((ch1 | ch2 | ch3 | ch4)    < 0)
                throw new EOFException();
            return ((ch1 <<    0) + (ch2 << 8)    + (ch3 << 16) +    (ch4 <<    24));
        }

        @Override
        public final long readLong() throws    IOException
        {
            final int    i1 = readInt();
            final int    i2 = readInt();
            return (i1 & 0xFFFFFFFFL) + ((long)i2 << 32);
        }

        @Override
        public final float readFloat() throws IOException
        {
            return Float.intBitsToFloat(readInt());
        }

        @Override
        public final double    readDouble() throws    IOException
        {
            return Double.longBitsToDouble(readLong());
        }

        /**
         * dont call this it is not implemented.
         * @return empty new string
         **/
        @Override
        public final String    readLine() throws IOException
        {
            return "";
        }

        /**
         * dont call this it is not implemented
         * @return empty new string
         **/
        @Override
        public final String    readUTF() throws IOException
        {
            return "";
        }

        /**
         * dont call this it is not implemented
         * @return empty new string
         **/
        public final static    String readUTF(final DataInput in) throws    IOException
        {
            return "";
        }
    }


}
