package com.github.geequery.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.geequery.core.util.Dealwith.OutputableImpl;

import jef.tools.IOUtils;

public class FileProcessors {
	
	private static final Logger log = LoggerFactory.getLogger(FileProcessors.class);
	
	public static File processFile(File f,Dealwith dealWith ,TextFileCallback call) throws IOException {
		if (!call.accept(f)) {
			return null;
		}
		Charset sourceCharset = call.sourceCharset(f);
		BufferedReader reader = IOUtils.getReader(f, sourceCharset);
		call.sourceFile = f;
		BufferedWriter w = null;
		
		File target = null;
		Charset charSet = null;
		if(dealWith instanceof OutputableImpl) {
			target=((OutputableImpl) dealWith).getTarget(f);
			charSet =((OutputableImpl) dealWith).targetCharset(sourceCharset);
		}
		
		if (target != null) {
			w =  IOUtils.getWriter(target, charSet == null ? sourceCharset : charSet, false);
		}
		String line;
		call.beforeProcess(f, target, w);
		while ((line = reader.readLine()) != null) {
			String txt = null;
			try {
				txt = call.processLine(line);
			} catch (Throwable e) {
				log.error("IO error", e);
				call.lastException = e;
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
			return dealWith.onSuccess(f, target);
		} else {
			if (target != null) {
				target.delete();
			}
			return null;
		}
	}
}
