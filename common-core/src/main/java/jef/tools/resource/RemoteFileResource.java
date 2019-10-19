package jef.tools.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jef.tools.IOUtils;
import jef.tools.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteFileResource extends FileResource {

	public RemoteFileResource(URL url) {
		super(url);
		String filename = url.toString();
		filename = StringUtils.substringBeforeLast(filename, "/") + "."
				+ StringUtils.getCRC(filename);
		file = new File(System.getProperty("java.io.tmpdir"), filename);
		if (file.exists()) {
			if (System.currentTimeMillis() - file.lastModified() < 86400000L) {
				log.info("The remote resource is redirect to: {}", file.getAbsolutePath());
				return;
			}
		}
		try {
			log.info("The remote resource is saved as: {}", file.getAbsolutePath());
			IOUtils.saveAsFile(file, openStream());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
