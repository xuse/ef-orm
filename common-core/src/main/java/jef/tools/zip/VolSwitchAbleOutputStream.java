package jef.tools.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.FileName;
import jef.tools.IOUtils;

public class VolSwitchAbleOutputStream extends SwitchAbleOutputStream{
	private File templateFile;
	private File firstFile;
	
	public VolSwitchAbleOutputStream(File firstFile, long volumnSize) throws FileNotFoundException{
		super(new FileOutputStream(firstFile),volumnSize);
		this.templateFile=firstFile.getAbsoluteFile();
		this.firstFile=firstFile;
	}

	@Override
	protected OutputStream getNextVolOutputStream(int currentIndex) {
		File parent=templateFile.getParentFile();
		Assert.folderExist(parent);
		FileName names=new FileName(templateFile.getName());
		if(currentIndex==1){//第一个文件自动改名
			String first=names.getValueIfAppend(".part"+(currentIndex));
			this.firstFile=IOUtils.rename(templateFile, first, true);
			if(firstFile==null){
				throw new IllegalAccessError("Can not rename file "+templateFile.getAbsolutePath()+" to "+ first);
			}
			LogUtil.debug(templateFile.getPath()+" rename to "+firstFile.getPath());
		}
		try {
			File newFile=new File(parent,names.getValueIfAppend(".part"+(currentIndex+1)));
			LogUtil.debug("Create new Volumn File:"+ newFile.getPath());
			return new FileOutputStream(newFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getFirstVolFile() {
		return firstFile;
	}
}
