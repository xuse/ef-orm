import jef.tools.reflect.ConvertUtils;


public class AAA {
	public static void main(String[] args) {
		int a=Integer.parseInt(args[0]);
		if(a==1) {
			System.out.println("1");
		}else {
			System.out.println("2");
		}
		System.out.println("333");
		
		
		ConvertUtils.toProperType("1", int.class);
		
		ConvertUtils.toProperType("1", Integer.TYPE);
	}
}
