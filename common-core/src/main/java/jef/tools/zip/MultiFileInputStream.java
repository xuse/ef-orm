package jef.tools.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jef.tools.IOUtils;

public class MultiFileInputStream extends AbstractVolchgInoputStream {

	private final List<File> filelist = new ArrayList<File>();
	private int index = 0;

	public int getFileSize() {
		return filelist.size();
	}

	public File[] getFiles() {
		return filelist.toArray(new File[filelist.size()]);
	}

	public MultiFileInputStream(File dir,String... files) {
		for (String s : files) {
			if (s != null) {
				File file = new File(dir,s);
				if (!file.exists()) {
					throw new IllegalArgumentException("file " + s + " not exist.");
				}
				filelist.add(file);
			}
		}
		init();
	}
	
	
	public MultiFileInputStream(String... files) {
		for (String s : files) {
			if (s != null) {
				File file = new File(s);
				if (!file.exists()) {
					throw new IllegalArgumentException("file " + s + " not exist.");
				}
				filelist.add(file);
			}
		}
		init();
	}

	public MultiFileInputStream(File... files) {
		for (File f : files) {
			if (f != null)
				filelist.add(f);
		}
		init();
	}

	private void init() {
		try {
			in = new FileInputStream(filelist.get(index++));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void add(File file) {
		if (file != null)
			filelist.add(file);
	}

	@Override
	protected boolean changeVolumn() throws IOException {
		if (index < filelist.size()) {
			IOUtils.closeQuietly(in);
			in = new FileInputStream(filelist.get(index++));
			return true;
		}
		return false;
	}

}
