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
	
	/**
	 * 构造
	 * @param name
	 */
	public FileName(String name){
		this.name=name;
		this.index=name.lastIndexOf('.');
		if(index==-1)index=name.length();
	}
	/**
	 * 将文件名和扩展名拆成两个部分
	 * @return 数组，[0]是文件名部分 [1]是扩展名
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
	 * @return this
	 */
	public FileName append(String append){
		if(append==null)append="";
		name=name.substring(0,index)+append+name.substring(index);
		index=index+append.length();
		return this;
	}
	
	/**
	 * 在文件名整体的右侧加上文字
	 * @param append 内容
	 * @return this
	 */
	public FileName appendAtLast(String append){
		if(append==null)append="";
		name=name.concat(append);
		return this;
	}
	
	/**
	 * 假设文件名后添加了指定后缀后的结果<br>
	 * 不改变当前对象，返回“如果”在右侧加上某个文本之后的整体文件名。
	 * @param append 
	 * @return 虚构文件名
	 */
	public String getValueIfAppend(String append){
		if(append==null || append.length()==0)return name;
		return new StringBuilder(name.length()+append.length()).append(name.subSequence(0, index))
				.append(append).append(name.substring(index))
				.toString();
	}
	
	/**
	 * 当右侧加上某段文本后，构造成file
	 * @param append 假设添加的文字
	 * @return 文件对象
	 */
	public File getFileIfAppend(String append){
		return new File(getValueIfAppend(append));
	}
	
	/**
	 * 当右侧加上某段文本后，构造成file
	 * @param parent 上级文件夹
	 * @param append 假设添加的文字
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
}
