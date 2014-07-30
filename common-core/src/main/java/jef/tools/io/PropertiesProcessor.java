package jef.tools.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import jef.http.client.support.CommentEntry;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.TextFileCallback;

/**
 * 辅助对properties进行修改
 * @author Administrator
 *
 */
public abstract class PropertiesProcessor extends TextFileCallback{
	CharSequence lastRecord;

	public static int indexOf(CharSequence cq,char c){
		for(int i=0;i<cq.length();i++){
			if(cq.charAt(i)==c){
				return i;
			}
		}
		return -1;
	}

	@Override
	protected Dealwith dealwithSourceOnSuccess(File source) {
		return Dealwith.REPLACE;
	}

	@Override
	protected final String processLine(String s) {
		if (StringUtils.isBlank(s) || s.startsWith("#")){
			return s;
		}
		
		if(lastRecord!=null){
			((StringBuilder)lastRecord).append(s);	
		}else{
			lastRecord=new StringBuilder(s);
		}
		
		if(s.endsWith("\\")){//继续到下一行，存储起来			
			return null;
		}
		
		int index = indexOf(lastRecord,'=');
		if (index == -1)
				return lastRecord.toString();//未知行，返回
		
		String key = lastRecord.subSequence(0, index).toString();
		String value = s.substring(index+1).trim();
		lastRecord=null;
		CommentEntry e=new CommentEntry(key,value);
		e=processEntry(e);
		if(e!=null && e.getKey()!=null){
			if(e.getValue()==null){
				return e.getKey();	
			}else{
				return e.getKey().concat("=").concat(e.getValue());
			}
		}
		return null;
	}


	@Override
	protected void afterProcess(File source,File target,BufferedWriter w) throws IOException {
		Map<String,String> map=getAddingEntries();
		if(map!=null){
			IOUtils.storeProperties(w,map,false);	
		}
	}

	/**
	 * 得到要添加的项目
	 * @return
	 */
	protected abstract Map<String, String> getAddingEntries();

	/**
	 * 子类继承，访问properties的中一项
	 * @param e
	 * @return 
	 * 		null表示删除此项，
	 * 		CommentEntry表示替换此项
	 * 		CommentEntry只有key并且以#开头可以表示注释
	 */
	protected abstract CommentEntry processEntry(CommentEntry e);
}
