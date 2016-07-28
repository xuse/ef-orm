package jef.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import jef.common.log.LogUtil;

/**
 * 文件比较器，用于比较两个文件是否相同
 * @author jiyi
 * @see #LENGTH_CRC
 * @see #LENGTH_ONLY
 * @see #LENGTH_SKIP
 */
public abstract class FileComparator {
	public abstract boolean equals(File source, File target);

	protected boolean compareFullContent(File source, File target) {
		try {
			String a = StringUtils.getCRC(IOUtils.getInputStream(source));
			String b = StringUtils.getCRC(IOUtils.getInputStream(target));
			return a.equals(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected boolean compareClips(File source, File target, long len) {
		if (len < 20480) {
			return compareFullContent(source, target);
		}
		long skipsize = (len / 10) - 1024;
		byte[] buf1 = new byte[1024];
		byte[] buf2 = new byte[1024];
		RandomAccessFile f1 = null;
		RandomAccessFile f2 = null;
		try {
			f1 = new RandomAccessFile(source, "r");
			f2 = new RandomAccessFile(target, "r");
			long seek = 0;
			while (seek < len) {
				f1.seek(seek);
				int l1 = f1.read(buf1);
				f2.seek(seek);
				int l2 = f2.read(buf2);
				seek += skipsize;
				if (!compareBuffer(buf1, l1, buf2, l2)) {
					return false;
				}
			}
			return true;
		} catch (IOException e) {
			LogUtil.error("CompareFileError:",e);
			return false;
		} finally {
			IOUtils.closeQuietly(f1);
			IOUtils.closeQuietly(f2);
		}
	}

	private boolean compareBuffer(byte[] buf1, int l1, byte[] buf2, int l2) {
		if(l1!=l2)return false;
		for(int i=0;i<l1;i++){
			if(buf1[i]!=buf2[i])return false;
		}
		return true;
	}

	/**
	 * 比较器：比较两个文件长度，长度相同就认为相同
	 */
	public static FileComparator LENGTH_ONLY = new FileComparator() {
		public boolean equals(File source, File target) {
			if (source.isFile() && target.isFile()) {
				return source.length() == target.length();
			}
			return false;
		}
	};
	/**
	 * 比较器：将两个文件取CRC后进行比较，能够比较出全部内容上的差异
	 */
	public static FileComparator LENGTH_CRC = new FileComparator() {
		public boolean equals(File source, File target) {
			if (source.isFile() && target.isFile()) {
				if (source.length() == target.length()) {
					return compareFullContent(source, target);
				}
			}
			return false;
		}
	};
	/**
	 * 比较器：在文件大小相同的前提下
	 * 将文件分为10个小段，每个小段比较1024字节（抽样比较文件）
	 * 适用于大文件的快速比较
	 */
	public static FileComparator LENGTH_SKIP = new FileComparator() {
		public boolean equals(File source, File target) {
			if (source.isFile() && target.isFile()) {
				long len = source.length();
				if (len == target.length()) {
					return compareClips(source, target, len);
				}
			}
			return false;
		}

	};
}
