package nars.gui.test;

import gleem.BSphere;
import gleem.BSphereProvider;
import gleem.HandleBoxManip;

/**
 * Created by me on 6/22/16.
 */
public class DefaultHandleBoxManip extends HandleBoxManip implements BSphereProvider {


    final BSphere bsph = new BSphere();

    public DefaultHandleBoxManip(GleemControl viewer) {
        super(viewer);
    }

    @Override
    public final BSphere getBoundingSphere() {
        return bsph.setCenter(translation).setRadius(getRadius());
    }

    public DefaultHandleBoxManip translate(float x, float y, float z) {
        translation.set(x, y, z);
        return this;
    }
}
