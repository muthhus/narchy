package nars.op.uibot;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.impl.ImplImageDistort_IL_U8;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;

/**
 * A manipulation or filtering of a Vision context.
 * They can be chained together to form a pipeline of transforms
 */
public interface ImageProcess {

    ImageBase apply(ImageBase input);


}
