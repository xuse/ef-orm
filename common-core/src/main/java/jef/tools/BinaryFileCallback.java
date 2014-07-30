package jef.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class BinaryFileCallback {
	protected abstract int nextBufferSize();
	
	/**
	 * 指定输出文件
	 * 
	 * @param source
	 * @return
	 */
	protected File getTarget(File source) {
		return new File(source.getPath().concat(".tmp"));
	}


	/**
	 * 允许继承类在处理前操作输出目标
	 * 
	 * @param w
	 * @throws IOException
	 * @deprecated 改用jef.tools.BinaryFileCallback.beforeProcess(FileOutputStream, File, File)
	 */
	protected void beforeProcess(OutputStream w) throws IOException {
	}
	
	/**
	 * 允许继承类在处理前操作输出目标
	 * @param w
	 * @param f
	 * @param target
	 */
	protected void beforeProcess(FileOutputStream w, File f, File target)throws IOException{
		beforeProcess(w);//调一下旧方法向下兼容
	}

	/**
	 * 允许继承类在处理后操作输出目标
	 * 
	 * @param w
	 * @throws IOException
	 */
	protected void afterProcess(OutputStream w) throws IOException {
	}

	/**
	 * 询问是否处理成功，如果返回false，则不修改和删除源文件，并删除临时文件
	 * 
	 * @return
	 */
	public boolean isSuccess() {
		return true;
	}

	/**
	 * 询问是否要中断处理过程
	 * 
	 * @return
	 */
	protected boolean breakProcess() {
		return false;
	}

	/**
	 * 询问是否删除源文件
	 * 
	 * @return
	 */
	protected boolean deleteSource(File source) {
		return false;
	};

	/**
	 * 询问是否替换源文件
	 * 
	 * @return
	 */
	protected boolean replaceSource(File source) {
		return false;
	}


	public abstract byte[] process(byte[] line);

	protected int getMaxBufferSize() {
		return 1024;
	}
}
