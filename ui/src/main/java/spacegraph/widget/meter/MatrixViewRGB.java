//package spacegraph.widget.meter;
//
//import com.jogamp.opengl.GL;
//import com.jogamp.opengl.GL2;
//import spacegraph.widget.Widget;
//
//import java.nio.ByteBuffer;
//
//public class MatrixViewRGB extends Widget {
//
//
//    private final int w;
//    private final int h;
//
//    private ByteBuffer imgBuffer;
//    private byte[] a;
//
//    public interface ViewFunction2DRGB {
//        void update(int x, int y, ByteBuffer b);
//    }
//
//    final ViewFunction2DRGB view;
//
//    public MatrixViewRGB(int w, int h, ViewFunction2DRGB view) {
//        this.w = w;
//        this.h = h;
//        this.view = view;
//    }
//
//
//    @Override
//    protected void paintComponent(GL2 gl) {
//
//        int h = this.h;
//        int w = this.w;
//
//        if ((w == 0) || (h == 0))
//            return;
//
//        int size = w * h * 3;
//        if (a == null || a.length != size) {
//            imgBuffer = ByteBuffer.wrap(a = new byte[size]);
//        }
//
//        imgBuffer.rewind();
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                view.update(x, y, imgBuffer);
//            }
//        }
//
//        imgBuffer.rewind();
//        gl.glDrawPixels(h, w, GL.GL_RGB,
//                GL.GL_UNSIGNED_BYTE, imgBuffer);
//        //gl.glBitmap(h, w, 0, 0, 0, 0, imgBuffer);
//
//    }
//}
