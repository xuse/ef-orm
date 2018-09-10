package jef.tools.resource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import jef.tools.IOUtils;

public abstract class Resource{
	public abstract BufferedWriter getWriter() throws IOException;

	public static FileResource DUMMY;
	static {
		try {
			URL u = new URL("file:///void");
			DUMMY = new DummyResource(u);
		} catch (MalformedURLException e) {
			throw new UnsupportedOperationException();
		}
	}

	public abstract boolean isWritable();

	protected URL url;
	protected Charset charset;

	public Resource(URL url) {
		this.url = url;
	}

	public boolean isReadable() {
		return true;
	}

	public static FileResource getFileResource(URL url) {
		if (url == null)
			return DUMMY;
		String p = url.getProtocol();
		if ("file".equals(p)) {
			return new FileResource(url);
		} else if ("jar".equals(p) || "zip".equals(p)) {
			return new ZipFileResource(url);
		} else {
			return new RemoteFileResource(url);
		}
	}

	public static Resource getResource(URL url) {
		if (url == null)
			return DUMMY;
		String p = url.getProtocol();
		if ("file".equals(p)) {
			return new FileResource(url);
		} else if ("jar".equals(p) || "zip".equals(p)) {
			return new ZipResource(url);
		} else {
			return new RemoteResource(url);
		}
	}

	public Resource setCharset(Charset charset) {
		this.charset = charset;
		return this;
	}

	public URL unwrap() {
		return url;
	}

	public String getUrl() {
		return url.toString();
	}
	
	public URL getURL() {
		return url;
	}

	public String getName() {
		return url.getFile();
	}

	public InputStream openStream() {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedReader openReader() {
		return IOUtils.getReader(openStream(), charset.name());
	}

	public String loadAsString() {
		try {
			return IOUtils.asString(openStream(), charset.name(), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public String loadAsString(String charset) {
		try {
			return IOUtils.asString(openStream(), charset, true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, String> loadAsProperties() throws IOException {
		return IOUtils.loadProperties(openReader());
	}

	static class ZipResource extends Resource {
		public ZipResource(URL url) {
			super(url);
		}

		public BufferedWriter getWriter() {
			throw new UnsupportedOperationException();
		}

		public boolean isWritable() {
			return false;
		}
	}

	static class RemoteResource extends Resource {

		public RemoteResource(URL url) {
			super(url);
		}

		@Override
		public BufferedWriter getWriter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}
}

class DummyResource extends FileResource {
	DummyResource(URL url) {
		super(url);
		super.url = null;
		super.file = null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public InputStream openStream() {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public String loadAsString() {
		return "";
	}

	@Override
	public String loadAsString(String charset) {
		return "";
	}

	@Override
	public Map<String, String> loadAsProperties() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

}
