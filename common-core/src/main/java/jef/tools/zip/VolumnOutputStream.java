package jef.tools.zip;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class VolumnOutputStream extends OutputStream{
	private SwitchAbleOutputStream switcher;
	private long total=0;
	
	public VolumnOutputStream(SwitchAbleOutputStream switcher){
		this.switcher=switcher;
	}
	@Override
	public void write(int b) throws IOException {
		if(switcher.needChangeVolumn()){
			switcher.switchNextVolumn();
		}
		switcher.write(b);
		total++;
	}
	@Override
	public void write(byte[] b) throws IOException {
		if(switcher.needChangeVolumn()){
			switcher.switchNextVolumn();
		}
		switcher.write(b);
		total+=b.length;
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(switcher.needChangeVolumn()){
			switcher.switchNextVolumn();
		}
		switcher.write(b,off,len);
		total+=len;
	}
	@Override
	public void flush() throws IOException {
		switcher.flush();
	}
	@Override
	public void close() throws IOException {
		switcher.flush();
		switcher.close();
	}
	public File getFirstVolFile() {
		return switcher.getFirstVolFile();
	}
	public long getTotal() {
		return total;
	}
}
