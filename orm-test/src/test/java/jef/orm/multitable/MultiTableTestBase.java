package jef.orm.multitable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jef.database.DbClient;
import jef.database.meta.Feature;
import jef.orm.multitable.model.Person;
import jef.orm.multitable.model.PersonFriends;
import jef.orm.multitable.model.School;
import jef.orm.multitable.model.Score;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapperImpl;

public abstract class MultiTableTestBase extends org.junit.Assert{
	protected DbClient db;
	
	private static String[] prepareSql=new String[]{
		"insert into person_friends(pid,friendId,COMMENT) values (1,2,'1 has friend: 2')",
		"insert into person_friends(pid,friendId,COMMENT) values (1,3,'1 has friend: 3')",
		"insert into person_friends(pid,friendId,COMMENT) values (2,1,'2 has friend: 1')",
		"insert into person_friends(pid,friendId,COMMENT) values (2,3,'2 has friend: 3')",
		"insert into person_friends(pid,friendId,COMMENT) values (3,2,'3 has friend: 2')",
		
		"insert into score (score,pid,testtime,subject) values(50,1,current_date,'电脑')",
		"insert into score (score,pid,testtime,subject) values(60,1,current_date,'语文')",
		"insert into score (score,pid,testtime,subject) values(70,1,current_date,'算数')",
		"insert into score (score,pid,testtime,subject) values(80,1,current_date,'英语')",
		"insert into score (score,pid,testtime,subject) values(90,1,current_date,'物理')",
		"insert into score (score,pid,testtime,subject) values(100,1,current_date,'化学')",
		"insert into score (score,pid,testtime,subject)values(60,2,current_date,'语文')",
		"insert into score (score,pid,testtime,subject)values(88,2,current_date,'算数')",
		"insert into score (score,pid,testtime,subject)values(90,3,current_date,'语文')",
		"insert into score (score,pid,testtime,subject)values(73,3,current_date,'算数')",
	};
			
			
	
	protected void initData() throws SQLException {
		System.out.println("=========== initData Begin ============");
		School s=new School();
		s.setName("枫林高校");
		db.insert(s);
		s.setId(0);
		s.setName("战国高校");
		db.insert(s);
		s.setId(0);
		s.setName("秀峰高校");
		db.insert(s);
		s.setId(0);
		s.setName("大行学校");
		db.insert(s);
		s.setId(0);
		s.setName("北平学校");
		db.insert(s);

		for(String sql:prepareSql){
			db.createNativeQuery(sql).executeUpdate();
		}
		
		System.out.println("=========== initData End ============");
	}
	protected void dropTable() throws SQLException {
		System.out.println("=========== dropTable Begin ============");
		db.dropTable(Person.class);
		db.dropTable(PersonFriends.class);
		db.dropTable(School.class);
		db.dropTable(Score.class);
		if(db.getProfile().has(Feature.AUTOINCREMENT_NEED_SEQUENCE)){
			try{
				db.executeSql("drop sequence person_seq");	
			}catch(SQLException e){
				System.out.println(e.getMessage());
			}
			try{
				db.executeSql("drop sequence SCHOOL_SEQ");	
			}catch(SQLException e){
				System.out.println(e.getMessage());
			}	
		}
		System.out.println("=========== dropTable End ============");
	}
	

	protected void createtable() throws SQLException {
		System.out.println("=========== createtable Begin ============");
		db.createTable(Person.class);
		db.createTable(PersonFriends.class);
		db.createTable(School.class);
		db.createTable(Score.class);
		System.out.println("=========== createtable End ============");
	}

	public static void printPerson(Person p) {
		System.out.println("===== Name ====\nName:" + p.getName()+"("+p.getId()+")");			
		System.out.println("Gender:" + p.getGender());
		
		System.out.println("phone:" + p.getPhone());
		System.out.println("Score:" + p.getScores());
		
		System.out.println("schoolId:" + p.getSchoolId());
		System.out.println("schoolName:" + p.getSchoolName());
		System.out.println("School:" + p.getSchool());
		
		System.out.println("friends:" + p.getFriends());
		
		List<PersonFriends> friends=p.getFriends();
		if(friends!=null){
			System.out.println("friendNames:" + StringUtils.join(getFieldValuesAsString(p.getFriends(), "friend.name"),","));	
		}
		System.out.println("frientComment"+ StringUtils.join(p.getFriendComment(), ","));
		System.out.println("parentId:" + p.getParentId());
		System.out.println("parentName:" + p.getParentName());
		System.out.println("photo:"+p.getPhoto()+(p.getPhoto()==null?"":String.valueOf(p.getPhoto().length())));
	}
	

	public static String[] getFieldValuesAsString(Collection<?> entity, String fieldName) {
		List<String> result = new ArrayList<String>();
		for (Object e : entity) {
			BeanWrapperImpl bw = new BeanWrapperImpl(e);
			result.add(StringUtils.toString(bw.getNestedProperty(fieldName)));
		}
		return result.toArray(new String[result.size()]);
	}
}
