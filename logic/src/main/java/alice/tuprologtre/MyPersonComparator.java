package alice.tuprologtre;

public class MyPersonComparator implements java.util.Comparator<Person> {
	@Override
	public int compare(Person p1, Person p2){
		return p1.getSurname().compareTo(p2.getSurname());
	}
}
