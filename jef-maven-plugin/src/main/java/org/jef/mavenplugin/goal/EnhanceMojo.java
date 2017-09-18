package org.jef.mavenplugin.goal;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;

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
            if (path == null) {
                workPath = classesDirectory.getAbsolutePath();
            } else {
                workPath = path;
            }
            workPath = workPath.replace('\\', '/');
            if (!workPath.endsWith("/")) {
                workPath = workPath + "/";
            }
            this.getLog().info("Easybuilder enhance entity classes working path is: " + workPath);

            EntityEnhancer en = new EntityEnhancer().addRoot(new URL("file://" + workPath));
            en.enhance();

            this.getLog().info("Easybuilder enhance entity classes total use " + (System.currentTimeMillis() - time) + "ms");
        } catch (Exception e) {
            this.getLog().error(e);
        }
    }

    // TEST
    public static void main(String[] args) throws IOException, MojoExecutionException {
        String workPath = "E:/Git/ef-orm/orm-test/target/test-classes";
        EnhanceMojo em=new EnhanceMojo();
        em.setPath(workPath);
        em.execute();
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
