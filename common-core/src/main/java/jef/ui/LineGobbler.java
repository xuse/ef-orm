package jef.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jef.common.log.LogUtil;

public class LineGobbler extends StreamGobbler{
	private String charset;
	
	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	protected void doTask() {
		try {
			BufferedReader isr = new BufferedReader(charset==null?new InputStreamReader(is):new InputStreamReader(is,charset));
			String line;
			while ((line=isr.readLine()) != null) {
				process(line);
			}
		} catch (IOException ioe) {
			LogUtil.exception(ioe);
		}
	}

	protected void process(String n) {
		LogUtil.show(n);
	}
}
