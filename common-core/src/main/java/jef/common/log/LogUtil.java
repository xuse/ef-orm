package jef.common.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import jef.common.Entry;
import jef.tools.ArrayUtils;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.JefConfiguration.Item;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;
import jef.tools.io.StringBuilderWriter;

/**
 * This class provides a logging facility. Each message is logged with a
 * priority allowing the log to be configured to filter out log messages that
 * are below a certain priority.
 */
public class LogUtil {
	public static List<Writer> otherStream;
	public static final org.slf4j.Logger log = LoggerFactory.getLogger("GeeQuery");
	
	//是否将输出改为日志形式输出
	public static boolean useSlf4j = JefConfiguration.getBoolean(Item.COMMON_DEBUG_ADAPTER, true);
	//是否为调试模式
	public static boolean debug = JefConfiguration.getBoolean(Item.DB_DEBUG, false);
	

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
		if(useSlf4j){
			log.error(msg);
		}else{
			System.err.println(msg);
		}
		showToOnthers(msg, true);
	}


	public static void error(Object o) {
		if(useSlf4j){
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
		if(useSlf4j){
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

	/**
	 * 
	 * @param o
	 * @deprecated
	 */
	
	public static void infox(Object o) {
		if(useSlf4j){
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
		infox(objs);
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
		infox(o);
	}

	public static void debug(Object o) {
		if(useSlf4j){
			if(log.isDebugEnabled()){
				String msg=toString(o);
				log.debug(msg);
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
	 * @deprecated
	 */
	public static void exception(Throwable t){
		log.error("",t);
		if (otherStream != null && !otherStream.isEmpty()) {
			showToOnthers(exceptionStack(t), true);
		}
	}
	
	/**
	 * 将异常信息输入日志
	 * @param message
	 * @deprecated
	 * @param t
	 */
	public static void exception(String message,Throwable t){
		log.error(message, t);
		if (otherStream != null && !otherStream.isEmpty()) {
			showToOnthers(exceptionStack(t), true);
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
			exceptionSummary((Throwable) o, sb);
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
	
	/**
	 * 将异常信息中的摘要输出到StringBuilder中
	 * 
	 * @param e
	 * @param sb
	 */
	public static void exceptionSummary(Throwable e, StringBuilder sb) {
		String msg = e.getLocalizedMessage();
		StackTraceElement[] stacks = e.getStackTrace();
		if (msg == null && e.getCause() != null) {
			exceptionSummary(e.getCause(), sb);
		}
		String stack = stacks.length > 0 ? stacks[0].toString() : "";
		sb.append(e.getClass().getSimpleName()).append(':').append(msg).append('\n').append(stack);
	}

	/**
	 * 返回异常信息的堆栈摘要
	 * 
	 * @param e
	 * @return
	 */
	public static String exceptionSummary(Throwable e) {
		String msg = e.getLocalizedMessage();
		StackTraceElement[] stacks = e.getStackTrace();
		if (msg == null && e.getCause() != null) {
			msg = exceptionSummary(e.getCause());
		}
		String stack = stacks.length > 0 ? stacks[0].toString() : "";
		return StringUtils.concat(e.getClass().getSimpleName(), ":", msg, "\r\n", stack);
	}

	/**
	 * 將错误堆栈信息转换为String
	 * 
	 * @param e
	 * @param pkgStart
	 * @return
	 */
	public static String exceptionStack(Throwable e, final String... pkgStart) {
		return exceptionStack("\r\n", e, pkgStart);
	}

	/**
	 * 将异常堆栈信息转换为String
	 * 
	 * @param cr       换行符
	 * @param e        异常
	 * @param pkgStart 包的开头描述
	 * @return
	 */
	public static String exceptionStack(final String cr, Throwable e, final String... pkgStart) {
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w) {
			@Override
			public void println() {
			}

			@Override
			public void write(String x) {
				x = StringUtils.rtrim(x, '\r', '\n', '\t');
				if (x.length() == 0) {
					return;
				}
				if (pkgStart.length == 0) {
					super.write(x, 0, x.length());
					super.write(cr, 0, cr.length());
					return;
				}
				String y = x.trim();
				if (!y.startsWith("at ")) {
					super.write(x, 0, x.length());
					super.write(cr, 0, cr.length());
					return;
				}
				for (String s : pkgStart) {
					if (StringUtils.matchChars(y, 3, s)) {
						super.write(x, 0, x.length());
						super.write(cr, 0, cr.length());
						return;
					}
				}
			}
		});
		w.flush();
		IOUtils.closeQuietly(w);
		return w.getBuffer().toString();
	}
}
