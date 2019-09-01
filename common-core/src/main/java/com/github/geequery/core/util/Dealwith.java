package com.github.geequery.core.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import jef.tools.IOUtils;

public interface Dealwith {

	public abstract class OutputableImpl implements Dealwith {
		private final Function<File, File> func;
		private Function<Charset, Charset> tCharset;
		
		/**
		 * 指定输出文件的字符集，默认和输入文件相同<br>
		 * 阶段：在文件处理开始前，getTarget方法之前执行。<br>
		 * 影响：控制输出文件的编码，输出null表示输出文件和输入文件编码一致<br>
		 * 
		 * @return
		 */
		public Charset targetCharset(Charset sourceCharset) {
			return tCharset == null ? sourceCharset : tCharset.apply(sourceCharset);
		};

		public File getTarget(File source) {
			return func == null ? new File(source.getPath().concat(".tmp")) : func.apply(source);
		}

		OutputableImpl(Function<File, File> func, Function<Charset, Charset> charset) {
			this.func = func;
			this.tCharset = charset;
		}

		public OutputableImpl charset(String toCharset) {
			if (toCharset != null) {
				this.tCharset = e -> Charset.forName(toCharset);
			}
			return this;
		}
	}

	public static final Dealwith DELETE = new Dealwith() {
		@Override
		public File onSuccess(File source, File target) {
			source.delete();
			return target;
		}
	};

	public static OutputableImpl replaceAndBackup(String charset) {
		final Charset charSet;
		if (StringUtils.isNotEmpty(charset)) {
			charSet = Charset.forName(charset);
		} else {
			charSet = null;
		}
		return new ReplaceImpl(null, charSet == null ? null : e -> charSet);
	}

	public static ReplaceImpl replace(String charset) {
		final Charset charSet;
		if (StringUtils.isNotEmpty(charset)) {
			charSet = Charset.forName(charset);
		} else {
			charSet = null;
		}
		return new ReplaceImpl(null, charSet == null ? null : e -> charSet);
	}

	public static OutputableImpl output(Function<File, File> lambda) {
		return new NONE(lambda, null);
	}

	public static OutputableImpl output(File out) {
		return new NONE(e -> out, null);
	}

	public static final Dealwith REPLACE = new ReplaceImpl(null, null);

	final class ReplaceImpl extends OutputableImpl {
		private boolean backup;
		ReplaceImpl(Function<File, File> func, Function<Charset, Charset> charsets) {
			super(func, charsets);
		}

		public ReplaceImpl backupSource(boolean backup) {
			this.backup=backup;
			return this;
		}
		
		@Override
		public File onSuccess(File source, File target) {
			boolean sourceSolution;
			if(backup) {
				File backupfile = new File(source.getParent(), source.getName() + ".bak");
				backupfile = IOUtils.escapeExistFile(backupfile);
				sourceSolution=source.renameTo(backupfile);
			}else {
				sourceSolution=source.delete();
			}
			if (sourceSolution) {
				File n = new File(source.getPath());
				target.renameTo(n);
				return n;
			} else {
				return target;
			}
		}
	};


	public static final Dealwith BACKUP_REPLACE = new ReplaceImpl(null,null).backupSource(true);

	final class NONE extends OutputableImpl {
		NONE(Function<File, File> func, Function<Charset, Charset> charsets) {
			super(func, charsets);
		}

		@Override
		public File onSuccess(File source, File target) {
			return target;
		}
	}

	public static final Dealwith NONE = new NONE(null, null);

	public static final Dealwith NO_OUTPUT = new Dealwith() {
		@Override
		public File onSuccess(File source, File target) {
			return null;
		}
	};

	File onSuccess(File source, File target);

}