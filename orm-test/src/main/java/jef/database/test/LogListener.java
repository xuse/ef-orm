package jef.database.test;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jef.common.log.LogUtil;

/**
 * 这个类可以监听数据库日志，在单元测试中可以使用正则表达式从日志中提取需要验证的信息。
 * @author jiyi
 *
 */
public class LogListener extends Writer{
	private Pattern pattern;
	private final List<String[]> sqls=new ArrayList<String[]>();
	
	/**
	 * 构造
	 * @param regexp 要提取的日志信息的正则表达式
	 */
	public LogListener(String regexp) {
		this.pattern=Pattern.compile(regexp);
		LogUtil.addOutput(this);
	}
	/**
	 * 
	 * @param regexp 要提取的日志信息的正则表达式
	 * @param flag
	 */
	public LogListener(String regexp,int flag) {
		this.pattern=Pattern.compile(regexp,flag);
		LogUtil.addOutput(this);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		String s=new String(cbuf,off,len);
		Matcher m=pattern.matcher(s);
		if(m.matches()){
			int n=m.groupCount();
			if(n==0){
				sqls.add(new String[]{m.group()});
			}else{
				String[] result=new String[n];
				for(int i=1;i<=n;i++){
					result[i-1]=m.group(i);
				}
				sqls.add(result);
			}
		}
	}

	/**
	 * 返回匹配的日志中的各个匹配项。
	 * 
	 * 调用此方法后，监听器不再监听。
	 * @return 匹配的日志必须仅有一条，如果没有发现匹配日志或者有多条将抛出异常。
	 */
	public String[] getSingleMatch(){
		stop();
		if(sqls.isEmpty()){
			throw new IllegalArgumentException("No Match Log found for "+ pattern);
		}else if(sqls.size()>1){
			throw new IllegalArgumentException("There are "+sqls.size()+" Matches Log found for "+ pattern);
		}
		return sqls.remove(sqls.size()-1);
	}
	/**
	 * 返回所有的匹配项
	 * 
	 * 调用此方法后，监听器不再监听。
	 * @return
	 */
	public List<String[]> getAllMatches(){
		stop();
		List<String[]> result=new ArrayList<String[]>(sqls);
		sqls.clear();
		return result;
	}
	/**
	 * 返回最后一个匹配项
	 * 
	 * 调用此方法后，监听器还将继续监听。
	 * @return
	 */
	public String[] getLastMatch(){
		if(sqls.isEmpty()){
			throw new IllegalArgumentException("No Match Log found for "+ pattern);
		}
		return sqls.remove(sqls.size()-1);
	}
	
	/**
	 * 返回最后一个匹配项
	 * 调用此方法后，监听器还将继续监听。
	 * @return
	 */
	public String getLastMatchString(){
		if(sqls.isEmpty()){
			throw new IllegalArgumentException("No Match Log found for "+ pattern);
		}
		return sqls.remove(sqls.size()-1)[0];
	}
	
	public void close(){
		stop();
		sqls.clear();
		pattern=null;
	}
	
	private void stop() {
		LogUtil.removeOutput(this);
	}

	@Override
	public void flush() throws IOException {
	}

}
