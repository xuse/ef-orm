package jef.tools.zip;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.io.CountOutputStream;

public abstract class SwitchAbleOutputStream extends CountOutputStream{
	public SwitchAbleOutputStream(OutputStream out,long size) {
		super(out);
		this.volumnSize=size;
		this.leftSize=volumnSize;
	}
	private long volumnSize;	//每卷的大小
	private int index=0;		//当前是第几卷
	//当前卷剩余的大小
	private long leftSize;
	
	public long getVolumnSize() {
		return volumnSize;
	}
	public void setVolumnSize(long volumnSize) {
		this.volumnSize = volumnSize;
	}
	public void switchNextVolumn() {
		Assert.isTrue(volumnSize>0);
		IOUtils.closeQuietly(out);
		index++;
		out=getNextVolOutputStream(index);
		super.count=0;
		this.leftSize=volumnSize;
	}
	
	public long getLeftSize() {
		return leftSize;
	}
	protected abstract OutputStream getNextVolOutputStream(int currentIndex);
	
	/**
	 * 计算目前是否需要切换分卷
	 * @return
	 */
	public boolean needChangeVolumn() {
		if(volumnSize==0)return false;
		return count>=volumnSize;
	}
	
	@Override
	public void write(int b) throws IOException {
		super.write(b);
		leftSize--;
	}
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b,0,b.length);
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(volumnSize>0 && len>(int)leftSize){
			int thisS=(int)leftSize;
			int nextS=(len-thisS);
			super.write(b, 0, thisS);
			if(needChangeVolumn()){
				this.switchNextVolumn();
			}
			super.write(b, thisS, nextS);
			leftSize-=nextS;
		}else{
			super.write(b, off, len);
			leftSize-=len;	
		}
	}
	@Override
	public void flush() throws IOException {
		super.flush();
	}
	@Override
	public void close() throws IOException {
		super.close();
	}
	
	public abstract File getFirstVolFile();
}
