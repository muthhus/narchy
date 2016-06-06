package nars.vision;

import com.gs.collections.api.tuple.primitive.IntIntPair;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.NAR;
import nars.index.TermIndex;
import nars.term.Termed;

import java.util.WeakHashMap;

/**
 * Created by me on 6/5/16.
 */
public class NARCamera implements PixelCamera.PerPixel {

    private final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
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
        return terms.computeIfAbsent(PrimitiveTuples.pair(x, y),
                xy -> index.the(pixelTerm.pixel(xy.getOne(), xy.getTwo())));
    }

}
