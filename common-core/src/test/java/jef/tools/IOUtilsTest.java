package jef.tools;

public class IOUtilsTest {
	public static void main(String[] args) {
		String n1=".sjfdnsdj";
		String n2="asdas.yxy.txt";
		String n3="fsdfs.TXT";
		String n4="sdfmwsjfldsfds";
		FileName f=new FileName(n1);
		System.out.println(f.getMain());
		System.out.println(f.getExt());
		
		f=new FileName(n2);
		f.append("(part2)");
		System.out.println(f.getMain());
		

		
		f=new FileName(n3);
		System.out.println(f.getMain());
		System.out.println(f.getExt());
		
		f=new FileName(n4);
		System.out.println(f.getMain());
		System.out.println(f.getExt());
		
	}
}
