package jef.database.dialect;

import jef.database.meta.FunctionMapping;
import jef.database.query.Func;
import jef.tools.Assert;

import org.junit.Test;

/**
 * 检查方言的设计
 * @author jiyi
 *
 */
public class DbDialectTest {

	/**
	 * 检查每种方言是否都提供了基本函数的实现
	 */
	@Test
	public void checkNormalFunctions(){
		DatabaseDialect dialect=DbmsProfile.getProfile("oracle");
		check(dialect);
		
		dialect=DbmsProfile.getProfile("derby");
		check(dialect);
		
		dialect=DbmsProfile.getProfile("mysql");
		check(dialect);
		
		dialect=DbmsProfile.getProfile("postgresql");
		check(dialect);
		
		dialect=DbmsProfile.getProfile("hsqldb");
		check(dialect);
		
		dialect=DbmsProfile.getProfile("sqlite");
		check(dialect);
	}

	private void check(DatabaseDialect dialect) {
		Class<Func> clz=Func.class;
		for(Func func:clz.getEnumConstants()){
			FunctionMapping mapping=dialect.getFunctionsByEnum().get(func);
			Assert.notNull(mapping,func+" 在数据库"+ dialect.getName()+" 中难道无法提供吗？");
			
			if(mapping.getMatch()==0){
				if(!func.name().equals(mapping.getFunction().getName()))throw new IllegalArgumentException(func+" 在数据库"+ dialect.getName()+" 中的名称是不一致的."+mapping.getFunction().getName());		
				
			}
			System.out.println(func +" \t"+ mapping.getFunction().getName());
			
		}
	}
	
	@Test
	public void testEmu(){
//		NvlFunction n=new NvlFunction();
//		System.out.println(n.renderExpression(ArrayUtils.asList((Expression)new StringValue("'11'"),new StringValue("'22'"),new StringValue("'33'"),new StringValue("'44'"))));	
	}
	
}
