package jef.tools;

import java.io.File;


/**
 * 文件名辅助操作工具
 * @author Administrator
 *
 */
public class FileName {
	private String name;
	private int index;
	
	public FileName(String name){
		this.name=name;
		this.index=name.lastIndexOf('.');
		if(index==-1)index=name.length();
	}
	/**
	 * 将文件名和扩展名拆成两个部分
	 * @return
	 */
	public String[] getAsArray(){
		return new String[]{getMain(),(index==name.length()?"":name.substring(index+1))};
	}
	
	/**
	 * 得到文件名主体部分
	 * @return
	 */
	public String getMain() {
		return name.substring(0,index);
	}
	/**
	 * 得到扩展名
	 * @return 总是小写
	 */
	public String getExt() {
		if(index==name.length())return "";
		return name.substring(index+1).toLowerCase();
	}
	
	/**
	 * 得到原始扩展名，包含点，并且保留原始大小写
	 */
	public String getRawExt(){
		return name.substring(index);
	}
	
	/**
	 * 在文件名右侧(扩展名左侧)加上文字
	 * @param append
	 */
	public FileName append(String append){
		if(append==null)append="";
		name=name.substring(0,index)+append+name.substring(index);
		index=index+append.length();
		return this;
	}
	
	/**
	 * 在文件名整体，右侧加上文字
	 * @param append
	 * @return
	 */
	public FileName appendAtlast(String append){
		if(append==null)append="";
		name=name.concat(append);
		return this;
	}
	
	/**
	 * 不改变当前对象，返回“如果”在右侧加上某个文本之后的整体文件名。
	 * 
	 * @param append
	 * @return
	 */
	public String getValueIfAppend(String append){
		if(append==null || append.length()==0)return name;
		return new StringBuilder(name.length()+append.length()).append(name.subSequence(0, index))
				.append(append).append(name.substring(index))
				.toString();
	}
	
	/**
	 * 当右侧加上某段文本后，构造成file
	 * @param append
	 * @return
	 */
	public File getFileIfAppend(String append){
		return new File(getValueIfAppend(append));
	}
	
	/**
	 * 当右侧加上某段文本后，构造成file
	 * @param parent 上级文件夹
	 * @param append
	 * @return
	 */
	public File getFileIfAppend(String parent,String append){
		return new File(parent,getValueIfAppend(append));
	}
	
	/**
	 * 在文件名左侧加上文字
	 */
	public FileName appendLeft(String append){
		if(append==null)append="";
		name=append.concat(name);
		index=index+append.length();
		return this;
	}
	
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * 设置扩展名
	 * @param extName
	 */
	public void setExt(String extName){
		boolean empty=StringUtils.isEmpty(extName);
		if(empty){
			name=name.substring(0,index);
		}else{
			name=name.substring(0,index)+"."+extName;	
		}
	}
	
	/**
	 * 设置文件名主体
	 * @param main
	 */
	public void setName(String main){
		if(main==null)main="";
		name=main+name.substring(index);
		index=main.length();
	}
	
	/**
	 * 将文件名拆成名称和扩展名两部分
	 * 
	 * @param name
	 * @return
	 */
	public final static String[] splitExt(String name) {
		int n = name.lastIndexOf('.');
		if (n == -1) {
			return new String[] { name, "" };
		} else {
			return new String[] { name.substring(0, n),
					name.substring(n + 1).toLowerCase() };
		}
	}
	
//	
//	public static void main(String[] args) {
//		String n1=".sjfdnsdj";
//		String n2="asdas.yxy.txt";
//		String n3="fsdfs.TXT";
//		String n4="sdfmwsjfldsfds";
//		FileName f=new FileName(n1);
//		System.out.println(f.getMain());
//		System.out.println(f.getExt());
//		
//		f=new FileName(n2);
//		f.append("(part2)");
//		System.out.println(f.getMain());
//		
//
//		
//		f=new FileName(n3);
//		System.out.println(f.getMain());
//		System.out.println(f.getExt());
//		
//		f=new FileName(n4);
//		System.out.println(f.getMain());
//		System.out.println(f.getExt());
//		
//	}
}
