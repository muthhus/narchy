package nars.rover.obj;

import com.artemis.Component;

/**
 * Created by me on 3/29/16.
 */
public class Edible extends Component {
    public float nutrients;
    public float poison;

    public Edible(float nutrients, float poison) {
        this.nutrients = nutrients;
        this.poison = poison;
    }

    public void energize() {
        if (nutrients >= 0.01 && nutrients < 1f)
            nutrients = Math.min(1f, nutrients + 0.01f);
        if (poison >= 0.01 && poison < 1f)
            poison = Math.min(1f, poison + 0.01f);
    }


    //TODO psychoactive components

//    protected void onEat(Body eaten, Material m) {
//
//    }
//
//    public void eat(Body eaten) {
//        Material m = (Material)eaten.getUserData();
//
//        onEat(eaten, m);
//
//        @Deprecated int sz = 48;
//        float x = (float) Math.random() * sz - sz / 2f;
//        float y = (float) Math.random() * sz - sz / 2f;
//        //random new position
//        eaten.setTransform(new Vec2(x * 2.0f, y * 2.0f), eaten.getAngle());
//    }

}
