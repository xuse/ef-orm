package jef.database.misc;

import jef.database.QB;
import jef.database.Condition.Operator;
import jef.database.query.Query;
import jef.orm.multitable.model.Person;
import jef.tools.Assert;

import org.junit.Test;

public class HashTest {		

	@Test
	public void testQueryHash1(){
		Query<Person> q1=QB.create(Person.class);
		q1.addCondition(Person.Field.cell,"123");
		q1.addCondition(Person.Field.gender,"F");

		Query<Person> q2=QB.create(Person.class);
		q2.addCondition(Person.Field.gender,"F");
		q2.addCondition(Person.Field.cell,"123");

		Assert.equals(q1.hashCode(), q2.hashCode());
		Assert.equals(q1, q2);
	}
	@Test
	public void testQueryHash2(){
		Query<Person> q1=QB.create(Person.class);
		q1.addCondition(Person.Field.cell,"123");
		q1.addCondition(Person.Field.gender,"F");

		Query<Person> q2=QB.create(Person.class);
		q2.addCondition(Person.Field.gender,"123");
		q2.addCondition(Person.Field.cell,"F");

		Assert.notEquals(q1.hashCode(), q2.hashCode());
		Assert.notEquals(q1, q2);
	}
	
	@Test
	public void testQueryHash3(){
		Query<Person> q1=QB.create(Person.class);
		q1.addCondition(Person.Field.cell,Operator.IN,new String[]{"123"});	

		Query<Person> q2=QB.create(Person.class);		
		q2.addCondition(Person.Field.cell,Operator.IN,new String[]{"123"});

		Assert.equals(q1.hashCode(), q2.hashCode());
		Assert.equals(q1, q2);
	}
	
}
