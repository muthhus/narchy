package nars.rover.obj;

import com.artemis.Component;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.term.Term;
import org.jbox2d.common.Color3f;

/**
 * Created by me on 7/18/15.
 */
public class Material extends Component {


	public final Term term;

	JoglAbstractDraw.DrawProperty renderer = null;

	public Material(Term term) {
		this.term = term;
	}

	public Term term() {
		return term;
	}


//	public static class FoodMaterial extends Material /*implements Edible*/ {
//
//		static final Color3f foodFill = new Color3f(0.15f, 0.15f, 0.6f);
//
//		public FoodMaterial() {
//			super(term);
//		}
//
////		@Override
////		public void before(Body b, JoglAbstractDraw d, float time) {
////
////			d.setFillColor(foodFill);
////		}
//
//		@Override
//		public String toString() {
//			return "food";
//		}
//	}
//
//	public static class WallMaterial extends Material {
//		static final Color3f wallFill = new Color3f(0.5f, 0.5f, 0.5f);
//
//		public WallMaterial() {
//			super(term);
//		}
//
////		@Override
////		public void before(Body b, JoglAbstractDraw d, float time) {
////
////			d.setFillColor(wallFill);
////		}
//
//		@Override
//		public String toString() {
//			return "wall";
//		}
//	}
//
//	public static final Color3f poisonFill = new Color3f(0.45f, 0.15f, 0.15f);
//
//	public static class PoisonMaterial extends Material /*implements Edible*/ {
//		public PoisonMaterial() {
//			super(term);
//		}
//
//
////		@Override
////		public void before(Body b, JoglAbstractDraw d, float time) {
////			d.setFillColor(poisonFill);
////		}
//
//		@Override
//		public String toString() {
//			return "poison";
//		}
//	}

}
