package alice.tuprologtre;
import java.util.function.BiFunction;


public class MyStringBiFunction implements BiFunction<String,String,String> {	
	@Override
	public String apply(String x, String y){
		return x+y;
	}
}
