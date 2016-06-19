package nars.vision;

import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import nars.$;
import nars.NAR;
import nars.index.TermIndex;
import nars.term.Termed;

/**
 * Created by me on 6/5/16.
 */
public class NARCamera implements PixelCamera.PerPixel {

    private final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    //final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
    final LongObjectHashMap<Termed> terms = new LongObjectHashMap<>();

    private final TermIndex index;
    private PerPixel perPixel;

    public NARCamera(PixelCamera c, PixelToTerm pixelTerm) {
        this($.terms, c, pixelTerm);
    }

    public NARCamera(NAR nar, PixelCamera c, PixelToTerm pixelTerm) {
        this(nar.index, c, pixelTerm);
    }

    public NARCamera(TermIndex index, PixelCamera c, PixelToTerm pixelTerm) {
        this.index = index;
        this.cam = c;
        this.pixelTerm = pixelTerm;
    }

    public interface PerPixel {
        void pixel(int x, int y, Termed t, int r, int g, int b);
    }
    public interface PixelToTerm {
        Termed pixel(int x, int y);
    }

    public void update(PerPixel pp) {
        cam.update(this);
        this.perPixel = pp;
    }

    @Override
    public void pixel(int x, int y, int rgb) {
        int r = (rgb & 0x00ff0000) >> 16;
        int g = (rgb & 0x0000ff00) >> 8;
        int b = (rgb & 0x000000ff);
        perPixel.pixel(x, y, p(x,y), r, g, b);
    }

    public final Termed p(int x, int y) {
        return terms.getIfAbsentPutWithKey(l(x, y),
                xy -> index.the(pixelTerm.pixel(x(xy), y(xy))));
    }

    private int x(long xy) {
        return (int)(xy >> 32);
    }

    private int y(long xy) {
        return (int)(xy & 0x0000ffff);
    }

    static long l(int x, int y) {
        return (((long)x) << 32) | ((long)y);
    }

}
