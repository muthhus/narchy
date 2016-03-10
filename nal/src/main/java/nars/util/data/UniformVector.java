package nars.util.data;

import nars.Global;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.util.Texts.n2;

/**
 * Represents a changing 1-dimensional array of double[], each element normalized to 0..1.0
 */
public class UniformVector  {

    float epsilon = Global.TRUTH_EPSILON;
    @Nullable
    public float[] lastData;
    public final float[] data;
    private final String prefix;
    private final NAR nar;
    private float priority;

    public UniformVector(NAR n, String prefix, float[] data) {
        nar = n;
        this.prefix = prefix;
        this.data = data;
        priority = Float.NaN;
    }

    public void update() {
        if (priority == 0) 
            return;
        
        if ((lastData != null) && (Arrays.equals(lastData, data))) {
            //unchanged
            return;
        }

        boolean changed = false;
        if ((lastData == null) || (lastData.length!=data.length)) {
            //first time
            lastData = new float[data.length];     
            changed = true;
        }

        
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            float v = data[i];
            if ((changed) || (different(v,lastData[i]))) {
                if (v > 1.0) v = 1.0f;
                else if (v < 0.0) v = 0.0f;
                float truth = v;
                float conf = 0.99f;
                s.append('$').append(n2(priority)).append("$ <").append(prefix).append('_').append(i).append(" --> ").append(prefix).append(">. :|: %").append(n2(truth)).append(';').append(n2(conf)).append("%\n");
            }
        }
        
        System.arraycopy(data, 0, lastData, 0, data.length);
        
        nar.input(s.toString());
        
    }

    public boolean different(float a, float b) {
        return Math.abs(a - b) >= epsilon;
    }
    
    @NotNull
    public UniformVector setPriority(float p) {
        priority = p;
        return this;
    }
    
    
}
