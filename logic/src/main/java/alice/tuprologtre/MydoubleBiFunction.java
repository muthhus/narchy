package alice.tuprologtre;
import java.util.function.ToDoubleBiFunction;


public class MydoubleBiFunction implements ToDoubleBiFunction<Integer,Integer>{	
	@Override
	public double applyAsDouble(Integer x, Integer y){
		return x+y;
	}
}