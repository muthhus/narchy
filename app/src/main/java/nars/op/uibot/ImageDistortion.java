package nars.op.uibot;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.*;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.distort.impl.ImplImageDistort_IL_U8;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_IL_U8;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;

/**
 * https://github.com/lessthanoptimal/BoofCV/blob/master/main/geo/src/boofcv/alg/distort/LensDistortionOps.java
 * https://github.com/lessthanoptimal/BoofCV/blob/master/main/ip/test/boofcv/alg/distort/impl/GeneralImageDistortTests.java#L140
 * */
public class ImageDistortion implements ImageProcess {
    /** proportion of image relative to center (0,0) -> (+/= 1, +/- 1) */
    public float x, y;

    /** proportion of image width/height */
    public float pw, ph;

    //InterleavedU8 output;

    private int offX;
    private int offY;
    private InterleavedU8 output;

    public ImageDistortion() {
        super();


        //output = new InterleavedU8(200, 200, 3);

    }




    @Override public ImageBase apply(ImageBase input) {
        int x1 = (int) (5 + Math.random() * 400);
        int y1 = (int) (5 + Math.random() * 400);
        return output = ((InterleavedU8)input).subimage(
                x1,
                y1,
                x1+100, y1+100, output);
//        FDistort tr = new FDistort().setRefs(input, output).interpNN().scaleExt();
//        //tr.apply(input,output/*,x0,y0,x1,y1*/);
//        tr.apply();
//        return output;
    }


}
