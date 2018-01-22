package jef.tools;

import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

import jef.common.log.LogUtil;
import jef.tools.ZipUtils.EntryProcessor;
import jef.tools.zip.ZipInputStream;
import jef.tools.zip.ZipOutputStream;

import org.junit.Ignore;
import org.junit.Test;

public class ZipUtilsTest {
	/**
	 * 分卷压缩测试，以5048576为一卷进行压缩。
	 * 
	 * @throws IOException
	 */
	@Test
	public void zipVolTest() throws IOException {
		{
			ZipUtils.TarLongFileNameMode = 1;
			File dir = ResourceUtils.getResourceFile("testfile/NTDETECT.123");
			File target = new File("./123.tar.gz");
			File zipped=ZipUtils.targz(target, new EntryProcessor(){
				@Override
				protected long getVolumnSize() {
					return 5048576;
				}
			}, dir);
			LogUtil.info("Ziped file: {}", zipped.getPath());	
		}
		
		/**
		 * 分卷解压测试
		 * @throws IOException
		 */
		{
			File dir = new File("./123.tar.gz");
			ZipUtils.unTarGz(dir, "./",null);
			LogUtil.show("Unziped file:" + dir.getPath());	
		}
	}

	/**
	 * A SImple test
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void zipTest() throws IOException {
		ZipUtils.TarLongFileNameMode = 1;

		File dir = new File("src/test/java");
		File target = new File("/123.tar");
		ZipUtils.tar(target, dir);
		assertTrue(target.length() > 0);

		File gz = new File("/123.tar.gz");
		ZipUtils.gzip(target, gz,10240);
		assertTrue(gz.length() > 0);

		File gz2 = new File("/123a.tar.gz");
		ZipUtils.targz(gz2, dir);
		gz2.delete();
		gz.delete();

		ZipUtils.unTarGz(gz2, "/temp", null);
		target.delete();
	}

	/**
	 * A SImple test
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void zipTest1() throws IOException {
		File dir = new File("src/test/java");
		ZipUtils.targz(new File("/123.tar.gz"), new EntryProcessor() {

		}, dir);
	}
	
	public void zipTest2() throws IOException {
		//将C盘，D盘，E盘的所有内容都打包到一个压缩文件去。
		ZipUtils.zip(new File("F:/myArchive.zip"), new File("c:/"),new File("d:/"),new File("e:/"));
	}

	/**
	 * 在ZIp中添加文件
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void addFileToZip() throws IOException {
		ZipInputStream in = new ZipInputStream(new FileInputStream("c:/123.zip"), "GB18030");
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File("c:/456.zip")), "GB18030");
		ZipEntry fEntry = null;
		while ((fEntry = in.getNextEntry()) != null) {
			// String entryName = fEntry.getName();
			out.putNextEntry(fEntry);

		}
		out.close();
		in.close(); // 鍏抽棴杈撳叆娴�
	}
}
