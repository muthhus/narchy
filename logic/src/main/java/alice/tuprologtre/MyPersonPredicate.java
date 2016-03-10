package alice.tuprologtre;

public class MyPersonPredicate {
	public boolean test(Person p){
		return p.getGender().trim().equalsIgnoreCase("MALE") && p.getAge() >= 18;
	}
}
