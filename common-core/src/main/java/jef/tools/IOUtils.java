/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jef.codegen.support.OverWrittenMode;
import jef.common.BigDataBuffer;
import jef.common.JefSerializable;
import jef.common.SimpleException;
import jef.common.log.LogUtil;
import jef.jre5support.ProcessUtil;
import jef.tools.TextFileCallback.Dealwith;
import jef.tools.io.UnicodeReader;

public class IOUtils {
	private static final int DEFAULT_BUFFER_SIZE = 4096;
	private static final File[] EMPTY = new File[0];

	/**
	 * 关闭指定的对象，不会抛出异常
	 * 
	 * @param input
	 *            需要关闭的资源
	 * @deprecated 请使用{@link #closeQuietly}，该方法和Apache commons-io中的工具类同名，更适合代码复用
	 */
	public static void close(Closeable input) {
		closeQuietly(input);
	}

	/**
	 * 关闭指定的对象，不会抛出异常
	 * 
	 * @param input
	 *            需要关闭的资源
	 */
	public static void closeQuietly(Closeable input) {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	/**
	 * 在临时文件目录下创建一个目录并清空，如果不能正常操作就抛出权限不足的异常(在linux上经常出现)。<br>
	 * 这个临时目录会使用pid作为名称，因此一个程序如果PID不变，每次都能得到相同的目录。<br>
	 * 
	 * 一般来说这个目录用于存放和本次运行相关的信息，如果上次运行后这个目录中存在数据，那么本次将清空这个目录。
	 * 
	 * @param 临时文件的文件名
	 * @return 创建的临时目录
	 */
	public synchronized static File createTempDirectory(String name) {
		String tempPath = System.getProperty("java.io.tmpdir");
		File f = new File(tempPath, name + "." + ProcessUtil.getPid());
		try {
			if (f.isDirectory()) {
				deleteAllChildren(f);
			} else if (f.isFile()) {
				f.delete();
			}
			f.mkdirs();
			return f;
		} catch (Exception e) {
			String user = System.getProperty("user.name");
			throw new SimpleException("Current user[" + user + "] doesn't have any permission to access folder " + e.getMessage());
		}
	}

	/**
	 * 获得配置文件的项目。配置文件用= :等分隔对，语法同properties文件
	 * <p>
	 * 使用此方法可以代替使用JDK中的{@link java.util.Properties}工具。因为Properties操作中往往有以下不便
	 * <ol>
	 * <li>在遍历时，由于Properties继承了Map&lt;Object,Object&gt;泛型，不得不编写强制类型转换的代码。</li>
	 * <li>Properties继承了Hashtable性能低下，此外如果getProperty(null)还会抛出异常。</li>
	 * <li>Properties中的数据是乱序的，无法保持原先在文件中出现的顺序</li>
	 * <li>Properties保留了基于InputStream的接口，使用时容易出现编码错误</li>
	 * </ol>
	 * 因此，建议在加载.properties文件时，不要使用JDK中的{@link java.util.Properties}。
	 * <s>彻底淘汰落后的java.util.Properties</s>
	 * 
	 * @param in
	 *            要读取的资源
	 * @return 文件中的键值对信息。
	 */
	public static Map<String, String> loadProperties(URL in) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		loadProperties(getReader(in, null), result);
		return result;
	}

	/**
	 * 获得配置文件的项目。配置文件用= :等分隔对，语法同properties文件
	 * <p>
	 * 使用此方法可以代替使用JDK中的{@link java.util.Properties}工具。因为Properties操作中往往有以下不便
	 * <ol>
	 * <li>在遍历时，由于Properties继承了Map&lt;Object,Object&gt;泛型，不得不编写强制类型转换的代码。</li>
	 * <li>Properties继承了Hashtable性能低下，此外如果getProperty(null)还会抛出异常。</li>
	 * <li>Properties中的数据是乱序的，无法保持原先在文件中出现的顺序</li>
	 * <li>Properties保留了基于InputStream的接口，使用时容易出现编码错误</li>
	 * </ol>
	 * 因此，建议在加载.properties文件时，不要使用JDK中的{@link java.util.Properties}。
	 * <s>彻底淘汰落后的java.util.Properties</s>
	 * 
	 * @param file
	 *            要读取的数据流。注意读取完成后流会被关闭。
	 * @return 文件中的键值对信息。
	 */
	public static Map<String, String> loadProperties(Reader in) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		loadProperties(in, result);
		return result;
	}

	/**
	 * 将Map内的数据保存成properties文件
	 * 
	 * @param writer
	 *            要输出的流
	 * @param map
	 *            要保存的键值对信息
	 * @param closeWriter
	 *            true表示保存完后关闭输出流，false则保持不变
	 */
	public static void storeProperties(Writer writer, Map<String, String> map, boolean closeWriter) {
		try {
			
			for (Map.Entry<String, String> entry : map.entrySet()) {
				writer.write(saveConvert(entry.getKey(), true));
				writer.write('=');
				writer.write(saveConvert(entry.getValue(), false));
				writer.write(StringUtils.CRLF_STR);
			}
			writer.flush();
		} catch (IOException e1) {
			LogUtil.exception(e1);
		} finally {
			if (closeWriter)
				closeQuietly(writer);
		}
	}

	/**
	 * 清空指定目录<br>
	 * 删除文件夹下的所有内容，文件夹本身不删除。 如果输入一个file，那么总是返回true
	 * 
	 * @param f
	 *            要清空的目录
	 * @return true表示操作成功，如果某些文件不能正常删除返回false.
	 */
	public static boolean deleteAllChildren(File f) {
		Assert.notNull(f);
		if (!f.exists())
			return true;
		if (f.isDirectory()) {
			for (File sub : listFolders(f)) {
				if (!deleteTree(sub, true))
					return false;
			}
			for (File sub : listFiles(f)) {
				if (!sub.delete())
					return false;
			}
		}
		return true;
	}

	/**
	 * 删除整个文件夹树
	 * 
	 * @param f
	 *            要删除的文件或文件夹
	 * @param includeSub
	 *            如果为false,那么如果目录非空，将不删除。返回false
	 * @return 成功删除返回true,没成功删除返回false。 如果文件夹一开始就不存在，也返回true。
	 */
	public static boolean deleteTree(File f, boolean includeSub) {
		Assert.notNull(f);
		if (!f.exists())
			return true;
		if (includeSub && f.isDirectory()) {
			for (File sub : listFolders(f)) {
				if (!deleteTree(sub, true))
					return false;
			}
			for (File sub : listFiles(f)) {
				if (!sub.delete())
					return false;
			}
		}
		return f.delete();
	}

	/**
	 * 递归列出(所有层级的)目录
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param folderFilter
	 *            自定义过滤器，可过滤掉不需要的文件夹
	 * @return 所有未被过滤的文件夹
	 */
	public static File[] listFoldersRecursive(File root, final FileFilter folderFilter) {
		List<File> files = new ArrayList<File>();
		for (File folder : listFolders(root)) {
			if (folderFilter == null || folderFilter.accept(folder)) {
				files.add(folder);
				files.addAll(Arrays.asList(listFoldersRecursive(folder, folderFilter)));
			}
		}
		return files.toArray(new File[files.size()]);
	}

	/**
	 * 递归列出目录下(所有层级的)文件。可以指定哪些扩展名，如果不指定扩展名则所有文件都被列出， <br>
	 * 不会列出目录
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param extnames
	 *            允许列出的扩展名，必须小写，不含.号。可以指定多种扩展名
	 * @return 所有指定扩展名的文件。
	 */
	public static File[] listFilesRecursive(File root, final String... extnames) {
		List<File> files = new ArrayList<File>();
		for (File folder : listFolders(root)) {
			files.addAll(Arrays.asList(listFilesRecursive(folder, extnames)));
		}
		files.addAll(Arrays.asList(listFiles(root, extnames)));
		return files.toArray(new File[files.size()]);
	}

	/**
	 * 递归列出目录下(所有层级的)文件。可以指定过滤器，过滤掉不需要的文件 <br>
	 * 不会列出目录
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param filter
	 *            指定的过滤器
	 * @param folderFilter
	 *            文件夹过滤器，可以用此过滤器来防止搜索不需要的目录
	 * @return 所有未被过滤的文件
	 */
	public static File[] listFilesRecursive(File root, final FileFilter fileFilter, final FileFilter folderFilter) {
		List<File> files = new ArrayList<File>();
		if (root.exists()) {
			for (File f : root.listFiles()) {
				if (f.isDirectory()) {
					if (folderFilter != null && !folderFilter.accept(f)) {
						continue;
					}
					files.addAll(Arrays.asList(listFilesRecursive(f, fileFilter, folderFilter)));
				} else {
					if (fileFilter != null && !fileFilter.accept(f)) {
						continue;
					}
					files.add(f);
				}
			}
		}
		return files.toArray(new File[files.size()]);
	}

	/**
	 * 递归列出目录下(所有层级的)文件。可以指定文件名的模板进行匹配。
	 * <p>
	 * 匹配字符串。该字符串中可以用*表示任意字符，用?表示单个字符。 这个函数的功能类似于windows的文件搜索。
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param pattern
	 *            指定的匹配字符串。该字符串中可以用*表示任意字符，用?表示单个字符。
	 * @return 所有符合条件的文件
	 */
	public static File[] listFilesRecursiveLike(File root, final String pattern) {
		return listFilesRecursive(root, new FileFilter() {
			public boolean accept(File f) {
				if (StringUtils.matches(f.getName(), pattern, true)) {
					return true;
				}
				return false;
			}
		}, null);
	}

	/**
	 * 递归列出目录下文件。可以指定扩展名。
	 * 
	 * @param file
	 *            要搜索的目录
	 * @param extnames
	 *            需要的文件类型（扩展名）。要求小写，无需带'.'符号。
	 * @return 该目录下符合指定类型的所有文件(只搜索一层，不会递归搜索)。<strong>仅列出文件，不会返回目录</strong>
	 */
	public static File[] listFiles(File file, final String... extnames) {
		File[] r = file.listFiles(new FileFilter() {
			public boolean accept(File f) {
				boolean isAll = extnames.length == 0;
				if (f.isFile() && (isAll || ArrayUtils.contains(extnames, getExtName(f.getName())))) {
					return true;
				}
				return false;
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 列出指定目录下的文件。可以指定文件名的模板进行匹配。
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param pattern
	 *            搜索字符串，可以用*,?,+表示匹配任意字符。
	 * @return 该目录下，文件名符合指定搜索字符串的文件(只搜索一层，不会递归搜索)。仅列出文件，不会返回目录
	 */
	public static File[] listFilesLike(File root, final String pattern) {
		File[] r = root.listFiles(new FileFilter() {
			public boolean accept(File f) {
				if (f.isFile() && StringUtils.matches(f.getName(), pattern, true)) {
					return true;
				}
				return false;
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 列出指定目录下的文件夹
	 * 
	 * @param root
	 *            指定目录
	 * @return 该目录下的所有文件夹
	 */
	public static File[] listFolders(File root) {
		File[] r = root.listFiles(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				return false;
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 列出指定目录下文件夹，匹配指定的字符串
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param pattern
	 *            搜索字符串，可以用*,?,+表示匹配任意字符。
	 * @return 该目录下符合搜索字符串的所有文件夹。(只搜索一层，不会递归搜索)。仅列出文件夹，不会返回文件
	 */
	public static File[] listFoldersLike(File root, final String pattern) {
		File[] r = root.listFiles(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory() && StringUtils.matches(f.getName(), pattern, true)) {
					return true;
				}
				return false;
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 列出指定目录下的文件和文件夹，其中文件只列出符合扩展名的文件。
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param extnames
	 *            允许列出的扩展名，必须小写。不含.号
	 * @return 该目录下的所有文件夹（不管名称中有没有.xxx）以及符合类型的文件。
	 */
	public static File[] listFilesAndFolders(File root, final String... extnames) {
		File[] r = root.listFiles(new FileFilter() {
			public boolean accept(File f) {
				boolean isAll = extnames.length == 0;
				if (f.isDirectory()) {
					return true;
				}
				if (isAll || ArrayUtils.contains(extnames, getExtName(f.getName()))) {
					return true;
				}
				return false;
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 列出指定目录下的文件和文件夹，可以指定搜索字符串
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param pattern
	 *            搜索字符串
	 * @return 该目录下所有符合搜索串的 文件和目录。(只搜索一层，不会递归搜索)
	 */
	public static File[] listFilesAndFoldersLike(File root, final String pattern) {
		File[] r = root.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return StringUtils.matches(f.getName(), pattern, true);
			}
		});
		return r == null ? EMPTY : r;
	}

	/**
	 * 给定一个File,确认其不存在于在磁盘上，如果存在就改名以回避 <br>
	 * 这个方法用于向磁盘输出文件时使用。<br>
	 * 比如输出名为 report.txt时，如果发现上一次的report.txt还在那么就会返回 "report(1).txt"。
	 * 如果"report(1).txt"也存在就会返回"report(2).txt"。 以此类推。
	 * 
	 * @param file
	 *            目标文件
	 * @return 如果目标文件不存在，返回本身。如果目标文件已存在，就返回一个带后缀而磁盘上不存在的文件。
	 */
	public static File escapeExistFile(File file) {
		if (!file.exists())
			return file;
		int pos = file.getName().lastIndexOf(".");
		String path = file.getParent();
		if (StringUtils.isEmpty(path)) {
			throw new IllegalArgumentException(file.getAbsolutePath() + " has no valid parent folder.");
		}
		String baseFilename = null;
		String extName = null;
		if (pos > -1) {
			baseFilename = file.getName().substring(0, pos);
			extName = file.getName().substring(pos + 1);
		} else {
			baseFilename = file.getName();
		}
		int n = 1;
		while (file.exists()) {
			file = new File(path + "/" + baseFilename + "(" + n + ")" + ((extName == null) ? "" : "." + extName));
			n++;
		}
		return file;
	}

	/**
	 * 得到文件的扩展名（小写如果没有则返回空字符串）
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getExtName(String fileName) {
		int pos = fileName.lastIndexOf(".");
		if (pos > -1) {
			return fileName.substring(pos + 1).toLowerCase();
		} else {
			return "";
		}
	}

	/**
	 * 得到文件名除去扩展名的部分
	 * 
	 * @param fileName
	 * @return
	 */
	public static String removeExt(String fileName) {
		return StringUtils.substringBeforeLast(fileName, ".");
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param path
	 * @return 如果该文件存在，且不是目录，返回true。否则返回false
	 */
	public static boolean isFile(String path) {
		File f = new File(path);
		return f.exists() && f.isFile();
	}

	/**
	 * 判断目录是否存在
	 * 
	 * @param path
	 * @return 如果该文件存在，且是目录，返回true。否则返回false
	 */
	public static boolean isFolder(String path) {
		File f = new File(path);
		return f.exists() && f.isDirectory();
	}

	/**
	 * JDK6之前的很多代码要将中文写作\\uHHHH格式，这里可以将此种格式转换回正常格式文件
	 * 
	 * @param source
	 * @param target
	 * @param charset
	 * @throws IOException
	 */
	public static void fromHexUnicodeString(File source, File target, String charset) throws IOException {
		Reader r = getReader(source, null);
		Writer w = getWriter(target, charset, false);
		StringUtils.fromHexUnicodeString(r, w);
		r.close();
		w.close();
	}

	/**
	 * JDK6之前的很多代码要将中文写作\\uHHHH格式，这里可以将正常中文文件转换到此种格式
	 * 
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void toHexUnicodeString(File source, File target, String sourceCharset) throws IOException {
		Reader r = getReader(source, sourceCharset);
		Writer w = getWriter(target, null, false);
		StringUtils.toHexUnicodeString(r, w, "\\u");
		r.close();
		w.close();
	}

	/**
	 * 检查/创建 文件夹
	 * 
	 * @param path
	 */
	public static void createFolder(String path) {
		File file = new File(path);
		if (file.exists() && file.isFile()) {
			throw new RuntimeException("Duplicate name file exist. can't create directory " + path);
		} else if (!file.exists()) {
			file.mkdirs();
		}
	}

	/**
	 * 检查/创建文件在所的文件夹
	 * 
	 * @param file
	 */
	public static void ensureParentFolder(File file) {
		File f = file.getParentFile();
		if (f != null && !f.exists()) {
			f.mkdirs();
		} else if (f != null && f.isFile()) {
			throw new RuntimeException(f.getAbsolutePath() + " is a exist file, can't create directory.");
		}
	}

	/**
	 * 在reader中读取字符，直到出现指定字符中的任一个，停止读取。 返回的字符串中包含了最后出现的字符
	 * 
	 * @param reader
	 * @param appear
	 * @return
	 * @throws IOException
	 */
	public static String readUntill(Reader reader, char... appear) throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int i = reader.read();
			if (i < 0)
				return sb.length() == 0 ? null : sb.toString();
			sb.append((char) i);
			for (int ind = 0; ind < appear.length; ind++) {
				if (appear[ind] == i) {
					return sb.toString();
				}
			}
		}
	}

	/**
	 * 在reader中读取字符，直到出现指定字符中的任一个，停止读取。 返回的字符串中不包含最后出现的字符
	 * 
	 * @param reader
	 * @param appear
	 * @return
	 * @throws IOException
	 */
	public static String readTill(Reader reader, char... appear) throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int i = reader.read();
			if (i < 0)
				return sb.length() == 0 ? null : sb.toString();
			for (int ind = 0; ind < appear.length; ind++) {
				if (appear[ind] == i) {
					return sb.toString();
				}
			}
			sb.append((char) i);
		}
	}

	public static String[] readLine(URL in, String charset, int... num) throws IOException {
		BufferedReader is = getReader(in, charset);
		try {
			String line = null;
			if (num.length == 0)
				num = new int[] { -1 };
			boolean isAll = num[0] == -1;
			List<String> result = new ArrayList<String>(isAll ? 20 : num.length);
			int n = 0;
			while ((line = is.readLine()) != null) {
				n++;
				if (isAll || ArrayUtils.contains(num, n)) {
					result.add(line);
				}
				if (!isAll && n >= num[num.length - 1])
					break;
			}
			return result.toArray(new String[result.size()]);
		} finally {
			closeQuietly(is);
		}
	}

	/**
	 * 得到文本文件的某几行，使用后文件会关闭 。
	 * 
	 * @param inName
	 *            要读的文本文件
	 * @param num
	 *            指定的行号,可以指定多行，必须按顺序(如果不指定则表示读取全部行；如果指定的行号小于1，会返回第一行)
	 * @return
	 * @throws IOException
	 */
	public static String[] readLine(File inName, String charset, int... num) throws IOException {
		BufferedReader is = getReader(inName, charset);
		try {
			String line = null;
			if (num.length == 0)
				num = new int[] { -1 };
			boolean isAll = num[0] == -1;
			List<String> result = new ArrayList<String>(isAll ? 20 : num.length);
			int n = 0;
			while ((line = is.readLine()) != null) {
				n++;
				if (isAll || ArrayUtils.contains(num, n)) {
					result.add(line);
				}
				if (!isAll && n >= num[num.length - 1])
					break;
			}
			return result.toArray(new String[result.size()]);
		} finally {
			closeQuietly(is);
		}
	}

	/**
	 * 将文本文件的指定行拼成String返回
	 * 
	 * @param inName
	 * @param charset
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	public static String readLinesAsString(File inName, String charset, LineFilter filter) throws IOException {
		BufferedReader is = getReader(inName, charset);
		String line = null;
		StringBuilder sb = new StringBuilder();
		int n = 0;
		while ((line = is.readLine()) != null) {
			if (filter == null) {
				if (sb.length() > 0)
					sb.append('\n');
				sb.append(line);
			} else {
				String ll = filter.filter(line, n++);
				if (ll != null) {
					if (sb.length() > 0)
						sb.append('\n');
					sb.append(line);
				}
			}
		}
		is.close();
		return sb.toString();
	}

	/**
	 * 行过滤器接口
	 * 
	 * @author Administrator
	 * 
	 */
	public interface LineFilter {
		/**
		 * 过滤行
		 * 
		 * @param line
		 *            传入：行的内容
		 * @param num
		 *            传入：行号
		 * @return 过滤后的行的内容，如果传出null表示不需要这一行
		 */
		String filter(String line, int num);
	}

	/**
	 * 从文件中读取需要的行
	 * 
	 * @param inName
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	public static String[] readLines(File inName, String charset, LineFilter filter) throws IOException {
		BufferedReader is = getReader(inName, charset);
		String line = null;
		List<String> result = new ArrayList<String>();
		int n = 0;
		while ((line = is.readLine()) != null) {
			if (filter != null) {
				String ll = filter.filter(line, n++);
				if (ll != null) {
					result.add(line);
				}
			} else {
				result.add(line);
			}
		}
		is.close();
		return result.toArray(new String[result.size()]);
	}

	/**
	 * 返回文件路径。<BR>
	 * getAbsolutePath 并不是唯一的，比如同一个文件， getAbsolutePath()可以返回 C:/TEMP/../book.exe
	 * ,也可以是 C:/book.exe。 而getCanonicalPath()才可以返回真正的文件路径。 <BR>
	 * 问题是这个方法需要抛出一个受检异常，很多时候影响 代码风格的简洁美观。
	 * 
	 * @param file
	 */
	public static String getPath(File file) {
		Assert.notNull(file);
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	/*
	 * Copies the contents of the given {@link InputStream} to the given {@link
	 * OutputStream}.
	 * 
	 * @param pIn The input stream, which is being read. It is guaranteed, that
	 * {@link InputStream#close()} is called on the stream.
	 * 关于InputStram在何时关闭的问题，我一直认为应当是成对操作的（即在哪个方法中生成Stream，就要在使用完后关闭），
	 * 因此不打算在这里使用close方法。 但是后来我又考虑到，InputStream在使用完后，其内部标记已经发生了变化，无法再次使用。
	 * (reset方法的效果和实现有关，并不能保证回复到Stream使用前的状态。)
	 * 因此考虑这里统一关闭以防止疏漏，外面再关一次也不会有问题(作为好习惯，还是应该成对打开和关闭)。
	 * 
	 * @param pOut 输出流，可以为null,此时输入流中的相应数据将丢弃
	 * 
	 * @param pClose True guarantees, that {@link OutputStream#close()} is
	 * called on the stream. False indicates, that only {@link
	 * OutputStream#flush()} should be called finally.
	 * 
	 * @param pBuffer Temporary buffer, which is to be used for copying data.
	 * 
	 * @return Number of bytes, which have been copied.
	 * 
	 * @throws IOException An I/O error occurred.
	 */
	private static long copy(InputStream in, OutputStream out, boolean inClose, boolean outClose, byte[] pBuffer) throws IOException {
		if (in == null)
			throw new NullPointerException();
		long total = 0;
		try {
			int res;
			while ((res = in.read(pBuffer)) != -1) {
				if (out != null) {
					out.write(pBuffer, 0, res);
				}
				total += res;
			}
			if (out != null)
				out.flush();
		} finally {
			if (outClose)
				closeQuietly(out);
			if (inClose)
				closeQuietly(in);
		}
		return total;
	}

	/*
	 * 同上、READER和Writer之间的拷贝
	 */
	private static long copy(Reader in, Writer out, boolean inClose, boolean outClose, char[] pBuffer) throws IOException {
		if (in == null)
			throw new NullPointerException();
		long total = 0;
		try {
			int res;
			while ((res = in.read(pBuffer)) != -1) {
				if (out != null) {
					out.write(pBuffer, 0, res);
				}
				total += res;
			}
			if (out != null)
				out.flush();
		} finally {
			if (outClose && out != null)
				closeQuietly(out);
			if (inClose)
				closeQuietly(in);
		}
		return total;
	}
	
	

	/**
	 * 流之间拷贝
	 * 
	 * @param in
	 *            输入
	 * @param out
	 *            输出
	 * @param inClose
	 *            关闭输入流？
	 * @param outClose
	 *            关闭输出流?
	 * @return
	 * @throws IOException
	 */
	public static long copy(InputStream in, OutputStream out, boolean inClose, boolean outClose) throws IOException {
		return copy(in, out, inClose, outClose, new byte[DEFAULT_BUFFER_SIZE]);
	}
	
	/**
	 * 流之间拷贝
	 * @param in 输入
	 * @param out 输出
	 * @param inClose 关闭输入流
	 * @param outClose 关闭输出流
	 * @return
	 * @throws IOException
	 */
	public static long copy(Reader in, Writer out, boolean inClose, boolean outClose) throws IOException {
		return copy(in, out, inClose, outClose, new char[DEFAULT_BUFFER_SIZE]);
	}
	

	/**
	 * 流之间拷贝
	 * 
	 * @param in 输入
	 * @param out 输出
	 * @param pClose 
	 *            关闭输出流? 
	 * @return 拷贝长度
	 * @throws IOException
	 */
	public static long copy(Reader in, Writer out, boolean pClose) throws IOException {
		return copy(in, out, true, pClose, new char[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * 流之间拷贝
	 * 
	 * @param in 输入
	 * @param out 输出
	 * @param closeOutStream
	 *            关闭输出流? (输入流默认关闭)
	 * @return
	 * @throws IOException
	 */
	public static long copy(InputStream in, OutputStream out, boolean closeOutStream) throws IOException {
		return copy(in, out, true, closeOutStream, new byte[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * 将Reader内容读取到内存中的charArray
	 * 
	 * @param reader 输入
	 * @return
	 * @throws IOException
	 */
	public static char[] asCharArray(Reader reader) throws IOException {
		CharArrayWriter cw = new CharArrayWriter(256);
		char[] buf = new char[1024];
		int n;
		try {
			while ((n = reader.read(buf)) > -1) {
				cw.write(buf, 0, n);
			}
		} finally {
			reader.close();
		}
		return cw.toCharArray();
	}

	/**
	 * 将Reader的内容读取为一个字符串
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static String asString(Reader reader) throws IOException {
		return asString(reader, true);
	}

	/**
	 * 将Reader内容读取为字符串
	 * 
	 * @param reader
	 * @param close
	 *            关闭reader
	 * @return
	 * @throws IOException
	 */
	public static String asString(Reader reader, boolean close) throws IOException {
		if (reader == null)
			return null;
		StringBuilder sb = new StringBuilder(128);
		char[] buf = new char[1024];
		int n;
		try {
			while ((n = reader.read(buf)) > -1) {
				sb.append(buf, 0, n);
			}
		} finally {
			if (close)
				reader.close();
		}
		return sb.toString();
	}

	public static String asString(File pStream, String charset) throws IOException {
		return asString(getReader(pStream, charset));
	}

	/**
	 * 将指定位置的数据读出成为文本
	 * 
	 * @param url
	 *            资源位置
	 * @param charset
	 *            字符编码，可以传入null
	 * @return 读到的文本
	 * @throws IOException
	 *             IO操作异常
	 **/
	public static String asString(URL url, String charset) throws IOException {
		if (url == null)
			return null;
		return asString(url.openStream(), charset, true);
	}

	/**
	 * 将输入流转化为String .(使用缺省的字符集)
	 * 
	 * 最简单的获取系统资源转换为String的写法如下： <code>
	 *    IOUtils.asString(ClassLoader.getSystemResourceAsStream(filename))
	 * </code>
	 */
	public static String asString(InputStream pStream) throws IOException {
		return asString(pStream, null, true);
	}

	/**
	 * 将输入流转化为String
	 * 
	 * @param pStream
	 *            The input stream to read.
	 * @param pEncoding
	 *            The character encoding, typically "UTF-8".
	 * @param close
	 *            close the in stream?
	 */
	public static String asString(InputStream pStream, String pEncoding, boolean close) throws IOException {
		if (pStream == null)
			return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
		copy(pStream, baos, close);
		if (pEncoding == null) {
			return baos.toString();
		} else {
			return baos.toString(pEncoding);
		}
	}

	/**
	 * 将制定的URL中的数据读出成byte[]
	 * 
	 * @param url
	 *            资源目标位置
	 * @return 字节数组
	 * @throws IOException
	 *             IO操作异常
	 */
	public static byte[] toByteArray(URL url) throws IOException {
		return toByteArray(url.openStream());
	}

	/**
	 * 读取文件到内存(不可用于大文件)
	 * 
	 * @param file
	 *            本地文件
	 * @return 字节数组
	 * @throws IOException
	 *             IO操作异常
	 */
	public static byte[] toByteArray(File file) throws IOException {
		InputStream in = (file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file);
		try {
			byte[] result = toByteArray(in, (int) file.length());
			return result;
		} finally {
			in.close();
		}
	}

	/**
	 * 读取文件到内存(不可用于大文件)
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @deprecated use {@linkp #toByteArray(File)}
	 */
	public static byte[] asByteArray(File file) throws IOException {
		return toByteArray(file);
	}

	/**
	 * 读取流数据到内存。注意这个方法会将数据流全部读入到内存中，因此不适用于很大的数据对象
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(InputStream in) throws IOException {
		try {
			byte[] msg = toByteArray(in, -1);
			return msg;
		} finally {
			in.close();
		}
	}

	/**
	 * @deprecated use {@link #toByteArray(InputStream)}
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] asByteArray(InputStream in) throws IOException {
		return toByteArray(in);
	}

	/**
	 * 将内存数据转换为流
	 * 
	 * @param bytes
	 * @return
	 */
	public static InputStream asInputStream(byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * 从流中读取指定的字节，第二个版本，性能比第一版本有明显提升，但比起第三次修改明显不如，仅作参考保留
	 * 
	 * @param in
	 * @param length
	 * 
	 * @return
	 * @throws IOException
	 * @deprecated
	 */
	public static byte[] toByteArray_v2(InputStream in, int length) throws IOException {
		ByteArrayOutputStream out;
		if (length > 0) {
			out = new ByteArrayOutputStream(length);
		} else {
			out = new ByteArrayOutputStream(1024);
		}
		byte[] pBuffer = new byte[DEFAULT_BUFFER_SIZE];
		int left = (length > 0) ? length : Integer.MAX_VALUE;// 剩余字节数
		while (left > 0) {
			int n;
			if (left < DEFAULT_BUFFER_SIZE) {
				n = in.read(pBuffer, 0, left);
			} else {
				n = in.read(pBuffer);
			}
			if (n == -1)
				break;
			left -= n;
			out.write(pBuffer, 0, n);
		}
		out.close();
		byte[] message = out.toByteArray();
		return message;
	}

	/**
	 * asByteArray的旧版本，重写后实际测试发现性能要低5%～10%。 仅为参考保留
	 * 
	 * @deprecated
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray_old(InputStream in, int length) throws IOException {
		ByteArrayOutputStream out;
		if (length > 0) {
			out = new ByteArrayOutputStream(length);
		} else {
			out = new ByteArrayOutputStream(1024);
		}
		byte[] pBuffer = new byte[DEFAULT_BUFFER_SIZE];
		int count = 0;
		while (count < length || length < 0) {
			if (length < 0 || length - count > DEFAULT_BUFFER_SIZE) {
				int n = in.read(pBuffer);
				if (n == -1)
					break;
				count += n;
				out.write(pBuffer, 0, n);
			} else {
				int n = in.read(pBuffer, 0, length - count);
				if (n == -1)
					break;
				count += n;
				out.write(pBuffer, 0, n);
			}
		}
		out.close();
		byte[] message = out.toByteArray();
		return message;
	}

	/**
	 * 这个方法是JDK InputStream.read(byte b[], int off, int len) 的代替方法
	 * 对于网络数据流，可以反复重试，直到读取到足够的数据
	 * 
	 * @return
	 */
	public static int readBytes(InputStream in, byte[] data, int offset, int length) throws IOException {
		if (length < 0)
			throw new IOException("This method just for reading bytes of a expected length from stream.The param  length must >=0");
		if (length == 0)
			return 0;
		if (offset + length > data.length) {
			throw new IOException("the container byte[] does not enough for the expected length.");
		}
		int left = length;
		int off = offset;
		while (left > 0) {
			int n;
			n = in.read(data, off, left);
			if (n == -1)
				break;
			left -= n;
			off += n;
		}
		return off - offset;
	}

	/**
	 * 合并字节数组
	 * 
	 * @param packages
	 * @return
	 */
	public static byte[] mergeBytes(List<byte[]> packages) {
		int len = 0;
		for (byte[] aPackage : packages) {
			len += aPackage.length;
		}
		byte[] result = new byte[len];
		int pos = 0;
		for (byte[] aPackage : packages) {
			for (int j = 0; j < aPackage.length; j++) {
				result[pos++] = aPackage[j];
			}
		}
		return result;
	}

	/**
	 * 从流中读取指定的字节，第三个版本，性能再度提升 参考数据，从120M文件中读取前60M，此方法耗时125ms,v2耗时156ms
	 * 
	 * @param in
	 * @param length
	 *            要读取的字节数，-1表示不限制。（注意实际处理中-1的情况下最多读取2G数据，超过2G不会读取）
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(InputStream in, int length) throws IOException {
		ByteArrayOutputStream out;
		if (length > 0) {
			out = new ByteArrayOutputStream(length);
		} else {
			out = new ByteArrayOutputStream(1024);
		}
		int buf = DEFAULT_BUFFER_SIZE;
		byte[] pBuffer = new byte[buf];
		int left = (length > 0) ? length : Integer.MAX_VALUE;// 剩余字节数
		while (left >= buf) {
			int n = in.read(pBuffer);
			if (n == -1) {
				left = 0;
				break;
			}
			left -= n;
			out.write(pBuffer, 0, n);
		}
		while (left > 0) {
			int n = in.read(pBuffer, 0, left);
			if (n == -1) {
				break;
			}
			left -= n;
			out.write(pBuffer, 0, n);
		}
		out.close();// ByteArrayOut其实是不需要close的，这里close是为了防止一些代码检查工具提出警告
		byte[] message = out.toByteArray();
		return message;
	}

	/**
	 * @deprecated use {@link #toByteArray(InputStream, int)}
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public static byte[] asByteArray(InputStream in, int length) throws IOException {
		return toByteArray(in, length);
	}

	/**
	 * 将流中的数据读入到BigDataBuffer对象中去。
	 * 
	 * @param in
	 *            输入流
	 * @param limit
	 *            限制长度
	 * @return
	 * @throws IOException
	 */
	public static BigDataBuffer asBigDataBuffer(InputStream in, long limit) throws IOException {
		BigDataBuffer out = new BigDataBuffer();
		byte[] pBuffer = new byte[DEFAULT_BUFFER_SIZE];
		long size = 0;
		try {
			while (size < limit || limit < 0) {
				int n = processDataRead(pBuffer, in, out, limit, size);
				if (n == 0) {
					// 继续
				} else if (n == -1) {
					break; // 完成
				} else {
					size += n;// 继续
				}
			}
			return out;
		} finally {
			in.close();
		}
	}

	/*
	 * @param pBuffer byte[] 的Bufferr，外部初始化一次，免去每次在此方法中创建数组的开销
	 * 
	 * @param in 数据来源
	 * 
	 * @param out 数据去向（一般为Buffer或byte[]等）
	 * 
	 * @param limit 总共要读取的字节数
	 * 
	 * @param current 当前已读取字节数
	 * 
	 * @return 本次读取的字节数
	 * 
	 * @throws IOException
	 */
	private static int processDataRead(byte[] pBuffer, InputStream in, OutputStream out, long limit, long current) throws IOException {
		int n = 0;
		if (limit < 0 || limit - current > DEFAULT_BUFFER_SIZE) {
			n = in.read(pBuffer);
		} else {
			int left = (int) (limit - current);
			n = in.read(pBuffer, 0, left);
		}
		if (n < 0) {
			return -1; // 没有数据
		} else {// 正常读取,返回本次读取字节数
			out.write(pBuffer, 0, n);
			return n;
		}
	}

	/**
	 * 将指定的流保存为临时文件
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static File saveAsTempFile(InputStream is) throws IOException {
		File f = File.createTempFile("~tmp", ".io");
		saveAsFile(f, is);
		return f;
	}

	/**
	 * 将数据从来源保存为临时文件
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static File saveAsTempFile(Reader reader) throws IOException {
		File f = File.createTempFile("~tmp", ".io");
		saveAsFile(f, null, reader);
		return f;
	}

	/**
	 * 将输入流保存为文件
	 * 
	 * @param is
	 * @param file
	 * @throws IOException
	 */
	public static void saveAsFile(File file, InputStream... iss) throws IOException {
		ensureParentFolder(file);
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		try {
			for (InputStream is : iss) {
				copy(is, os, false);
			}
		} finally {
			if (os != null) {
				os.flush();
				os.close();
			}
		}
	}

	/**
	 * 将reader内容保存为文件
	 * 
	 * @param reader
	 * @param file
	 * @throws IOException
	 */
	public static void saveAsFile(File file, Charset charset, Reader... readers) throws IOException {
		BufferedWriter os = getWriter(file, charset == null ? null : charset.name(), false);
		try {
			for (Reader reader : readers) {
				copy(reader, os, true, false, new char[2048]);
			}
		} finally {
			closeQuietly(os);
		}
	}

	/**
	 * 将文字写入文件
	 * 
	 * @param text
	 * @param file
	 * @param append
	 * @throws IOException
	 */
	public static void saveAsFile(File file, Charset charset, String... texts) throws IOException {
		BufferedWriter os = getWriter(file, charset == null ? null : charset.name(), false);
		try {
			for (String text : texts) {
				os.write(text);
			}
		} finally {
			if (os != null) {
				os.flush();
				os.close();
			}
		}
	}

	public static void saveAsFile(File file, String... texts) throws IOException {
		saveAsFile(file, null, texts);
	}

	/**
	 * 将内存数据块写入文件
	 * 
	 * @param data
	 * @param file
	 * @throws IOException
	 */
	public static void saveAsFile(File file, boolean append, byte[] data) throws IOException {
		ensureParentFolder(file);
		OutputStream out = new FileOutputStream(file, append);
		try {
			out.write(data);
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}

	/**
	 * 将内存数据块写入文件
	 * 
	 * @param file
	 * @param data
	 * @throws IOException
	 */
	public static void saveAsFile(File file, byte[] data) throws IOException {
		saveAsFile(file, false, data);
	}

	/**
	 * 将文件分割成size大小的小块文件。 文件切割，使用Channel方式
	 * 
	 * @param file
	 * @param output
	 * @param size
	 * @return 文件分割的块数
	 */
	public static int cut(File file, String output, int size) {
		try {
			if (!file.exists())
				return -1;
			// 创建文件输入流
			FileInputStream fin = (file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file);
			FileChannel fc1 = fin.getChannel();
			int cnt = 0;// 存储每次读取的字节数
			int nth = 1;
			long position = 0;// 位置
			long len = file.length();
			while (position < len) {
				String name = output + "/" + file.getName() + "@" + nth;
				FileOutputStream fou = new FileOutputStream(name);
				FileChannel fc2 = fou.getChannel();
				cnt = (int) fc1.transferTo(position, size, fc2);
				fou.close();
				fc2.close();
				nth++;
				position += cnt;
			}
			fin.close();
			fc1.close();
			return nth - 1;
		} catch (IOException e) {
			LogUtil.exception(e);
			return -1;
		}
	}

	/**
	 * 文件合并 实现方式，使用nio操作。
	 * 
	 * @param list
	 * @param path
	 * @param srcFileName
	 * @return
	 */
	public static boolean combine(Collection<String> list, String path, String srcFileName) {
		File outputFile = new File(path + "/" + srcFileName);
		outputFile = escapeExistFile(outputFile);
		try {
			FileOutputStream fou = new FileOutputStream(outputFile);
			FileChannel fco = fou.getChannel();
			long position = 0;
			for (String i : list) {// 按顺序获得各个文件名
				File file = new File(i);// 创建文件
				if (!file.exists())
					return false;
				FileInputStream fin = new FileInputStream(file);
				FileChannel fci = fin.getChannel();
				long len = file.length();
				fco.transferFrom(fci, position, len);// 接收数据到指定的位置
				position += len;
				closeQuietly(fin);
				closeQuietly(fci);
			}
			closeQuietly(fou);
			closeQuietly(fco);
			return true;
		} catch (Exception ee) {
			LogUtil.exception(ee);
			return false;
		}
	}

	/**
	 * 拷贝（目录）拷贝<br>
	 * 支持目录拷贝，指定目录下的目录结构会被保留，并复制到新的路径上<br>
	 * 如果有同名文件或文件夹，会自动覆盖。
	 * 
	 * @param file
	 *            源文件或文件夹
	 * @param newFile
	 *            目标文件或文件夹
	 * @return true拷贝成功，false表示拷贝过程出现失败
	 */
	public static boolean copyFile(File file, File newFile) {
		return copyFile(file, newFile, CopyStrategy.ALLWAYS_OVERWRITE);
	}

	/**
	 * 拷贝（目录）拷贝<br>
	 * 可以指定拷贝策略<br>
	 * 支持目录拷贝，指定目录下的目录结构会被保留，并复制到新的路径上
	 * <p>
	 * 注意，此方法是在两个完整 文件/目录 路径之间进行拷贝。
	 * <p>
	 * 例如 copyFile(new File("c:\temp"),new File("d:\temproot"));
	 * //c:\temp和d:\temproot都是目录 拷贝后，c:\temp目录下的所有文件都被拷贝到d:'temproot目录下。
	 * <p>
	 * copyFile(new File("c:\temp\io.sys"),new File("d:\temproot"));
	 * 如果d:\temproot不存在，那么拷贝后d:\temproot是一个文件。
	 * 
	 * @param source
	 *            源文件或目录
	 * @param newFile
	 *            目标文件或目录。
	 * @param strategy
	 *            拷贝策略，拷贝策略可以用于指定拷贝中的各种行为
	 * @return true拷贝成功，false表示拷贝过程出现失败
	 * @see CopyStrategy
	 */
	public static boolean copyFile(File source, File newFile, CopyStrategy strategy) {
		if (!source.exists())
			return false;
		if (source.isDirectory()) {// 源为目录时
			if (!strategy.processFolder(source, newFile)) {
				return false;
			}
			if (newFile.exists()) {
				if (!newFile.isDirectory()) {
					throw new IllegalArgumentException("the target " + newFile.getPath() + " has exist, and is not folder.");
				}
			}
			newFile.mkdirs();
			for (File f : source.listFiles()) {
				File target = strategy.getTargetFile(f, newFile);
				if (target != null) {
					copyFile(f, target, strategy);
				}
			}
			if (strategy.isMove()) {
				if (source.list().length == 0) {
					source.delete();
				}
			}
			return true;
		} else {// 源为文件时
			if (newFile.isDirectory()) {
				if (strategy.allowFileIntoFolder()) {
					return copyFile(source, new File(newFile, source.getName()), strategy);
				} else {
					throw new IllegalArgumentException("the target " + newFile.getPath() + " has exist, and is a folder.");
				}
			}
			if (newFile.exists() && !strategy.canOverWritten(source, newFile)) {
				return false;
			}
			if (strategy.isMove()) {
				if (newFile.exists() && !newFile.delete()) {// 如果目标存在，并且无法删除，则移动失败
					return false;
				}
				return move(source, newFile);
			} else {
				FileChannel in = null;
				FileChannel out = null;
				boolean flag = false;
				try {
					in = ((source instanceof URLFile) ? ((URLFile) source).getInputStream() : new FileInputStream(source)).getChannel();
					out = new FileOutputStream(newFile).getChannel();
					in.transferTo(0, source.length(), out);
					flag = true;
				} catch (IOException e) {
					LogUtil.exception(e);
				} finally {
					closeQuietly(out);
					closeQuietly(in);
				}
				return flag;
			}
		}
	}

	/**
	 * 将文件拷贝到指定目录下，文件名保持不变<br>
	 * 
	 * @param tmpFile
	 *            需要复制的文件或目录
	 * @param path
	 *            文本，要复制的目标路径(必须是一个文件夹)，如果目标不存在会自动创建为文件夹。
	 * @return 拷贝后的文件
	 * @throws IOException
	 *             磁盘操作异常时抛出
	 * @deprecated Please use {@link #copyIntoFolder(File, File)};
	 */
	public static File copyToFolder(File file, String path) throws IOException {
		return copyIntoFolder(file, new File(path));
	}

	/**
	 * 将文件拷贝到指定目录下，文件名保持不变<br>
	 * 
	 * @param tmpFile
	 *            需要复制的文件或目录
	 * @param path
	 *            文本，要复制的目标路径(必须是一个文件夹)，如果目标不存在会自动创建为文件夹。
	 * @return 拷贝后的文件
	 * @throws IOException
	 *             磁盘操作异常时抛出
	 */
	public static File copyIntoFolder(File source, File dir) {
		File target = new File(dir, source.getName());
		copyFile(source, target);
		return target;
	}

	/**
	 * 将文件移动到指定目录下
	 * 
	 * @param file
	 *            文件
	 * @param folder
	 *            目标文件夹
	 * @param autoEscape
	 *            如果存在同名文件，则自动改名
	 * @return
	 */
	public static boolean moveToFolder(File file, File folder, boolean autoEscape) {
		if (folder.exists() && folder.isFile()) {
			throw new IllegalArgumentException("Target is a file.(" + folder.getAbsolutePath() + ")");
		}
		if (!folder.exists())
			folder.mkdirs();
		File target = new File(folder, file.getName());
		if (target.exists()) {
			if (autoEscape) {
				target = escapeExistFile(target);
			} else {
				return false;
			}
		}
		if (file.equals(target)) {
			return true;
		}
		return move(file, target);
	}

	/**
	 * 文件(目录)重新命名
	 * 
	 * @param file
	 *            要处理的文件或目录
	 * @param newName
	 *            修改后的文件名（不含路径）。
	 * @param overwite
	 *            覆盖模式，如果目标文件已经存在，则删除目标文件后再改名
	 * @return 如果成功改名，返回改名后的file对象，否则返回null。
	 */
	public static File rename(File file, String newName, boolean overwite) {
		File target = new File(file.getParentFile(), newName);
		if (target.exists()) {
			if (overwite) {
				if (!target.delete())
					return null;
			} else {
				return null;
			}
		}
		return file.renameTo(target) ? target : null;
	}

	/**
	 * 将文件移动为指定的新文件
	 * 
	 * @param oldFile
	 * @param newFile
	 * @return
	 */
	public static boolean move(File oldFile, File newFile) {
		Assert.notNull(oldFile, "source file is null!");
		Assert.notNull(newFile, "target file is null!");
		Assert.isTrue(oldFile.exists(), "source file doesn't exist.");
		Assert.isFalse(newFile.exists(), "target file already exist!");
		ensureParentFolder(newFile);
		return oldFile.renameTo(newFile);
	}

	/**
	 * 将制定文件的编码从 from 字符集转换到 to字符集
	 * 
	 * @param f
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static int converFileEncode(File f, final String from, final String to, String... extPatterns) throws IOException {
		TextFileCallback c = new TextFileCallback(from, to, Dealwith.REPLACE);
		int n = 0;
		if (f.isDirectory()) {
			for (File sub : f.listFiles()) {
				n += converFileEncode(sub, from, to, extPatterns);
			}
		} else {
			if (extPatterns.length == 0 || ArrayUtils.contains(extPatterns, getExtName(f.getName()))) {
				processFile(f, c);
				n++;
			}
		}
		return n;
	}

	/**
	 * 文件过滤器
	 * 
	 * @author jiyi
	 * 
	 */
	public static abstract class FileFilterEx implements FileFilter {
		/**
		 * 是否跳出当前文件夹搜索 每次运行完成accept方法后，程序会执行此方法，如果返回true则将停止在此目录中的搜索。
		 * 
		 * @return true中断文件搜索，false继续搜索。
		 */
		protected boolean breakFolder(File root) {
			return false;
		};
	}

	/**
	 * 总是被继承，用于描述对文件夹的处理
	 * 
	 * @Company: Asiainfo-Linkage Technologies(China),Inc. Hangzhou
	 * @author Administrator
	 * @Date 2011-6-15
	 */
	public abstract static class FolderCallback {
		protected abstract void process(File source);

		protected boolean breakProcess() {
			return false;
		}
	}

	/**
	 * 用指定的回调方法处理文件夹
	 * 
	 * @param root
	 * @param call
	 * @return
	 * @throws IOException
	 */
	public static int processFolders(File root, FolderCallback call) throws IOException {
		int n = 0;
		for (File f : listFolders(root)) {
			call.process(f);
			n += processFolders(f, call);
			if (call.breakProcess())
				break;
		}
		return n;
	}

	/**
	 * 用指定的回调方法处理文本文件(可指定目录并批量处理目录下所有该类型文件)
	 * 
	 * @param f
	 *            文件夹
	 * @param sourceCharset
	 *            源文件编码
	 * @param call
	 *            处理器
	 * @param extPatterns
	 *            扩展名过滤
	 * @throws IOException
	 */
	public static int processFiles(File f, TextFileCallback call, String... extPatterns) throws IOException {
		int n = 0;
		if (f.isDirectory()) {
			for (File sub : f.listFiles()) {
				n += processFiles(sub, call, extPatterns);
			}
		} else {
			if (extPatterns.length == 0 || ArrayUtils.contains(extPatterns, getExtName(f.getName()))) {
				processFile(f, call);
				n++;
			}
		}
		return n;
	}

	/**
	 * 用指定的回调方法处理二进制文件(可指定目录并批量处理目录下所有该类型文件)
	 * 
	 * @param f
	 * @param sourceCharset
	 * @param call
	 * @throws IOException
	 */
	public static int processFiles(File f, BinaryFileCallback call, String... extPatterns) throws IOException {
		int n = 0;
		if (f.isDirectory()) {
			for (File sub : f.listFiles()) {
				n += processFiles(sub, call, extPatterns);
			}
		} else {
			if (extPatterns.length == 0 || ArrayUtils.contains(extPatterns, getExtName(f.getName()))) {
				processFile(f, call);
				n++;
			}
		}
		return n;
	}

	/**
	 * 处理二进制文件
	 * 
	 * @param f
	 * @param call
	 * @return
	 * @throws IOException
	 */
	public static File processFile(File f, BinaryFileCallback call) throws IOException {
		InputStream reader = (f instanceof URLFile) ? ((URLFile) f).getInputStream() : new FileInputStream(f);
		FileOutputStream w = null;
		File target = call.getTarget(f);
		if (target != null) {
			ensureParentFolder(target);
			w = new FileOutputStream(target);
		}
		call.beforeProcess(w, f, target);
		byte[] cache = new byte[call.getMaxBufferSize()];
		int next = call.nextBufferSize();
		int len = 0;
		while ((len = reader.read(cache, 0, next)) > -1) {
			byte[] txt = call.process(ArrayUtils.subArray(cache, len));
			if (w != null) {
				if (txt != null) {
					w.write(txt);
				}
			}
			if (call.breakProcess())
				break;
		}
		reader.close();
		call.afterProcess(w);
		if (w != null)
			w.close();

		if (call.isSuccess()) {
			if (call.replaceSource(f)) {
				if (f.delete()) {
					File n = new File(f.getPath());
					target.renameTo(n);
					return n;
				}
			} else if (call.deleteSource(f)) {
				f.delete();
			}
			return target;
		} else {
			if (target != null) {
				target.delete();
			}
			return null;
		}
	}

	/**
	 * 用指定的回调方法处理文本文件
	 * 
	 * @param f
	 *            文件
	 * @param sourceCharset
	 *            文件编码
	 * @param call
	 *            处理器
	 * @throws IOException
	 */
	public static File processFile(File f, TextFileCallback call) throws IOException {
		if (!call.accept(f)) {
			return null;
		}
		String sourceCharset = call.sourceCharset(f);
		BufferedReader reader = getReader(f, sourceCharset);
		call.sourceFile = f;
		String charSet = call.targetCharset();
		BufferedWriter w = null;
		File target = call.getTarget(f);
		if (target != null) {
			w = getWriter(target, charSet == null ? sourceCharset : charSet, false);
		}
		String line;
		call.beforeProcess(f, target, w);
		while ((line = reader.readLine()) != null) {
			String txt = null;
			try {
				txt = call.processLine(line);
			} catch (Throwable e) {
				LogUtil.exception(e);
				call.lastException = e;
			}
			if (w != null) {
				if (txt != null) {
					w.write(txt);
					if (call.wrapLine())
						// w.newLine();
						w.write("\r\n");
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
			Dealwith deal = call.dealwithSourceOnSuccess(f);
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
				backupfile = escapeExistFile(backupfile);
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

	/**
	 * 追加行到已有的文本文件
	 * 
	 * @param file
	 * @param charset
	 * @param lines
	 * @throws IOException
	 */
	public static void appendToTextFile(File file, String charset, String... lines) throws IOException {
		Assert.isTrue(file.exists(), "The file which you want to append is not exist!" + file.getAbsolutePath());
		FileOutputStream out = new FileOutputStream(file, true);
		String line;
		for (int i = 0; i < lines.length - 1; i++) {
			line = lines[i];
			if (line != null) {
				out.write(charset == null ? line.getBytes() : line.getBytes(charset));
				out.write(StringUtils.CRLF);
			}
		}
		line = lines[lines.length - 1];
		if (line != null)
			out.write(charset == null ? line.getBytes() : line.getBytes(charset));
		out.close();
	}

	/**
	 * 从URL获得reader
	 * 
	 * @param file
	 * @param charSet
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(URL file, String charSet) {
		if (file == null)
			return null;
		try {
			InputStream is = file.openStream();
			UnicodeReader isr = new UnicodeReader(is, charSet);
			return new BufferedReader(isr);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获得一个供读取文本的reader, 本方法可以从BOM自动判断utf-8, unicode等类型，因此charset一般可以为null.
	 * 只有当文件为不带BOM的unicode时才需要指定。
	 * 
	 * @param source
	 * @param charSet
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(File file, String charSet) throws IOException {
		if (file == null)
			return null;
		InputStream is = (file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file);
		UnicodeReader isr = new UnicodeReader(is, charSet);
		return new BufferedReader(isr);
	}

	/**
	 * 获得Reader
	 * 
	 * @param is
	 * @param charSet
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(InputStream is, String charSet) {
		if (is == null)
			return null;
		UnicodeReader isr = new UnicodeReader(is, charSet);
		return new BufferedReader(isr);
	}

	/**
	 * 获得相对于一个class的所在路径的相对路径的文件资源
	 * 
	 * @param source
	 *            class
	 * @param fileName
	 *            文件相对路径
	 * @param charSet
	 *            编码
	 * @return BufferedReader 如果文件不存在，返回null
	 */
	public static BufferedReader getReader(Class<?> source, String fileName, String charSet) {
		InputStream is = source.getResourceAsStream(fileName);
		if (is == null) {
			is = source.getClassLoader().getResourceAsStream(toClassLoaderResourcePath(source, fileName));
		}
		if (is == null)
			return null;
		UnicodeReader isr = new UnicodeReader(is, charSet);
		return new BufferedReader(isr);
	}

	/**
	 * ClassLoader resource不用/开头。这里将c转换过去
	 * 
	 * @param fileName
	 * @return
	 */
	public static String toClassLoaderResourcePath(Class<?> c, String fileName) {
		if (fileName.startsWith("/"))
			return fileName.substring(1);
		String path = c.getPackage().getName().replace('.', '/');
		return path.concat("/").concat(fileName);
	}

	/**
	 * 类加载器的资源路径url，转换
	 * 
	 * @param fileName
	 * @return
	 */
	public static String toClassResourcePath(String fileName) {
		if (fileName.startsWith("/"))
			return fileName;
		return "/".concat(fileName);
	}

	public static BufferedInputStream getInputStream(URL url) throws IOException {
		URLConnection conn = url.openConnection();
		return new BufferedInputStream(conn.getInputStream());
	}

	/**
	 * 将URL转化为文件
	 * 
	 * @param url
	 *            要转换的URL，必须是file://协议，否则抛出异常。
	 */
	public static File urlToFile(URL url) {
		if (url == null)
			return null;
		try {
			URLFile file = new URLFile(url);
			if (file.isLocalFile())
				return file.getLocalFile();
			return file;
		} catch (RuntimeException e) {
			LogUtil.error(url.toString() + " is not a valid file:" + e.getMessage());
			return null;
		}
	}

	/**
	 * 将多个URL转换为多个文件对象
	 * 
	 * @param url
	 * @return
	 */
	public static File[] urlToFile(URL[] url) {
		File[] result = new File[url.length];
		for (int i = 0; i < url.length; i++) {
			result[i] = urlToFile(url[i]);
		}
		return result;
	}

	/**
	 * 获得二进制文件写入句柄
	 * 
	 * @Title: getInputStream
	 */
	public static BufferedInputStream getInputStream(File file) throws IOException {
		return new BufferedInputStream((file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file));
	}

	/**
	 * 获得文本文件写入流
	 * 
	 * @param target
	 * @param charSet
	 * @param append
	 * @return
	 * @throws IOException
	 */
	public static BufferedWriter getWriter(File target, String charSet, boolean append) {
		ensureParentFolder(target);
		try {
			OutputStream os = new FileOutputStream(target, append);
			if (charSet == null)
				charSet = Charset.defaultCharset().name();
			OutputStreamWriter osw = new OutputStreamWriter(os, charSet);
			return new BufferedWriter(osw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * 获得文本文件写入流
	 * 
	 * @param target
	 * @param charSet
	 * @return
	 * @throws IOException
	 */
	public static BufferedWriter getWriter(File target, String charSet){
		return getWriter(target, charSet, false);
	}

	/**
	 * 将OutputStream封装为BufferedWriter
	 * 
	 * @param out
	 * @param charSet
	 * @return
	 */
	public static BufferedWriter getWriter(OutputStream out, String charSet) {
		if (charSet == null)
			charSet = Charset.defaultCharset().name();
		OutputStreamWriter osw;
		try {
			osw = new OutputStreamWriter(out, charSet);
			return new BufferedWriter(osw);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 返回创建文件的流
	 * 
	 * @param file
	 * @return
	 */
	public static BufferedOutputStream getOutputStream(File file) {
		return getOutputStream(file, OverWrittenMode.YES);
	}

	/**
	 * 返回创建文件的流
	 * 
	 * @param file
	 * @param mode
	 * @return
	 */
	public static BufferedOutputStream getOutputStream(File file, OverWrittenMode mode) {
		if (file.exists()) {
			if (mode == OverWrittenMode.NO) {
				return null;
			} else if (mode == OverWrittenMode.ESCAPE_NAME || mode == OverWrittenMode.AUTO) {
				file = IOUtils.escapeExistFile(file);
			} else if (mode == OverWrittenMode.YES && file.isDirectory()) {
				throw new IllegalArgumentException("the folder " + file.getAbsolutePath() + " is already exists");
			}
		}
		ensureParentFolder(file);
		try {
			return new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			LogUtil.exception(e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 将对象序列化在内存中
	 * 
	 * @param obj
	 * @return
	 */
	public static byte[] serialize(Serializable obj) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		saveObject(obj, out);
		return out.toByteArray();
	}

	/**
	 * 将序列化数据还原为对象
	 * 
	 * @param data
	 * @return
	 */
	public static Object deserialize(byte[] data) {
		return loadObject(new ByteArrayInputStream(data));
	}

	/**
	 * 将可序列化的对象存储到流中
	 * 
	 * @param obj
	 * @param output
	 * @return
	 */
	public static boolean saveObject(Serializable obj, OutputStream output) {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(output);
			out.writeObject(obj);
			return true;
		} catch (IOException ex) {
			LogUtil.exception(ex);
			return false;
		} finally {
			closeQuietly(out);
		}
	}

	/**
	 * 将可序列化的对象转换到字节数组
	 * 
	 * @param obj
	 * @return
	 */
	public static byte[] saveObject(Serializable obj) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(2048);
		try {
			ObjectOutputStream out = new ObjectOutputStream(bytes);
			out.writeObject(obj);
			closeQuietly(out);
			return bytes.toByteArray();
		} catch (IOException ex) {
			LogUtil.exception(ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	/**
	 * 将可序列化的对象保存到磁盘文件
	 * 
	 * @param obj
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static boolean saveObject(Serializable obj, File file) {
		try {
			return saveObject(obj, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 将可序列化的对象保存到磁盘文件
	 * 
	 * @param aaa
	 * @param filePath
	 * @return
	 */
	public static boolean saveObject(Serializable aaa, String filePath) {
		return saveObject(aaa, new File(filePath));
	}

	/**
	 * 从流读取序列化对象
	 * 
	 * @param objFile
	 * @return
	 */
	public static Object loadObject(InputStream inn) {
		try {
			ObjectInputStream in = (inn instanceof ObjectInputStream) ? (ObjectInputStream) inn : new ObjectInputStream(inn);
			Object obj = in.readObject();
			if (obj instanceof JefSerializable) {
				((JefSerializable) obj).init();
			}
			return obj;
		} catch (ClassNotFoundException ex) {
			LogUtil.exception(ex);
		} catch (IOException ex) {
			LogUtil.exception(ex);
		} finally {
			IOUtils.closeQuietly(inn);
		}
		return null;
	}

	public static Object loadObject(byte[] objFile) {
		return loadObject(new ByteArrayInputStream(objFile));
	}

	/**
	 * 从磁盘读取指定的序列化对象
	 * 
	 * @param objFile
	 * @return
	 */
	public static Object loadObject(File file) {
		if (!file.exists())
			return null;
		try {
			return loadObject((file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file));
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * 相对路径计算，计算从folder出发，到达file的相对路径
	 * 
	 * @param file
	 * @param folder
	 * @return
	 */
	public static String getRelativepath(String file, String folder) {
		String[] f1 = StringUtils.split(file.replace('/', '\\'), '\\');
		String[] f2 = StringUtils.split(folder.replace('/', '\\'), '\\');
		int breakCount = -1;
		for (int i = 0; i < f1.length; i++) {
			String str = f1[i];
			if (i < f2.length && str.equals(f2[i])) {
				breakCount = i + 1;
			} else {
				break;
			}
		}
		if (breakCount == -1)
			return file;
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat("../", f2.length - breakCount));
		for (int i = breakCount; i < f1.length; i++) {
			sb.append(f1[i] + "/");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 相对路径计算，计算从folder出发，到达file的相对路径
	 * 
	 * @param file
	 * @param folder
	 */
	public static String getRelativepath(File file, File folder) {
		String s1 = getPath(file);
		String s2 = getPath(folder);
		return getRelativepath(s1, s2);
	}


	

	/**
	 * 将ByteBuffer对象脱壳，得到byte[]
	 * 
	 * @param bf
	 *            ByteBuffer对象
	 * @return 字节数组
	 */
	public static byte[] toByteArray(ByteBuffer bf) {
		if (bf.position() != 0) {
			throw new RuntimeException("This method only allow you to read a ByteBuffer from start");
		}
		if (bf.limit() == bf.capacity()) {
			return bf.array();
		}
		byte[] bb = new byte[bf.limit()];
		bf.get(bb);
		return bb;
	}

	/**
	 * 在指定目录下搜索单个文件
	 * 
	 * @Title: findFile
	 * @param root
	 * @param filter
	 *            过滤条件
	 * @return File 返回文件
	 */
	public static File findFile(File root, FileFilterEx filter) {
		if (root == null || !root.exists())
			return null;
		boolean breakThisFolder = false;
		for (File f : root.listFiles()) {
			if (!breakThisFolder) {
				if (filter.accept(f)) {
					return f;
				}
				breakThisFolder = filter.breakFolder(root);
			}
			if (f.isDirectory()) {
				File result = findFile(f, filter);
				if (result != null)
					return result;
			}
		}
		return null;
	}

	/**
	 * 在指定目录下搜索文件
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param filter
	 *            文件过滤器
	 * @return 搜索到的所有文件
	 */
	public static Collection<File> findFiles(File root, FileFilterEx filter) {
		if (root == null || !root.exists())
			return null;
		List<File> result = new ArrayList<File>();
		boolean breakThisFolder = false;
		for (File f : root.listFiles()) {
			if (!breakThisFolder) {
				if (filter.accept(f)) {
					result.add(f);
				}
				breakThisFolder = filter.breakFolder(root);
			}
			if (f.isDirectory()) {
				result.addAll(findFiles(f, filter));
			}
		}
		return result;
	}

	/**
	 * 在指定目录下搜索单个文件
	 * 
	 * @param root
	 *            要搜索的目录
	 * @param name
	 *            搜索的文件名称（完全匹配）
	 * @param acceptFolder
	 *            是否搜索文件夹
	 * @return File 返回文件
	 */
	public static File findFile(File root, final String name, final boolean acceptFolder) {
		return findFile(root, new FileFilterEx() {
			public boolean accept(File pathname) {
				if (!acceptFolder && pathname.isDirectory())
					return false;
				return pathname.getName().equals(name);
			}
		});
	}

	// 一段较为粗糙的代码，目的是区分GB18030和几种UTF编码，但不能识别其他复杂编码
	// 更精确的处理还是要用chardet包才行
	public static String get_charset(File file) {
		String charset = "GB18030";
		byte[] first3Bytes = new byte[3];
		try {
			boolean checked = false;
			BufferedInputStream bis = new BufferedInputStream((file instanceof URLFile) ? ((URLFile) file).getInputStream() : new FileInputStream(file));
			bis.mark(0);
			int read = bis.read(first3Bytes, 0, 3);
			if (read == -1) {
				bis.close();
				return charset;
			}

			if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) { // 高位在前的unicode
				charset = "UTF-16LE";
				checked = true;
			} else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {// 低位在前的unicode
				charset = "UTF-16BE";
				checked = true;
			} else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB && first3Bytes[2] == (byte) 0xBF) {// UTF-8
				charset = "UTF-8";
				checked = true;
			}
			if (checked) {
				bis.close();
				return charset;
			}
			bis.reset();
			// int pos = 0;
			while ((read = bis.read()) != -1) {// 每次读取一个字节
				// pos++;
				if (read >= 0xF0) // 出现0xF0以上，认为是GBK
					break;
				if (0x80 <= read && read <= 0xBF) // 单独出现BF以下的，也算是GBK
					break;
				if (0xC0 <= read && read <= 0xDF) { // c0-df之间的，继续读取判断
					read = bis.read();
					if (0x80 <= read && read <= 0xBF) // 双字节 (0xC0 - 0xDF) -
														// (0x80 -
														// 0xBF),也可能在GBK编码内
						continue;
					else
						break;
				} else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
					read = bis.read();
					if (0x80 <= read && read <= 0xBF) {
						read = bis.read();
						if (0x80 <= read && read <= 0xBF) {
							charset = "UTF-8";
							break;
						} else
							break;
					} else
						break;
				}
			}
			bis.close();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		return charset;
	}

	/**
	 * 比较两个文件/目录是否内容一致。 <br>
	 * 作为传入参数的文件/目录的名称不会被比较。
	 * 
	 * @param origin
	 *            源文件。可传入文件或目录
	 * @param target
	 *            目标文件。可传入文件或目录
	 * @return
	 */
	public static boolean equals(File origin, File target) {
		boolean isFile = origin.isFile();
		if (isFile != target.isFile()) {
			return false;
		}
		if (isFile) {
			// 文件比较
			return FileComparator.LENGTH_SKIP.equals(origin, target);
		} else if (origin.isDirectory() && target.isDirectory()) {
			File[] ss = origin.listFiles();
			File[] ts = target.listFiles();
			if (ss.length != ts.length) {
				return false;
			}
			// 目录比较
			for (File file : ss) {
				File newTarget = new File(target, file.getName());
				boolean flag = equals(file, newTarget);
				if (!flag) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Read in a "logical line" from an InputStream/Reader, skip all comment and
	 * blank lines and filter out those leading whitespace characters ( , and )
	 * from the beginning of a "natural line". Method returns the char length of
	 * the "logical line" and stores the line in "lineBuf".
	 */
	static final class LineReader {
		private char[] inCharBuf;
		private char[] lineBuf = new char[1024];
		private int inLimit = 0;
		private int inOff = 0;
		private Reader reader;

		public LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		int readLine() throws IOException {
			int len = 0;
			char c = 0;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff >= inLimit) {
					inLimit = reader.read(inCharBuf);
					inOff = 0;
					if (inLimit <= 0) {
						if (len == 0 || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				c = inCharBuf[inOff++];

				if (skipLF) {
					skipLF = false;
					if (c == '\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if (c == ' ' || c == '\t' || c == '\f') {
						continue;
					}
					if (!appendedLineBegin && (c == '\r' || c == '\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if (c == '#' || c == '!') {
						isCommentLine = true;
						continue;
					}
				}

				if (c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if (len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if (newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					// flip the preceding backslash flag
					if (c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
				} else {
					// reached EOL
					if (isCommentLine || len == 0) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff >= inLimit) {
						inLimit = reader.read(inCharBuf);
						inOff = 0;
						if (inLimit <= 0) {
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						// skip the leading whitespace characters in following
						// line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c == '\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

	private static void load0(LineReader lr, Map<String, String> map) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		while ((limit = lr.readLine()) >= 0) {
			c = 0;
			keyLen = 0;
			valueStart = limit;
			hasSep = false;

			precedingBackslash = false;
			while (keyLen < limit) {
				c = lr.lineBuf[keyLen];
				// need check if escaped.
				if ((c == '=' || c == ':') && !precedingBackslash) {
					valueStart = keyLen + 1;
					hasSep = true;
					break;
				} else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
					valueStart = keyLen + 1;
					break;
				}
				if (c == '\\') {
					precedingBackslash = !precedingBackslash;
				} else {
					precedingBackslash = false;
				}
				keyLen++;
			}
			while (valueStart < limit) {
				c = lr.lineBuf[valueStart];
				if (c != ' ' && c != '\t' && c != '\f') {
					if (!hasSep && (c == '=' || c == ':')) {
						hasSep = true;
					} else {
						break;
					}
				}
				valueStart++;
			}
			String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
			String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
			map.put(key, value);
		}
	}

	/*
	 * Converts encoded &#92;uxxxx to unicode chars and changes special saved
	 * chars to their original forms
	 */
	private static String loadConvert(char[] in, int off, int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = len * 2;
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;

		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					else if (aChar == 'f')
						aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = (char) aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	private static String saveConvert(String theString, boolean escapeSpace) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuilder outBuffer = new StringBuilder(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				outBuffer.append(aChar);
			}
		}
		return outBuffer.toString();
	}
	
	/*
	 * 内部使用,properties文件读取
	 */
	static final void loadProperties(Reader in, Map<String, String> map) {
		if (in == null)
			return;
		try {
			load0(new LineReader(in), map);
		} catch (Exception e1) {
			LogUtil.exception(e1);
		} finally {
			closeQuietly(in);
		}
	}
}
