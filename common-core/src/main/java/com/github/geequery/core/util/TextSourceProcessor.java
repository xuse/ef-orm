package com.github.geequery.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.IOUtils;

public class TextSourceProcessor implements FileProcessor<TextFileProcessor>{
	private static final Logger log=LoggerFactory.getLogger(TextSourceProcessor.class);
	protected File sourceFile;

	protected File target;
	protected Charset sourceCharset;
	protected Dealwith dealwith = Dealwith.NONE;
	
	private String[] extPatterns= ArrayUtils.EMPTY_STRING_ARRAY; 
	
	public int processFiles(File f) throws IOException {
		int n = 0;
		if (f.isDirectory()) {
			for (File sub : f.listFiles()) {
				n += processFiles(sub);
			}
		} else {
			if (extPatterns.length == 0 || ArrayUtils.contains(extPatterns, IOUtils.getExtName(f.getName()))) {
				processFile(f);
				n++;
			}
		}
		return n;
	}
	
	 TextFileCallback call;
		private Throwable lastException;
		
		private Charset targetCharset;
	
	/**
	 * 用指定的回调方法处理文本文件
	 * 
	 * @param f             文件
	 * @param sourceCharset 文件编码
	 * @param call          处理器
	 * @throws IOException
	 */
	public File processFile(File f) throws IOException {
		if (!call.accept(f)) {
			return null;
		}
		BufferedReader reader = IOUtils.getReader(f, sourceCharset);
		call.sourceFile = f;
		Charset charSet =targetCharset;
		BufferedWriter w = null;
		if (target != null) {
			w = IOUtils.getWriter(target, charSet == null ? sourceCharset : charSet, false);
		}
		String line;
		call.beforeProcess(f, target, w);
		while ((line = reader.readLine()) != null) {
			String txt = null;
			try {
				txt = call.processLine(line);
			} catch (Throwable e) {
				log.error("IO error", e);
				lastException = e;
			}
			if (w != null) {
				if (txt != null) {
					w.write(txt);
					int newLines = call.wrapLine();
					for (int li = 0; li < newLines; li++) {
						w.write("\r\n");
					}
				}
			}
			if (call.breakProcess())
				break;
		}
		reader.close();
		call.afterProcess(f, target, w);
		if (w != null)
			w.close();

		if (call.isSuccess() && target != null) {
			Dealwith deal = this.dealwith;
			if (deal == Dealwith.REPLACE) {
				if (f.delete()) {
					File n = new File(f.getPath());
					target.renameTo(n);
					return n;
				}
			} else if (deal == Dealwith.DELETE) {
				f.delete();
			} else if (deal == Dealwith.BACKUP_REPLACE) {
				File backupfile = new File(f.getParentFile(), f.getName() + ".bak");
				backupfile = IOUtils.escapeExistFile(backupfile);
				if (f.renameTo(backupfile)) {
					File n = new File(f.getPath());
					target.renameTo(n);
					return n;
				}
			}
			return target;
		} else {
			if (target != null) {
				target.delete();
			}
			return null;
		}
	}
}
