package jef.tools.io;

import java.io.File;
import java.io.IOException;

import jef.tools.IOUtils;
import jef.tools.StringUtils;


/**
 * JDK提供了native2ascii.exe ascii2native.exe， 不过要命令行，不能在eclipse里面直接操作，因此编写了一个可
 * 运行的类，在eclipse里直接运行。
 * 
 * @author Administrator
 */
public class ResourceAscii2Native {
	public static void main(String... args) throws IOException {
		if (args.length == 0) {
			System.out.println("please input the file/folder path you want to convert as a running arg.");
			return;
		}
		File f = new File(StringUtils.join(args, " "));
		if (!f.exists()) {
			System.out.println("target file " + f.getAbsolutePath() + " not found.");
			return;
		}
		File t = new File(f.getAbsolutePath() + ".nav");
		IOUtils.fromHexUnicodeString(f, t, "UTF-8");
		File bak = IOUtils.escapeExistFile(new File(f.getAbsolutePath() + ".bak"));
		if (f.renameTo(bak) && t.renameTo(f)) {
			System.out.println("convert successful! the original file was backup as :" + bak.getAbsolutePath());
		} else {
			System.out.println("convert successful!");
		}
	}
}