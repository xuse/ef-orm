package jef.database.pooltest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


import org.slf4j.LoggerFactory;

/**
 * 读取配置文件的工具类
 * @author zhaolong
 *
 */
public class ConnDBConfigUtil {
	
	private static Properties p = null;
	private static org.slf4j.Logger log=LoggerFactory.getLogger(ConnDBConfigUtil.class);
	
	
	
	static{
		p=new Properties();
		InputStream input=null;
		if(ConnPrintOutUtil.isOnServer()){
			//如果在服务器上 则读取case in目录下的db.properties
			try {
				input = new FileInputStream(new File("../in/db.properties"));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}else{
			//否则读取src/test/resource中的内容
			input=ConnDBConfigUtil.class.getClassLoader().getResourceAsStream("db.properties");
		}
		try {
			p.load(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static String getStringValue(String key){
		String value=p.getProperty(key);
		return value;
	}
	
	//获取propterties中的内容
	public static String getStringValue(String key,String defaultValue){
		String value=getStringValue(key);
		if(value!=null && !"".equals(value)){
			return value;
		}else{
			return defaultValue;
		}
	}
	
	public static int getIntValue(String key){
		String value=p.getProperty(key);
		return Integer.parseInt(value);
	}
	
	
	//获取propterties中的内容
	public static int getIntValue(String key,int defaultValue){
		Integer value=getIntValue(key);
		if(value!=null){
			return value;
		}else{
			return defaultValue;
		}
	}


}
