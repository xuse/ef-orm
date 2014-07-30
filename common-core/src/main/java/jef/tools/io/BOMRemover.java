package jef.tools.io;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

import jef.tools.BinaryFileCallback;


/**
 * 二进制文件处理器：删除BOM字符.(其他字符也可)
 * @author Administrator
 * @Date 2011-7-1
 */
public class BOMRemover extends BinaryFileCallback {
	int n=0;
	private boolean isBomFile=false;
	byte[] start;
	

	@Override
	protected void beforeProcess(FileOutputStream w, File f, File target) {
		isBomFile=false;
	}

	private File current;
	@Override
	protected File getTarget(File source) {
		this.current=source;
		return super.getTarget(source);
	}

	@Override
	protected int nextBufferSize() {
		if(isBomFile)return super.getMaxBufferSize();
		return start.length;
	}

	@Override
	public boolean isSuccess() {
		return isBomFile;
	}

	public BOMRemover(byte[] bom){
		this.start=bom;
	}
	
	public BOMRemover() {
		this(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
	}


	@Override
	protected boolean replaceSource(File source) {
		return true;
	}

	@Override
	protected boolean breakProcess() {
		return !isBomFile;
	}

	@Override
	public byte[] process(byte[] data) {
		if(isBomFile)return data;
		if(Arrays.equals(start, data)){
			System.out.println("ISBOM!"+current.getAbsolutePath());
			isBomFile=true;
			return null; 
		}
		return data;
	}
}
