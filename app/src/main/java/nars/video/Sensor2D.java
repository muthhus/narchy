package nars.video;

import nars.NAR;
import nars.concept.SensorConcept;
import nars.control.NARService;

/**
 * Created by me on 9/21/16.
 */
public class Sensor2D<S> extends NARService {

    public final SensorConcept[][] matrix;
    public final int width, height;
    public final S src;


    public Sensor2D(S src, int width, int height, NAR nar) {
        super(nar);
        this.src = src;
        this.width = width;
        this.height = height;
        this.matrix = new SensorConcept[width][height];
    }


}
