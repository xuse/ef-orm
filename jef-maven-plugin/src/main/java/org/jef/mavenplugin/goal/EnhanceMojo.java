package org.jef.mavenplugin.goal;

import java.io.File;

import jef.codegen.EntityEnhancer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal enhance
 * @phase process-classes
 */
public class EnhanceMojo extends AbstractMojo {
	/**
	 * @parameter expression="${enhance.path}"
	 */
	private String path;

	/**
	 * The directory containing generated classes.
	 * 
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 * 
	 */
	private File classesDirectory;

	/**
	 * 基路径
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 * @readonly
	 */
	protected String basedir;
	
	public void setPath(String path) {
		this.path = path;
	}

	public void execute() throws MojoExecutionException {
		long time = System.currentTimeMillis();
		try {
			this.getLog().info("Easybuilder enhanceing entity classes......");

			String workPath;
			if (path == null)
				workPath = classesDirectory.getAbsolutePath();
			else
				workPath = path;
			
		
			
			workPath = workPath.replace('\\', '/');
			this.getLog().info("Easybuilder enhance entity classes working path is: " + workPath);

			EntityEnhancer en = new EntityEnhancer();

			en.enhance();

			this.getLog().info("Easybuilder enhance entity classes total use " + (System.currentTimeMillis() - time) + "ms");
		} catch (Exception e) {
			this.getLog().error(e);
		}
	}

	public String getPath() {
		return path;
	}

	public File getClassesDirectory() {
		return classesDirectory;
	}
	
	public String getBasedir() {
		return basedir;
	}

}
