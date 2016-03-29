package nars.rover.obj;


import com.artemis.Component;


public class Motorized extends Component {

    public float left, right, fore, back;

    public float linearSpeed = 25;
    public float angularSpeed = 15;

    public float stop(float s) {
        left *= (1f - s);
        right *= (1f - s);
        fore *= (1f - s);
        back *= (1f - s);
        return s;
    }

    public float backward(float l) {
        back = l;
        return l;
    }

    public float forward(float l) {
        fore = l;
        return l;
    }

    public float left(float l) {
        left = l;
        return l;
    }

    public float right(float l) {
        right = l;
        return l;
    }

}
