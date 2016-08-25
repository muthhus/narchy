package alice.tuprologtre;
public class Person {
	
	private final String name;
	private final String surname;
	private String gender;
	private int age;
	
	public Person(){
		this.name="N/A";
		this.surname="N/A";
	}
	
	public Person(String name, String surname){
		this.name=name;
		this.surname=surname;
	}
	
	public Person(String name, String surname, String gender, int age){
		this.name=name;
		this.surname=surname;
		this.gender=gender;
		this.age=age;
	}
	
	public String getName(){
	 return this.name;
	}
	
	public String getSurname(){
		 return this.surname;
	}
	
	public String getGender(){
		 return this.gender;
	}
	
	public int getAge(){
		 return this.age;
	}
	
	public String toString(){
		 return this.name+ ' ' +this.surname+ ' ' +this.gender+ ' ' +this.age;
	}
	
}