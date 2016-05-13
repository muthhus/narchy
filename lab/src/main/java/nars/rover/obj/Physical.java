package nars.rover.obj;


import com.artemis.Component;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Fixture;

public class Physical extends Component {

    public Body body;
    public BodyDef bodyDef;
    public Shape shape;
    public Fixture fixture;
    public float density = 10;

    public Physical() {

    }

    public Physical(BodyDef b, Shape s) {
        this.bodyDef = b;
        this.shape = s;
        this.body = null;
    }


    @Override
    public String toString() {
        return "Physical{" +
                "body=" + body +
                ", bodyDef=" + bodyDef +
                ", shape=" + shape +
                ", fixture=" + fixture +
                ", density=" + density +
                '}';
    }
}
