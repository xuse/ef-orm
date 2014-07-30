package jef.database.support;

import java.io.File;

import jef.database.ORMConfig;
import jef.database.dialect.type.BlobStringMapping;
import jef.tools.StringUtils;

public abstract class LogFormat {
	/**
	 * 记录日志
	 * 
	 * @param n
	 * @param fieldName
	 * @param value
	 */
	public abstract void log(StringBuilder logMessage, int count, String fieldName, Object value);

	
	public static class NowrapLineLogFormat extends LogFormat {
		public void log(StringBuilder logMessage, int count, String fieldName, Object value) {
			if(count==1){
				logMessage.append(" [");
			}else{
				logMessage.append(" ");
			}
			logMessage.append('(').append(count).append(')');
			if (value == null) {
				logMessage.append("null");
			} else if (value.getClass() == byte[].class) {
				logMessage.append(((byte[]) value).length).append(" bytes");
			} else {
				String valStr = String.valueOf(value);
				if (valStr.length() > 40) {// 如果日志太长是不行的
					logMessage.append(valStr.substring(0, 38)).append("..");
					if (ORMConfig.getInstance().isShowStringLength()) {
						logMessage.append(" len=").append(BlobStringMapping.getLength(valStr));
					} else {
						logMessage.append(" len=").append(valStr.length());
					}
				} else {
					logMessage.append(valStr);
				}
			}
		}
	}

	public static class Default extends LogFormat {
		public void log(StringBuilder logMessage, int count, String fieldName, Object value) {
			int start = logMessage.length();
			logMessage.append('\n');
			logMessage.append('(').append(count).append(')');
			logMessage.append(fieldName).append(':');
			start = logMessage.length() - start;
			if (start < 18) {
				StringUtils.repeat(logMessage, ' ', 18 - start);
			}
			if (value == null) {
				logMessage.append("\tnull");
				return;
			}
			Class<?> vClass=value.getClass();
			if (vClass == byte[].class) {
				logMessage.append("\t").append(((byte[]) value).length).append(" bytes");
			} else if (vClass== File.class) {
				logMessage.append("\t").append(String.valueOf(value)).append('(').append(((File) value).length()).append(" bytes)");
			} else {
				String valStr = String.valueOf(value);
				if (valStr.length() > 40) {// 如果日志太长是不行的
					logMessage.append("\t[").append(valStr.substring(0, 38)).append("..]");
					if (ORMConfig.getInstance().isShowStringLength()) {
						logMessage.append(" Length=").append(BlobStringMapping.getLength(valStr));
					} else {
						logMessage.append(" chars=").append(valStr.length());
					}
				} else {
					logMessage.append("\t[").append(valStr).append(']');
				}
			}
		}
	}
}
