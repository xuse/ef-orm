package jef.tools.security;

import java.io.IOException;
import java.io.Writer;

public class IChannelWriter extends Writer{
	private String charset;
	private IChannel channel;
	
	public IChannelWriter(IChannel c,String encode){
		this.channel=c;
		this.charset=encode;
	}
	
	StringBuilder buff=new StringBuilder();
	
	@Override
	public synchronized void write(char[] cbuf, int off, int len) throws IOException {
		int length=buff.append(cbuf,off,len).length();
		if(length>0){
			char last=buff.charAt(length-1);
			if(last=='\n'){
				flush();
			}	
		}
	}
	
	public IChannel get(){
		return channel;
	}

	@Override
	public synchronized void flush() throws IOException {
		if(buff.length()>0){
			channel.write(buff.toString().getBytes(charset));
			buff.setLength(0);
		}
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}
}
