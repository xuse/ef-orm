package org.jef.mavenplugin.goal;

import java.io.File;
import java.io.IOException;

import jef.tools.IOUtils;
import jef.tools.ZipUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * gzip压缩资源文件
 * 
 * @goal compress
 * @phase prepare-package
 */
public class ResourceGzipCompressMojo extends AbstractMojo {
	private String RESOURCE_FOLDER = "/src/main/webapp/resource"; //资源文件所在目录。
	private String[] RESOURCE_SUFFIX = new String[]{"js","css"}; //需要压缩的资源文件扩展名
	
	/*
	 * 哪些目录下的资源文件打包时不需要压缩
	 * 
	 * #parameter
	 */
	//private String[] compressExcludes;
	/**
	 * 基路径
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 * @readonly
	 */
	protected String basedir;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		long startTime = System.currentTimeMillis();
		this.getLog().info("Easybuilder starts to compress resources(js/css) in resource folder......");
		//合并压缩resource包下的js、css文件
		gzipResources();
		this.getLog().info("Easybuilder compressing-resources complete. Total use "+(System.currentTimeMillis() - startTime)+"ms");
	}
	
	/**
	 * 用gzip压缩指定目录下的js css文件
	 * @throws MojoExecutionException
	 */
	private void gzipResources() throws MojoExecutionException{
		File resourceFolderFile = new File(this.basedir + RESOURCE_FOLDER);
		if(!resourceFolderFile.exists()){
			throw new MojoExecutionException("Resource folder does not exist!");
		}
		 
		File[] resFiles = IOUtils.listFilesRecursive(resourceFolderFile, RESOURCE_SUFFIX);
		long volumnSize = 10485760; //压缩卷大小，10M, 资源文件不可能会这么大 ，保证了文件压缩后不会被分拆为多个文件
		try {
			for(File resFile : resFiles){
				String path = resFile.getAbsolutePath();
				this.getLog().debug("gzip resource file: "+ path);
				int idx = path.lastIndexOf(".");
				//gzip压缩后的文件名后缀为原来的后缀前加 "gz"
				String gzSuffix = ".gz"+path.substring(idx+1);
				File targetFile = new File(path.substring(0, idx)+gzSuffix);
				ZipUtils.gzip(resFile, targetFile, volumnSize);
			}
		} catch (IOException e) {
			this.getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
