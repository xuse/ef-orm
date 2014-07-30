package org.jef.mavenplugin.goal;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal toedl
 * @phase generate-sources
 * */
public class MergeResourceMojo extends AbstractMojo {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * @parameter expression="${basedir}"
	 */
	private String path;

	/**
	 * @parameter expression="${sdl.path}"
	 */
	private String sdlPath;
	
	/**
	 * @parameter expression="${edl.module.for.sdl}"
	 */	
	private String edlModuleForSdl;

	public void execute() throws MojoExecutionException {
		long time = System.currentTimeMillis();

		File sdlDir=new File(sdlPath);
		
	}
}
