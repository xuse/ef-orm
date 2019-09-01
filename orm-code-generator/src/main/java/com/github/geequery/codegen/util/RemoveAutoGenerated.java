package com.github.geequery.codegen.util;

import java.io.File;

import com.github.geequery.core.util.TextFileCallback;

public class RemoveAutoGenerated extends TextFileCallback {
	private int resolution = 0;

	@Override
	public String processLine(String line) {
		if (line.trim().equals("@NotModified")) {
			resolution = 1;
		} else if (line.trim().startsWith("public class ")) {
			resolution = -1;
		}
		return null;
	}

	protected boolean breakProcess() {
		return resolution != 0;
	}

//	@Override
//	public File getTarget(File source) {
//		resolution = 0;
//		return null;
//	}
//
//	@Override
//	protected Dealwith dealwithSourceOnSuccess(File source) {
//		if (resolution == 1) {
//			System.out.println("Delete: " + source.getName());
//			return Dealwith.REPLACE;
//		}
//		return Dealwith.NONE;
//	}
}
