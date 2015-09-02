package jef.common.log;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.common.Entry;
import jef.tools.ArrayUtils;
import jef.tools.JefConfiguration;
import jef.tools.JefConfiguration.Item;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;
import jef.tools.io.StringBuilderWriter;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * This class provides a logging facility. Each message is logged with a
 * priority allowing the log to be configured to filter out log messages that
 * are below a certain priority.
 */
public class LogUtil {
	public static List<Writer> otherStream;
	private static org.slf4j.Logger log = LoggerFactory.getLogger("GeeQuery");
	
	//是否将输出改为日志形式输出
	public static boolean commonDebugAdapter = JefConfiguration.getBoolean(Item.COMMON_DEBUG_ADAPTER, false);
	//是否为调试模式
	public static boolean debug = JefConfiguration.getBoolean(Item.DB_DEBUG, false);
	
	
	//将标志输出替换，从而实现System.out.print重定向到日志。
	static {
		if(commonDebugAdapter){
			boolean redirect = JefConfiguration.getBoolean(Item.SYSOUT_REDIRECT, false);
			try {
				//是否使用控制台输出重定向到到Logger的功能
				log.info("=== The System.out.redirect function is "+(redirect?"Enabled.":"Disabled."));
				if(redirect && !(System.out instanceof LogPrintStream)){
					org.slf4j.Logger ll=LoggerFactory.getLogger("SYSTEM");
					PrintStream sysout=new LogPrintStream(ll,false,System.out);
					PrintStream syserr=new LogPrintStream(ll,true,System.err);
					System.setOut(sysout);
					System.setErr(syserr);//将系统的标准输出替换为自己的输出
				}
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public static void appendBytesString(StringBuilder sb,byte[] value){
		sb.append("          -0 -1 -2 -3 -4 -5 -6 -7 -8 -9 -A -B -C -D -E -F\r\n");
		int left=value.length;
		int i=0;
		try{
			while(left>16){
				String name=Integer.toHexString(i);
				int offset=i*16;
				name=StringUtils.leftPad(name, 7,'0').concat("0: ");
				sb.append(name).append(StringUtils.join(value, ' ',offset,16)).append(" ; ");
				String text=new String(value,offset,16,"ISO-8859-1");
				text=StringUtils.replaceChars(text, "\r\n\t", "...");
				sb.append(text);
				sb.append("\r\n");
				left-=16;
				i++;
			}
			if(left>0){
				int offset=i*16;
				String name=Integer.toHexString(i);
				
				name=StringUtils.leftPad(name, 7,'0').concat("0: ");
				sb.append(name).append(StringUtils.join(value, ' ',offset,16));
				for(int x=left;x<16;x++){
					sb.append("   ");	
				}
				String text=new String(value,offset,left,"ISO-8859-1");
				text=StringUtils.replaceChars(text, "\r\n\t", "...");
				sb.append(" ; ").append(text);
			}	
		}catch(UnsupportedEncodingException e){
			//NEVER Happens
			e.printStackTrace();
		}
	}
	
	/**
	 * 添加一个输出流
	 * 
	 * @param p
	 */
	public static void addOutput(Writer p) {
		if (p == null)
			return;
		synchronized (LogUtil.class) {
			if (otherStream == null){
				otherStream = new ArrayList<Writer>();		
			}else if(otherStream.contains(p)){
				return;
			}
			otherStream.add(p);
			
		}
	}

	/**
	 * 删除一个输出流
	 * 
	 * @param p
	 */
	public static void removeOutput(Writer p) {
		if (p == null)
			return;
		synchronized (LogUtil.class) {
			otherStream.remove(p);
		}
	}
	
	
	public static void fatal(Object o) {
		String msg=toString(o);
		if(commonDebugAdapter){
			log.error(msg);
		}else{
			System.err.println(msg);
		}
		showToOnthers(msg, true);
	}


	public static void error(Object o) {
		if(commonDebugAdapter){
			if(log.isErrorEnabled()){
				String msg=toString(o);
				log.error(msg);
				showToOnthers(msg, true);
			}
		}else{
			String msg=toString(o);
			System.err.println(msg);
			showToOnthers(msg, true);
		}
	}

	public static void error(String s,Object... o) {
		log.error(s,o);
	}
	
	/**
	 * 以标准的slf4j的格式输出warn
	 * @param s
	 * @param o
	 */
	public static void warn(String s,Object... o) {
		log.warn(s,o);
	}
	
	/**
	 * 以标准的slf4j的格式输出info
	 * @param s
	 * @param o
	 */
	public static void info(String s,Object... o){
		log.info(s,o);
	}
	
	/**
	 * 以标准的slf4j的格式输出debug
	 */
	public static void debug(String s,Object... o){
		log.debug(s,o);
	}
	
	public static void warn(Object o) {
		if(commonDebugAdapter){
			if(log.isWarnEnabled()){
				String msg=toString(o);
				log.warn(msg);
				showToOnthers(msg, true);
			}
		}else {
			String msg=toString(o);
			System.err.println(msg);
			showToOnthers(msg, true);
		}
	}

	public static void info(Object o) {
		if(commonDebugAdapter){
			if(log.isInfoEnabled()){
				String msg=toString(o);
				log.info(msg);
				showToOnthers(msg, true);
			}
		}else{
			String msg=toString(o);
			System.out.println(msg);
			showToOnthers(msg, true);
		}
	}


	/**
	 * 将指定的对象显示输出
	 * @param objs
	 */
	public static void shows(Object... objs) {
		info(objs);
	}

	public static void show(ResultSet rs) {
		try{
			show((Object)rs);
		}finally{
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
	}
	
	
	/**
	 * 将指定的对象显示输出
	 * @param o
	 */
	public static void show(Object o) {
		info(o);
	}

	public static void debug(Object o) {
		if(commonDebugAdapter){
			if(log.isDebugEnabled()){
				String msg=toString(o);
				log.error(msg);
				showToOnthers(msg, true);
			}
		}else if(debug){
			String msg=toString(o);
			System.out.println(msg);
			showToOnthers(msg, true);
		}
	}

	
	/**
	 * 将异常异常堆栈打入日志
	 * 改起来影响比较大，所以就不改了。
	 * @param t
	 */
	public static void exception(Throwable t){
		log.error("",t);
		if (otherStream != null && !otherStream.isEmpty()) {
			showToOnthers(StringUtils.exceptionStack(t), true);
		}
	}
	
	/**
	 * 将异常信息输入日志
	 * @param message
	 * @param t
	 */
	public static void exception(String message,Throwable t){
		log.error(message, t);
		if (otherStream != null && !otherStream.isEmpty()) {
			showToOnthers(StringUtils.exceptionStack(t), true);
		}
	}

	/**
	 * 將各种对象轉換為文本
	 * 
	 * @param o
	 */
	@SuppressWarnings("rawtypes")
	public static void toString(Object o, StringBuilder sb) {
		if (o == null){
			return;
		}
		Class<?> c = o.getClass();
		if (c==String.class) {
			sb.append( (String) o);
		} else if (c.isArray()) {
			if (c.getComponentType() == Byte.TYPE) {//如果是byte[]，就打印出像UltraEdit的二进制文件编辑那种数据对照格式
				sb.append("ByteArray:\n");
				appendBytesString(sb,(byte[])o);
			} else if (c.getComponentType().isPrimitive()) {
				StringUtils.joinTo(ArrayUtils.toObject(o), ' ', sb);
			} else {
				Object[] objs=(Object[]) o;
				if(objs.length==0)return;
				sb.append(objs[0]);
				for(int i=1;i<objs.length;i++){
					sb.append('\n').append(objs[i]);
				}
			}
		} else if (o instanceof Iterable<?>) {
			Iterator iter=((Iterable) o).iterator();
			if(iter.hasNext()){
				sb.append(iter.next());
				for (;iter.hasNext();) {
					sb.append('\n').append(iter.next());
				}
			}
		} else if (o instanceof Enumeration<?>) {
			Enumeration enu=(Enumeration)o;
			if(enu.hasMoreElements()){
				sb.append(enu.nextElement());
				for(;enu.hasMoreElements();){
					sb.append('\n').append(enu.nextElement());
				}
			}
		} else if (o instanceof Map<?, ?>) {
			Map map = (Map) o;
			@SuppressWarnings("unchecked")
			Iterator<Map.Entry> iter=map.entrySet().iterator();
			if(iter.hasNext()){
				Map.Entry e=iter.next();
				sb.append(StringUtils.rightPad(StringUtils.toString(e.getKey()), 18)).append('\t').append(toString(e.getValue()));
				for (;iter.hasNext();) {
					e=iter.next();
					sb.append('\n').append(StringUtils.rightPad(StringUtils.toString(e.getKey()), 18)).append('\t').append(toString(e.getValue()));
				}
			}
		} else if (o instanceof Node) {
			try {
				XMLUtils.output((Node)o, new StringBuilderWriter(sb), null, 4);
			} catch (IOException e) {
				e.printStackTrace();
				sb.append(o);
			}
		} else if (o instanceof Entry<?, ?>) {
			Entry<?, ?> e = (Entry<?, ?>) o;
			sb.append(StringUtils.rightPad(StringUtils.toString(e.getKey()), 18)).append('\t').append(toString(e.getValue()));
		} else if (o instanceof Throwable) {
			StringUtils.exceptionSummary((Throwable) o, sb);
		} else {
			sb.append(StringUtils.toString(o));
		}
	}

	public static CharSequence toString(String head,byte[] b){
		StringBuilder sb=new StringBuilder(head.length()+b.length*4+16);
		sb.append(head);
		appendBytesString(sb, b);
		return sb;
	}
	
	//不关闭
	private static String toString(ResultSet rs) throws SQLException {
		StringBuilder sb=new StringBuilder(64);
		int limit=JefConfiguration.getInt(Item.CONSOLE_SHOW_RESULT_LIMIT, 200);
		ResultSetMetaData meta=rs.getMetaData();
		int count=meta.getColumnCount();
		
		sb.append(meta.getColumnLabel(1));
		for(int i=2;i<=count;i++){
			sb.append(", ");
			sb.append(meta.getColumnLabel(i));
		}
		sb.append('\n');
		int size=0;
		while(rs.next()){
			size++;
			sb.append('[');
			sb.append(rs.getObject(1));
			for(int i=2;i<=count;i++){
				sb.append(", ");
				sb.append(rs.getObject(i));
			}
			sb.append("]\n");
			if(limit==size){//No need to print...
				while(rs.next()){
					size++;
				}
				break;
			}
		}
		
		sb.append("Total:").append(size).append(" record(s).");
		return sb.toString();
		
	}
	
	private static String toString(Object object) {
		if(object==null)return "";
		@SuppressWarnings("rawtypes")
		Class clz=object.getClass();
		if(clz==String.class)return (String)object;
		if(object.getClass().isArray() || Collection.class.isAssignableFrom(clz) || Enumeration.class.isAssignableFrom(clz)||Node.class.isAssignableFrom(clz)||Map.class.isAssignableFrom(clz)){
			StringBuilder sb=new StringBuilder();
			toString(object, sb);
			return sb.toString();
		}else if(object instanceof Throwable){
			StringBuilder sb=new StringBuilder();
			toString(object, sb);
			return sb.toString();
		}else if(object instanceof ResultSet){
			ResultSet rs=(ResultSet)object;
			try{
				return toString(rs);	
			}catch(SQLException e){
				throw new RuntimeException(e);
			}
		}else{
			return StringUtils.toString(object);
		}
	}

	public static void showToOnthers(String msg, boolean withNewLine) {
		if (otherStream != null) {
			for (Iterator<Writer> iter = otherStream.iterator(); iter.hasNext();) {
				Writer out = iter.next();
				try {
					if (withNewLine) {
						out.write(msg);
						out.write(StringUtils.CRLF_STR);
					} else {
						out.write(msg);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
					System.out.print("Will not send display message to " + out.toString());
					iter.remove();
				}
			}
		}
	}

	public static Writer[] getOtherOutput() {
		Writer[] r = new Writer[0];
		if (otherStream == null)
			return r;
		return otherStream.toArray(r);
	}

	public static boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}
}
