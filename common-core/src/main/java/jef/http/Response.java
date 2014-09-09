package jef.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import jef.common.MimeTypes;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

public class Response {
	private static final int BUFFER_SIZE = 4096;
	private Request request;
	private OutputStream output;

	public Response(OutputStream output2) {
		this.output = output2;
	}

	public void send404() throws IOException {
		String errorMessage = "HTTP/1.1 404 File Not Found\r\n" + "Content-Type: text/html\r\n" + "Content-Length: 23\r\n" + "\r\n" + "<h1>File Not Found</h1>";
		output.write(errorMessage.getBytes());
	}

	public void sendStaticResource(File file) throws IOException {
		this.setStatus(200);
		this.setContentType(MimeTypes.getByFileName(file.getName()));
		this.println();
		IOUtils.copy(new FileInputStream(file), output, true);
	}

	public PrintStream getPrintStream() {
		return new PrintStream(output);
	}

	public void setRequest(Request request2) {
		this.request = request2;
	}

	public void setStatus(int status) {
		try {
			output.write(("HTTP/1.1 " + status + "\r\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setHeader(String name, String value) {
		try {
			output.write((name + ": " + value + "\r\n").getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setContentType(String string) {
		try {
			output.write(("Content-Type: " + string + "\r\n").getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public OutputStream getOutput() {
		return output;
	}

	public void println() {
		try {
			output.write(StringUtils.CRLF);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void returnResource(String contentType, InputStream resourceAsStream) {
		setStatus(200);
		setContentType(contentType);
		println();
		try {
			IOUtils.copy(getClass().getResourceAsStream("/spring/datasource_lookup.json"), output, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
