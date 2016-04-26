package nars.rover.obj;

import com.artemis.Component;
import org.jbox2d.common.Color3f;

/**
 * Created by me on 3/30/16.
 */
public class MaterialColor extends Component {


    public Color3f fillColor;

    private float freq;
    private float phase;

    public MaterialColor() {
        this(0,0,0);
    }
    public MaterialColor(float r, float g, float b) {
        this.fillColor = new Color3f(r, g, b);
        //this.a = 1f;
    }

    public MaterialColor strobe(float freq, float phase) {
        this.freq = freq;
        this.phase = phase;
        //TODO arbitrary functions, not just sine
        return this;
    }

    /** called by the renderer to set the current palette color */
    public void use(Color3f target, float t) {
        target.set(fillColor);
        if (freq!=0) {
            float amp = 0.5f * ((float)Math.cos(t) + 1.0f);
            target.x *= amp;
            target.y *= amp;
            target.z *= amp;
        }

    }

    public void set(float r, float g, float b) {
        fillColor.set(r, g, b);
    }

}
