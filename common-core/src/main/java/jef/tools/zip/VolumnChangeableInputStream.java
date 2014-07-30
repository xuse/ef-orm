package jef.tools.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jef.common.log.LogUtil;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.string.RegexpUtils;

public final class VolumnChangeableInputStream extends AbstractVolchgInoputStream{
	private File file;
	
	
	private String baseName;
	private String extName;
	private int index = -1;
	
	public VolumnChangeableInputStream(File file) throws IOException{
		this.file=file.getAbsoluteFile();
		init();
	}
	
	private void init() throws IOException {
		String fName=file.getName();
		String[] strs=RegexpUtils.getMatcherResult(fName, "(.*\\.part)(\\d{1,3})(\\.\\w+)", true);
		in=new FileInputStream(file);
		if(strs!=null){
			index=StringUtils.toInt(strs[1], 1);
			if(index!=1){
				return;
			}
			baseName=strs[0];
			extName=strs[2];
		}
	}

	

	/**
	 * 尝试切换卷，如果切换成功返回true,如果没有后继分卷返回false
	 * @return
	 * @throws IOException 
	 */
	protected boolean changeVolumn() throws IOException {
		if(baseName==null)return false;
		File next=new File(file.getParentFile(),baseName+(index+1)+extName);
		if(next.exists() && next.isFile()){
			IOUtils.closeQuietly(in);
			in=new FileInputStream(next);
			index++;
			LogUtil.debug("Change volumn to "+ next.getName());
			return true;
		}
		return false;
	}
}
