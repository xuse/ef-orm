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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

import jef.common.BigDataBuffer;
import jef.common.log.LogUtil;
import jef.tools.support.ArchiveSummary;
import jef.tools.zip.TarEntry;
import jef.tools.zip.TarInputStream;
import jef.tools.zip.TarOutputStream;
import jef.tools.zip.VolSwitchAbleOutputStream;
import jef.tools.zip.VolumnChangeableInputStream;
import jef.tools.zip.VolumnOutputStream;
import jef.tools.zip.ZipInputStream;
import jef.tools.zip.ZipOutputStream;

/**
 * JEF压缩解压的通用包：目前支持以下格式的文件
 * <ul>
 * <li>zip 压缩/解压 密码不支持，修复了JDK的编码问题。</li>
 * <li>tar.gz 压缩/解压  修复了Apache同名类的编码问题。</li>
 * <li>tar 压缩/解压</li>
 * </ul>
 */
public class ZipUtils {
	static {
		ZipOutputStream.DEFAULT_NAME_ENCODING = "GB18030";
		TarEntry.DEFAULT_NAME_ENCODING="GB18030";
	}
	public static int TarLongFileNameMode=0;
	
	/**
	 * 压缩为zip文件
	 * 
	 * @param zipFileName
	 *            压缩包路径
	 * @param inputName
	 *            源路径
	 * @throws IOException
	 */
	public static File zip(String zipFileName, String inputName) throws IOException {
		return zip(new File(zipFileName), new File(inputName));
	}

	/**
	 * 压缩为zip文件
	 * 
	 * @param zipFile 压缩包文件
	 * @param inputFiles
	 *            多个压缩源
	 * @throws IOException
	 */
	public static File zip(File zipFile, File... inputFiles) throws IOException {
		VolumnOutputStream vol = new VolumnOutputStream(new VolSwitchAbleOutputStream(zipFile,0));
		ZipOutputStream out=new ZipOutputStream(vol);
		for (File f : inputFiles) {
			zip(out, f, null, null);
		}
		out.flush();
		out.close();
		return vol.getFirstVolFile();
	}
	
	
	/**
	 * 压缩为zip文件
	 * @param zipFile 压缩包文件
	 * @param ep      压缩处理回调
	 * @param inputFiles 压缩源文件
	 * @throws IOException
	 */
	public static File zip(File zipFile, EntryProcessor ep, File... inputFiles) throws IOException {
		long size=ep==null?0:ep.getVolumnSize();
		VolumnOutputStream vol = new VolumnOutputStream(new VolSwitchAbleOutputStream(zipFile,size));
		ZipOutputStream out=new ZipOutputStream(vol);
		for (File f : inputFiles) {
			zip(out, f, null, ep);
		}
		out.flush();
		out.close();
		return vol.getFirstVolFile();
	}


	/*
	 * 递归压缩方法
	 * 
	 * @param out 压缩包输出流
	 * 
	 * @param f 需要压缩的文件
	 * 
	 * @param base压缩包中的路径
	 */
	private static void zip(ZipOutputStream out, File f, String base, EntryProcessor ep) throws IOException {
		Assert.exist(f);
		if (StringUtils.isNotEmpty(base) && !base.endsWith("/"))
			base = base.concat("/");
		if (f.isDirectory()) {
			base = StringUtils.toString(base) + f.getName() + "/";
			base = (ep == null) ? base : ep.getZippedPath(f, base);
			if (base != null) {
				out.putNextEntry(new ZipEntry(base));
				for (File fl : f.listFiles()) {
					zip(out, fl, base, ep);
					if (ep!=null && ep.breakProcess())
						break;
				}
			}
		} else { // 如果是文件，则压缩
			String entryName = StringUtils.toString(base) + f.getName();
			entryName = ep == null ? entryName : ep.getZippedPath(f, entryName);
			if (entryName != null) {
				LogUtil.debug("adding to Zip:" + entryName);
				out.putNextEntry(new ZipEntry(entryName)); // 生成下一个压缩节点
				FileInputStream in = new FileInputStream(f); // 读取文件内容
				IOUtils.copy(in, out, false);
				in.close();
			}
		}
	}

	/**
	 * 解压zip文件
	 * 
	 * @param zipFile
	 *            压缩包
	 * @param unzipPath
	 *            解压路径
	 * @return
	 */
	public static boolean unzip(String zipFile, String unzipPath) {
		return unzip(new File(zipFile), unzipPath, null,null);
	}

	/**
	 * 解压zip文件
	 * 
	 * @param file
	 *            压缩包
	 * @param unzipPath
	 *            解压路径
	 * @throws IOException
	 */
	public static boolean unzip(File file, String unzipPath, String charset,EntryProcessor cd) {
		InputStream in = null;
		try {
			in = new BufferedInputStream(new VolumnChangeableInputStream(file));
			unzip(in, unzipPath, charset, cd);
			return true;
		} catch (IOException e) {
			LogUtil.exception(e);
			return false;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * 单个文件Gzip压缩
	 * @param source 未压缩文件
	 * @param target 压缩文件
	 * @throws IOException
	 * @author Administrator
	 */
	public static File gzip(File source,File targetFile,long volumnSize) throws IOException {
		VolumnOutputStream vol=new VolumnOutputStream(new VolSwitchAbleOutputStream(targetFile, volumnSize));
		GZIPOutputStream target=new GZIPOutputStream(vol);
		IOUtils.copy(IOUtils.getInputStream(source), target, true);
		return vol.getFirstVolFile();
	}
	
	/**
	 * 单个文件Gzip解压缩
	 * @param source  压缩文件
	 * @param target  解压后文件
	 * @throws IOException
	 */
	public static void ungzip(File source,File target) throws IOException {
		IOUtils.copy(new GZIPInputStream(IOUtils.getInputStream(source)), IOUtils.getOutputStream(target), true);
	}
	
	/**
	 * 解压
	 * 
	 * @param file
	 *            压缩包
	 * @param unzipPath
	 *            解压路径
	 * @param charSet
	 *            压缩包内的文件名编码(可为null)
	 * @param cd
	 *            压缩处理器，压缩包的每个文件名都可以经过该类的检查和修正。(可以null)
	 * @throws IOException
	 */
	public static void unzip(InputStream ins, String unzipPath, String charSet, EntryProcessor cd) throws IOException {
		ZipInputStream in = null;
		try {
			in = new ZipInputStream(ins, charSet);
			ZipEntry fEntry = null;
			while ((fEntry = in.getNextEntry()) != null) {
				String entryName = fEntry.getName();
				if (cd != null) {
					entryName = cd.getExtractName(entryName, fEntry.getCompressedSize(), fEntry.getSize());
				}
				if (entryName != null) {
					String fname = unzipPath + "/" + entryName;
					if (fname.endsWith("/")) {
						IOUtils.createFolder(fname);
						continue;
					}
					byte[] doc = new byte[1024];
					File output = new File(fname);
					if (!output.getParentFile().exists()) {
						output.getParentFile().mkdirs();
					}
					FileOutputStream out = new FileOutputStream(fname);
					int n;
					while ((n = in.read(doc, 0, 1024)) != -1)
						out.write(doc, 0, n);
					out.close();
					out = null;
					doc = null;
				}
			}
		} finally {
			if (in != null)
				in.close(); // 关闭输入流
		}
	}

	/**
	 * 压缩为tar.gz格式文件
	 * 
	 * @param source
	 * @param tarFilename
	 * @throws IOException
	 */
	public static File targz(String tarFilename, String source) throws IOException {
		return targz(new File(source), new File(tarFilename));
	}

	/**
	 * 压缩为tar.gz格式的文件
	 * 
	 * @param source
	 * @param tarFilename
	 * @throws IOException
	 */
	public static File targz(File zipFile, File... source) throws IOException {
		return targz(zipFile,null,source);
	}
	
	/**
	 * 打包成targz文件
	 * @param zipFile
	 * @param ep
	 * @param inputFiles
	 * @throws IOException
	 * @return file 返回压缩成功后的压缩文件（如果是分卷压缩返回第一个分卷文件，如果压缩不成功返回null）
	 */
	public static File targz(File zipFile, EntryProcessor ep, File... inputFiles) throws IOException {
		if(inputFiles==null || inputFiles.length==0){
			return null;
		}
		BigDataBuffer bf=new BigDataBuffer();
		TarOutputStream tarout=new TarOutputStream(bf);
		tarout.setLongFileMode(TarLongFileNameMode);
		try {
			for(File file: inputFiles){
				tar(tarout,file,"",ep);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			tarout.flush();
			tarout.close();
		}
		long size=ep==null?0:ep.getVolumnSize();
		VolumnOutputStream vol=new VolumnOutputStream(new VolSwitchAbleOutputStream(zipFile, size));
		GZIPOutputStream out=new GZIPOutputStream(vol);
		IOUtils.copy(bf.getAsStream(), out, true);
		bf.close();
//		System.out.println(vol.getTotal());
		return vol.getFirstVolFile();
	}

	/**
	 * 压缩tar格式的压缩文件
	 * 
	 * @param inputFilename
	 *            压缩文件
	 * @param tarFilename
	 *            输出路径
	 * @throws IOException
	 */
	public static File tar(String inputFilename, String tarFilename) throws IOException {
		return tar(new File(inputFilename), new File(tarFilename));
	}

	/**
	 * 压缩tar格式的压缩文件
	 * 
	 * @param inputFile
	 *            压缩文件
	 * @param tarFilename
	 *            输出路径
	 * @throws IOException
	 */
	public static File tar(File tarFilename,File... inputFile) throws IOException {
		return tar(tarFilename,null,inputFile);
	}

	/**
	 * 压缩tar格式的压缩文件
	 * @param tarFile
	 * @param ep
	 * @param inputFile
	 * @throws IOException
	 */
	public static File tar(File tarFile,EntryProcessor ep,File... inputFile) throws IOException {
		TarOutputStream out = new TarOutputStream(new FileOutputStream(tarFile));
		out.setLongFileMode(TarLongFileNameMode);
		try {
			for(File input: inputFile){
				tar(out,input,"",ep);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			out.flush();
			out.finish();
			out.close();
		}
		return tarFile;
		
	}
	
	/**
	 * 将多个文件以Tar格式打包后 放入一个缓冲区内
	 * @param ep
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static BigDataBuffer tarBuffer(EntryProcessor ep,File... inputFile) throws IOException {
		BigDataBuffer result=new BigDataBuffer();
		TarOutputStream out=new TarOutputStream(result);
		out.setLongFileMode(TarLongFileNameMode);
		try {
			for(File input: inputFile){
				tar(out,input,"",ep);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			out.flush();
			out.finish();
			out.close();
		}
		return result;
	}
	
	
	/**
	 * 压缩tar格式的压缩文件
	 * 
	 * @param f
	 *            压缩文件
	 * @param out
	 *            输出文件
	 * @param base
	 *            结束标识
	 * @throws IOException
	 */
	private static void tar(TarOutputStream out, File f,String base,EntryProcessor ep) throws IOException {
		Assert.exist(f);
		if (StringUtils.isNotEmpty(base) && !base.endsWith("/"))
			base = base.concat("/");
		if (f.isDirectory()) {
			base = StringUtils.toString(base) + f.getName() + "/";
			base = (ep == null) ? base : ep.getZippedPath(f, base);
			if (base != null) {
//				LogUtil.debug("folder to Tar:" +  base);
				out.putNextEntry(new TarEntry(base));
				for (File file : f.listFiles()) {
					tar(out, file, base, ep);
					if (ep!=null && ep.breakProcess())
						break;
				}
				out.closeEntry();
			}
		} else {
			String entryName = StringUtils.toString(base) + f.getName();
			entryName = ep == null ? entryName : ep.getZippedPath(f, entryName);
			if (entryName != null) {
				TarEntry entry=new TarEntry(entryName);
				entry.setSize(f.length());
//				LogUtil.debug("adding to Tar:" + entryName+" "+f.length());
				out.putNextEntry(entry); // 生成下一个压缩节点
				FileInputStream in = new FileInputStream(f); // 
				IOUtils.copy(in, out, false);
				in.close();
				out.closeEntry();
			}
		}
	}
	
	
	/**
	 * 解压tar.gz压缩包
	 * 
	 * @param archivePath
	 *            压缩包路径
	 * @param unzipPath
	 *            解压路径
	 * @throws IOException
	 */
	public static boolean unTarGz(String archivePath, String unzipPath) {
		return unTarGz(new File(archivePath), unzipPath, null);
	}

	/**
	 * 解压tar.gz格式的压缩包
	 * 
	 * @param in
	 * @param unzipPath
	 * @param cd
	 * @throws IOException
	 */
	public static boolean unTarGz(File file, String unzipPath, EntryProcessor cd) {
		try {
			InputStream in = new BufferedInputStream(new VolumnChangeableInputStream(file));
			return unTarGz(in, unzipPath, cd);
		} catch (IOException e) {
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 解压tar.gz格式的输入流
	 * 
	 * @param in
	 * @param unzipPath
	 * @param cd
	 * @throws IOException
	 */
	public static boolean unTarGz(InputStream in, String unzipPath, EntryProcessor cd) {
		try {
			untar(new GZIPInputStream(in), unzipPath, cd);
			return true;
		} catch (IOException e) {
			LogUtil.exception(e);
			return false;
		}
	}
	
	/**
	 * 解压tar格式的压缩文件
	 * @param archivePath
	 * @param unzipPath
	 * @return
	 */
	public static boolean untar(String archivePath, String unzipPath) {
		return untar(new File(archivePath), unzipPath, null);
	}
	
	/**
	 * 解压tar格式的压缩文件到指定目录下
	 * 
	 * @param tarFileName
	 *            压缩文件
	 * @param extPlace
	 *            解压目录
	 * @throws Exception
	 */
	public static boolean untar(File file, String unzipPath, EntryProcessor cd) {
		try {
			untar(new BufferedInputStream(new VolumnChangeableInputStream(file)), unzipPath, cd);
			return true;
		} catch (IOException e) {
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 解压tar格式的文件
	 * 
	 * @param tarStream
	 * @param unzipPath
	 * @param cd
	 * @throws IOException
	 */
	public static void untar(InputStream tarStream, String unzipPath, EntryProcessor cd) throws IOException {
		TarInputStream in = null;
		try {
			in = new TarInputStream(tarStream);
			TarEntry fEntry = null;
			while ((fEntry = in.getNextEntry()) != null) {
				String entryName = fEntry.getName();
//				LogUtil.debug("untar:"+ entryName+" "+ fEntry.getSize());
				if (cd != null) {
					entryName = cd.getExtractName(entryName, fEntry.getSize(), fEntry.getSize());
				}
				if (entryName != null) {
					String fname = unzipPath + "/" + entryName;
					if (fEntry.isDirectory()) {
						IOUtils.createFolder(fname);
						continue;
					}else{
						OutputStream out = IOUtils.getOutputStream(new File(fname));
						@SuppressWarnings("unused")
						long size=IOUtils.copy(in, out, false,true);
//						LogUtil.debug("size="+size);
					}
				}else{
					long size=in.skip(fEntry.getSize());
					if(size>0)LogUtil.debug("left:" + size);
				}
				if (cd != null && cd.breakProcess())
					break;
			}
		} finally {
			if (in != null)
				in.close(); // 关闭输入流
		}
	}

	/**
	 * 得到tar压缩文件的摘要信息
	 * @param file
	 * @return
	 */
	public static ArchiveSummary getTarSummary(File file) {
		SummaryCollector sc = new SummaryCollector();
		untar(file, null, sc);
		return sc.getSummary();
	}
	/**
	 * 得到zip压缩文件的摘要信息
	 * @param file
	 * @return
	 */
	public static ArchiveSummary getZipArchiveSummary(File file) {
		SummaryCollector sc = new SummaryCollector();
		unzip(file, null,null, sc);
		return sc.getSummary();
	}
	/**
	 * 得到targz压缩文件的摘要信息
	 * @param file
	 * @return
	 */
	public static ArchiveSummary getTarGzSummary(File file) {
		SummaryCollector sc = new SummaryCollector();
		unTarGz(file, null, sc);
		return sc.getSummary();
	}

	/**
	 *  默认的EntryProcessor实现B，目的收集压缩包的各项信息
	 * @author Administrator
	 *
	 */
	public static class SummaryCollector extends EntryProcessor {
		private ArchiveSummary summary;

		public SummaryCollector() {
			summary = new ArchiveSummary();
		}

		public boolean breakProcess() {
			return false;
		}

		public String getExtractName(String input, long packedSize, long unpackedSize) {
			summary.addItem(input, packedSize, unpackedSize);
			return null;
		}

		public ArchiveSummary getSummary() {
			return summary;
		}
	}

	/**
	 * 默认的EntryProcessor实现B，目的是在控制台上打印出压缩解压进度
	 * @author Administrator
	 *
	 */
	public static class ConsoleShow extends EntryProcessor {
		private ArchiveSummary summary;
		private long currentPosition = 0;
		private long nextPromptSize = 0;// 下次提示
		private int count = 0;
		private long step; // 每次提示的步长
		private String name;

		public ConsoleShow(String name, ArchiveSummary size) {
			this.name = name;
			this.summary = size;
			this.step = size.getPackedSize() / 10;
			if (step < 100)
				step = 100;
		}

		public boolean breakProcess() {
			return false;
		}

		public String getExtractName(String input, long ps, long unp) {
			count++;
			if (currentPosition >= nextPromptSize) {
				nextPromptSize += step;
				String percent = count + "/" + summary.getItemCount() + " " + StringUtils.toPercent(currentPosition, summary.getPackedSize());
				LogUtil.debug(name.replace("%%", percent).replace("$$", input));
			}
			currentPosition += ps;
			return input.replace('?', '_');
		}

		public String getZippedPath(File source, String zippedBase) {
			return null;
		}
	}

	public static ConsoleShow getConsoleProgressHandler(String msg, ArchiveSummary summary) {
		return new ConsoleShow(msg, summary);
	}

	/**
	 * 压缩处理器
	 * 抽象类，你可以覆盖整个类的各种方法，实现以下功能
	 * 1、指定分卷压缩的大小
	 * 2、指定压缩后文件的名称、指定解压后文件的路径
	 * 3、指定某些文件跳过不压缩或者不解压
	 * 4、通过覆盖对应的事件，可以计算压缩解压的进度、时间、字节数等
	 * 当需要定制上述特殊行为时，可以传入一个处理器，实现你需要的逻辑
	 * @author Administrator
	 * @Date 2011-7-7
	 */
	public abstract static class EntryProcessor {
		/**
		 * 当一个文件将被解压前调用，返回压缩后的文件路径
		 * @param entryName  压缩包中的文件路径
		 * @param packedSize 文件压缩后大小
		 * @param unpackedSize 压缩前大小
		 * @return 解压后文件（相对）路径，默认应当和entryName一致。
		 * 			如果不想解压此文件，可以return null.
		 */
		protected String getExtractName(String entryName, long packedSize, long unpackedSize) {
			return entryName;
		}
		/**
		 * 返回分卷大小
		 * 如果返回0表示无须分卷
		 */
		protected long getVolumnSize(){
			return 0;
		}
		/**
		 * 当一个文件将被压缩前调用，返回文件在压缩包中的路径
		 * @param source   源文件
		 * @param zippedPath 默认压缩包中的文件路径
		 * @return  默认压缩包中的文件路径，如果不想压缩此文件，返回null.
		 */
		protected String getZippedPath(File source, String zippedPath) {
			return zippedPath;
		}
		/**
		 * 当每个文件压缩/解压后执行
		 * @return true,继续压缩/解压任务。false则中断整个操作
		 */
		protected boolean breakProcess() {
			return false;
		}
	}
}
