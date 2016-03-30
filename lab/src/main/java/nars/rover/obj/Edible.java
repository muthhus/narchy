package nars.rover.obj;

import com.artemis.Component;

/**
 * Created by me on 3/29/16.
 */
public class Edible extends Component {
    float nutritious;
    float poisonous;

    public Edible(float nutritious, float poisonous) {
        this.nutritious = nutritious;
        this.poisonous = poisonous;
    }

    //TODO psychoactive components

}
