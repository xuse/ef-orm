package jef.common.log;

import jef.tools.StringUtils;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

@SuppressWarnings("serial")
public abstract class AbstractLogger extends MarkerIgnoringBase implements org.slf4j.Logger {
	public final static int OFF_INT = Integer.MAX_VALUE;
	public final static int FATAL_INT = 50000;
	public final static int ERROR_INT = 40000;
	public final static int WARN_INT = 30000;
	public final static int INFO_INT = 20000;
	public final static int DEBUG_INT = 10000;
	public final static int ALL_INT = Integer.MIN_VALUE;
	
	protected int level = INFO_INT;
	
	abstract protected  void log(Object... obj);

	public final boolean isTraceEnabled() {
		return level > 0;
	}

	public final boolean isDebugEnabled() {
		return level >= DEBUG_INT;
	}

	public final boolean isErrorEnabled() {
		return level >= ERROR_INT;
	}

	public final boolean isInfoEnabled() {
		return level >= INFO_INT;
	}

	public final boolean isWarnEnabled() {
		return level >= WARN_INT;
	}


	public String getName() {
		return this.getClass().getName();
	}
	
	public void trace(String message) {
		if (this.isTraceEnabled()) {
			log(StringUtils.toString(message));
		}
	}


	public void trace(String message, Throwable t) {
		if (this.isTraceEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void debug(String message) {
		if (this.isDebugEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void debug(String message, Throwable t) {
		if (this.isDebugEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void info(String message) {
		if (this.isInfoEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void info(String message, Throwable t) {
		if (this.isInfoEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void warn(String message) {
		if (this.isWarnEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void warn(String message, Throwable t) {
		if (this.isWarnEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void error(String message) {
		if (this.isErrorEnabled()) {
			log(StringUtils.toString(message));
		}
	}

	public void error(String message, Throwable t) {
		if (this.isErrorEnabled()) {
			log(StringUtils.toString(message));
		}
	}


	public void debug(String s, Object obj) {
		if (this.isDebugEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj);
			log(StringUtils.toString(f.getMessage()));
		}
	}
	
	public void debug(String s, Object obj, Object obj1) {
		if (this.isDebugEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj,obj1);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void debug(String s, Object... aobj) {
		if (this.isDebugEnabled()) {
			FormattingTuple f=MessageFormatter.arrayFormat(s,aobj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void error(String s, Object obj) {
		if (this.isErrorEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj);
			log(StringUtils.toString(f.getMessage()));
		}
	}
	
	public void error(String s, Object obj, Object obj1) {
		if (this.isErrorEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj,obj1);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void error(String s, Object... aobj) {
		if (this.isErrorEnabled()) {
			FormattingTuple f=MessageFormatter.arrayFormat(s,aobj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void info(String s, Object obj, Object obj1) {
		if (this.isInfoEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj,obj1);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void info(String s, Object... aobj) {
		if (this.isInfoEnabled()) {
			FormattingTuple f=MessageFormatter.arrayFormat(s,aobj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void info(String s, Object obj) {
		if (this.isInfoEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void trace(String s, Object obj, Object obj1) {
		if (this.isTraceEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj,obj1);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void trace(String s, Object... aobj) {
		if (this.isTraceEnabled()) {
			FormattingTuple f=MessageFormatter.arrayFormat(s,aobj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void trace(String s, Object obj) {
		if (this.isTraceEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void warn(String s, Object obj) {
		if (this.isWarnEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj);
			log(StringUtils.toString(f.getMessage()));
		}
	}
	
	public void warn(String s, Object obj, Object obj1) {
		if (this.isWarnEnabled()) {
			FormattingTuple f=MessageFormatter.format(s,obj,obj1);
			log(StringUtils.toString(f.getMessage()));
		}
	}

	public void warn(String s, Object... aobj) {
		if (this.isWarnEnabled()) {
			FormattingTuple f=MessageFormatter.arrayFormat(s,aobj);
			log(StringUtils.toString(f.getMessage()));
		}
	}
}
