package nars.video;

import nars.concept.SensorConcept;

/**
 * Created by me on 9/21/16.
 */
public class Sensor2D<S> {

    public final SensorConcept[][] matrix;
    public final int width, height;
    public final S src;


    public Sensor2D(S src, int width, int height) {
        this.src = src;
        this.width = width;
        this.height = height;
        this.matrix = new SensorConcept[width][height];
    }


}
