package org.jef.mavenplugin.goal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.ZipUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 打包时自动下载资源包并更新，这样业务开发就不需要再将ext、aijs等webapp/resource/目录下的资源文件上传到服务器了。
 * @goal download
 * @phase generate-resources
 * @author jianghy3 2012-9-21
 */
public class ResourceDownloadMojo extends AbstractMojo  {
	private String resourceDirectory = File.separator+"src"+File.separator+"main"+File.separator+"webapp";
	private String remoteResourceDirectoryURL;
	
	/**
	 * 项目根目录
	 * 
	 * @parameter expression="${basedir}"
	 */
	private String basedir;
	
	/**
	 * 远程资源服务器IP
	 * 
	 * @parameter expression="${configServerIP}"
	 */
	private String configServerIP;
	
	/**
	 *  工程依赖的profile版本号
	 *  
	 *  @parameter expression="${profile}"
	 */
	private String profile;
	
		
	public void execute() throws MojoExecutionException, MojoFailureException {
		//向前兼容，若pom中指定了资源服务器地址和profile版本号则用指定的，否则用工程gen.conf文件的配置。
		//建议：为了和工程profile保持一致，不要指定profile，使用工程gen.conf文件即可。
		if(profile == null){
			usePrjProfile();
		}
		
		remoteResourceDirectoryURL =  "http://"+ configServerIP +"/easyframe/resource/";
		
		String[] versionStack = getRemoteResourceVersionStack(); 
		String unzipPath = basedir + resourceDirectory;
		
		try {
			for (String aVersion:versionStack){
				String encodeVersion = StringUtils.urlEncode(aVersion);
				String url=remoteResourceDirectoryURL + encodeVersion;
				URL u=new URL(url);
				this.getLog().info("下载资源包：" + url);
				File file=IOUtils.saveAsTempFile(u.openStream());
				this.getLog().info("正在解压资源["+ encodeVersion +"]到：" + unzipPath);
				ZipUtils.unzip(file, unzipPath,null, null);//解压到/src/main/webapp下
			}
			this.getLog().info("资源解压完成。");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	
	/**
	 * 从gen.conf文件中得到serverIp和profile
	 */
	private void usePrjProfile() throws MojoExecutionException{
		File genConf = new File(basedir, "gen.conf");
		if(!genConf.exists()){
			throw new MojoExecutionException("The gen.conf file cann't be found.Please Check Resource Update first.");
		}
		
		String[] genConfStrs = null;
		try {
			genConfStrs = IOUtils.readLines(genConf, "UTF-8",null);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (genConfStrs==null || genConfStrs.length<2){
			throw new MojoExecutionException("Content in gen.conf is not correct");
		}
		this.configServerIP=genConfStrs[0].trim();
		this.profile=genConfStrs[1].trim();
	}
	
	private String[] getRemoteResourceVersionStack() throws MojoExecutionException{
		List<String> profileStack=getProfileStack(profile);
		String[] versionStack=new String[profileStack.size()];
		for (int i=0;i<profileStack.size();i++){
			versionStack[i]=getResourceVersion(profileStack.get(i));
		}
		return versionStack;
	}
	
	private List<String> getProfileStack(String theProfile){
		List<String> profileStack=new ArrayList<String>();
		int pos=0;
		while(true){
			pos=theProfile.indexOf("--)", pos+1);
			if (pos>0)
				profileStack.add(theProfile.substring(0, pos));
			else{
				profileStack.add(theProfile);
				break;
			}
		}
		return profileStack;
	}

	private String getResourceVersion(String theProfile) throws MojoExecutionException{
		String indexFile = remoteResourceDirectoryURL + StringUtils.urlEncode(theProfile)+".txt";
		this.getLog().info("profile "+theProfile+" url: "+ indexFile);

		String version;
		try {
			URL url = new URL(indexFile);
			BufferedReader reader=IOUtils.getReader((InputStream)url.getContent(), "US-ASCII");
			version = reader.readLine();
			this.getLog().info("检测："+indexFile+"的最新配置包为:"+version);
			
			return version;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	//getter & setter
	public String getBasedir() {
		return basedir;
	}

	public void setBasedir(String basedir) {
		this.basedir = basedir;
	}

	public String getConfigServerIP() {
		return configServerIP;
	}

	public void setConfigServerIP(String configServerIP) {
		this.configServerIP = configServerIP;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}
}
