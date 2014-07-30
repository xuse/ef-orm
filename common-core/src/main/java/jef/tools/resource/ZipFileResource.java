package jef.tools.resource;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import jef.common.log.LogUtil;
import jef.tools.DateUtils;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

public class ZipFileResource extends FileResource {
	private File zipFile;
	
	public ZipFileResource(URL url) {
		this.url=url;
		String filename=url.toString();
		try {
			String zipName=URLDecoder.decode(StringUtils.substringAfter(StringUtils.substringBefore(filename, "!"), "file:"),"UTF-8");
			zipFile=new File(zipName);
		} catch (UnsupportedEncodingException e1) {
			//never happens.
		}
		
		filename=StringUtils.substringAfterLast(filename, "/")+"."+StringUtils.getCRC(filename);
		File tempFile=new File(System.getProperty("java.io.tmpdir"),filename);
		if(tempFile.exists() && (System.currentTimeMillis()-tempFile.lastModified()<DateUtils.TimeUnit.DAY.getMs())){
			this.file=tempFile;
		}else{
			try {
				IOUtils.saveAsFile(tempFile, url.openStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			this.file=tempFile;
			LogUtil.info("The zipped resource is saved as:"+ tempFile.getAbsolutePath());
		}
	}

	public File getZipFile() {
		return zipFile;
	}
}
