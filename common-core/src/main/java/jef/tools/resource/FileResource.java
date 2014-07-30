package jef.tools.resource;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jef.tools.IOUtils;

public class FileResource extends Resource implements IResource {
	protected File file;
	
	protected FileResource(){
		super(null);
	} 
	
	public FileResource(URL url) {
		super(url);
		try {
			file=new File(url.toURI());
		} catch (URISyntaxException e) {
			System.out.println(url);
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			System.out.println(url);
			throw e;
		}
	}
	
	
	public FileResource(File file2) {
		super(null);
		this.file=file2;
		try {
			super.url=file2.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}


	public File getFile(){
		return file;
	}
	
	private boolean open;
	
	
	@Override
	public InputStream openStream(){
		try {
			open=true;
			return new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		return IOUtils.getWriter(file, charset,false);
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public boolean isReadable() {
		return file.exists();
	}

	public InputStream getInputStream() throws IOException {
		return this.openStream();
	}

	public boolean exists() {
		return file.exists();
	}

	public boolean isOpen() {
		return open;
	}

	public URI getURI() throws IOException {
		try {
			return url.toURI();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public long contentLength() throws IOException {
		return file.length();
	}

	public long lastModified() throws IOException {
		return file.lastModified();
	}

	public IResource createRelative(String relativePath) throws IOException {
		return new FileResource(new File(file,relativePath));
	}

	public String getFilename() {
		return file.getName();
	}

	public String getDescription() {
		return "file [" + file.getAbsolutePath() + "]";
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
