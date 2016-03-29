package nars.rover.obj;

import com.artemis.Component;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.dynamics.Body;

/**
 * Created by me on 7/18/15.
 */
public class Material extends Component {

	public static final Material wall = new WallMaterial();
	public static final Material food = new FoodMaterial();
	public static final Material poison = new PoisonMaterial();

	JoglAbstractDraw.DrawProperty renderer = null;


	public static class FoodMaterial extends Material /*implements Edible*/ {

		static final Color3f foodFill = new Color3f(0.15f, 0.15f, 0.6f);

//		@Override
//		public void before(Body b, JoglAbstractDraw d, float time) {
//
//			d.setFillColor(foodFill);
//		}

		@Override
		public String toString() {
			return "food";
		}
	}

	public static class WallMaterial extends Material {
		static final Color3f wallFill = new Color3f(0.5f, 0.5f, 0.5f);

//		@Override
//		public void before(Body b, JoglAbstractDraw d, float time) {
//
//			d.setFillColor(wallFill);
//		}

		@Override
		public String toString() {
			return "wall";
		}
	}

	public static final Color3f poisonFill = new Color3f(0.45f, 0.15f, 0.15f);

	public static class PoisonMaterial extends Material /*implements Edible*/ {


//		@Override
//		public void before(Body b, JoglAbstractDraw d, float time) {
//			d.setFillColor(poisonFill);
//		}

		@Override
		public String toString() {
			return "poison";
		}
	}

}
