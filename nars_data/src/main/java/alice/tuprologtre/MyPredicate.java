package alice.tuprologtre;

public class MyPredicate implements java.util.function.Predicate<String> {
	@Override
	public boolean test(String s){
		return s.length()>4;
	}
}
