package alice.tuprologtre;
import java.util.function.BiFunction;


public class MyIntegerBiFunction implements BiFunction<Integer,Integer,Integer> {	
	@Override
	public Integer apply(Integer x, Integer y){
		return x+y;
	}
}
