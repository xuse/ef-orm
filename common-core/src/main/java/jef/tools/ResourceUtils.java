package jef.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jef.common.log.LogUtil;
import jef.tools.reflect.ClassUtils;
import jef.tools.resource.ClassRelativeLoader;
import jef.tools.resource.ClasspathLoader;
import jef.tools.resource.CompsiteLoader;
import jef.tools.resource.FileResource;
import jef.tools.resource.FileSearchLoader;
import jef.tools.resource.IResource;
import jef.tools.resource.PathMatchingResourcePatternResolver;
import jef.tools.resource.Resource;
import jef.tools.resource.ResourceLoader;
import jef.tools.resource.ResourcePatternResolver;
import jef.tools.resource.UrlResourceLoader;
import jef.tools.resource.UrlResourceLoader.EnvURL;

/**
 * 资源定位和加载工具，原先在IOUtils当中，后来重新对资源定位模型的功能需求和API进行了设计。从IOUtils中独立出来。
 * 新的统一资源定位将重新分析资源定位的需求。然后重新抽象出资源加载策略来实现。满足 任意协议/任意资源/策略自由组合/任意环境等复杂条件，
 * 成为放置四海皆便利的最佳实践。
 * 
 * 总结：资源定位加载无非以下几种场景
 * 
 * 一、classpath类： 1.1: 全classpath搜索，搜到哪个在前面就是哪个 1.2 全classpath搜索，搜到匹配的资源全部返回 1.3
 * 某个指定类的classpath,仅搜索该类文件所在的classpath。（一般为该类文件的相同目录下，但应该可以搜索到该类所在cp的根路径下）
 * 二、web类 即web工程的主目录，或者WEB-INF目录。注意虽然大部分web容器都将classes,lib定位于WEB-INF目录下，
 * 但实际上Java规范无此约定。 因此不能假定classpath上溯可以得到WEB-INF目录，也不能假定WEB-INF下面还有classes和lib目录。
 * 
 * 三、运行目录类
 * 
 * 
 * 四、虚拟机参数、环境变量类
 * 
 * 
 * 五、osgi bundle类 之前在开发Eclipse插件时做过特殊处理支持，目前看来还是不合适。此版本开始暂不支持，今后再说
 * 
 * 六、上述一种或多种的混合。
 * 
 * 
 * 
 * 
 * -------------------------- 考虑需要满足的复杂资源定位场景 1、在Eclipse中作为常规工程
 * 2、发布为一个可执行的jar(fatjar) 3. 多个jar在一个目录中的可执行程序 4.
 * 在eclipse中被依赖的工程和当前工程下都有的资源。能根据需要定位 5. 在Tomcat中作为一个war 6.
 * 在WL中作为一个war(war包直接发布) 7. 在WL中作为一个发布目录（war包目录发布） 8. 在EJB中作为一个公共库 9
 * 在WL中为一个EJB模块 --------------------------
 * JEF早期版本在设计时，考虑到classpath的特殊性（CP有目录和压缩文件两种存储方式，而后者很难支持资源文件修改），
 * 因此设计了向上级路径查找的规则，虽然在一定范围内取得了实用效果，但终究是非主流用法。
 * 从这个版本开始不再兼容。如果用户要确保资源定位必须可写，那么采用后几种方式来设计资源位置比较好。
 * 
 * 
 * 我们在资源定位设计中常碰到的问题 1/
 * 资源冲突。尤其是使用全局classpath定位的时候，这让Asiainfo的开发模式下EasyFrame的故障分析成为一个噩梦。 2/ 资源路径计算错误。
 * 
 * -----
 * 
 * 设计： 新的资源加载/存储API将结合设计模式实现更灵活的加载策略。
 * 
 * @author jiyi
 * 
 */
public class ResourceUtils {

	/** Pseudo URL prefix for loading from the class path: "classpath:" */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/** URL prefix for loading from the file system: "file:" */
	public static final String FILE_URL_PREFIX = "file:";

	/** URL protocol for a file in the file system: "file" */
	public static final String URL_PROTOCOL_FILE = "file";

	/** URL protocol for an entry from a jar file: "jar" */
	public static final String URL_PROTOCOL_JAR = "jar";

	/** URL protocol for an entry from a zip file: "zip" */
	public static final String URL_PROTOCOL_ZIP = "zip";

	/** URL protocol for an entry from a JBoss jar file: "vfszip" */
	public static final String URL_PROTOCOL_VFSZIP = "vfszip";

	/** URL protocol for a JBoss VFS resource: "vfs" */
	public static final String URL_PROTOCOL_VFS = "vfs";

	/** URL protocol for an entry from a WebSphere jar file: "wsjar" */
	public static final String URL_PROTOCOL_WSJAR = "wsjar";

	/** URL protocol for an entry from an OC4J jar file: "code-source" */
	public static final String URL_PROTOCOL_CODE_SOURCE = "code-source";

	/** Separator between JAR URL and file path within the JAR */
	public static final String JAR_URL_SEPARATOR = "!/";

	private static final ResourceLoader LOADER_DefaultCp = new ClasspathLoader();
	private static final ResourceLoader LOADER_DirOnlyCp = new ClasspathLoader(true);
	private static final CompsiteLoader LOADER_DIR_FIRST = new CompsiteLoader(LOADER_DirOnlyCp, LOADER_DefaultCp).setFirstCollection(false);
	private static final ResourceLoader LOADER_USERDIR = new UrlResourceLoader(EnvURL.USER_DIR);
	private static final ResourceLoader LOADER_USERHOME = new UrlResourceLoader(EnvURL.USER_HOME);
	private static final ResourceLoader LOADER_SYS = new UrlResourceLoader(EnvURL.PATH, EnvURL.JAVA_LIBRARY_PATH);
	private static final ResourceLoader Loader_searchUserDir = new FileSearchLoader(new File(System.getProperty("user.dir")));

	/**
	 * 在指定的class的ClassLoader的载入路径上提取资源
	 * 
	 * <pre>
	 * 解释：这个操作只会在指定的class所属的classloader拥有的路径上查找资源。
	 * 在一个应用环境中，可能会有多级的classloader，使用这个操作相当于指定了要使用的classloader。
	 * 除此之外，这个操作还有一个特点，也就是说总是会优先查找指定class所在的目录或jar包。这一点在多个路径上都有同名资源时尤其有用。
	 * </pre>
	 * 
	 * @param name
	 * @return
	 */
	public static URL getResource(Class<?> c, String name) {
		return getResource(c, name, false);
	}

	/**
	 * 在指定的class的ClassLoader的载入路径上提取资源,返回Resource对象<br>
	 * 参考{@link #getResource}
	 * 
	 * @param c
	 * @param name
	 * @return
	 */
	public static Resource getResourceEx(Class<?> c, String name) {
		return Resource.getResource(getResource(c, name));
	}

	/**
	 * 获得相对于某个class同路径下的资源，可使用相对路径
	 * 
	 * @param c
	 * @param name
	 * @return
	 */
	public static Resource getClassResource(Class<?> c, String name) {
		ClassRelativeLoader loader = new ClassRelativeLoader(c);
		return loader.getResourceEx(name);
	}

	/**
	 * 得到程序运行目录下的资源 由于这种场合都用于获取文件资源，所以直接返回file对象。
	 * 
	 * @param name
	 * @return
	 */
	public static File getDirResource(String name) {
		FileResource file = Resource.getFileResource(LOADER_USERDIR.getResource(name));
		return file == null ? null : file.getFile();
	}

	/**
	 * 得到用户目录下（不是程序运行目录）下的资源 由于这种场合都用于获取文件资源，所以直接返回file对象。
	 * 
	 * @param name
	 * @return
	 */
	public static File getUserHomeResource(String name) {
		FileResource file = Resource.getFileResource(LOADER_USERHOME.getResource(name));
		return file == null ? null : file.getFile();
	}

	/**
	 * 在系统（操作系统）的path和程序的classpath所有目录下获取资源
	 * 
	 * @param name
	 * @return
	 */
	public static Resource getSystemResource(String name) {
		return LOADER_SYS.getResourceEx(name);
	}

	/**
	 * 得到程序运行目录下的资源 搜索获取
	 * 
	 * @param name
	 * @return
	 */
	public static URL getDirResourceHierarchy(String name) {
		return Loader_searchUserDir.getResource(name);
	}

	/**
	 * 在指定的class的loader的相对路径查找资源。 资源不要带"/"前缀，默认都是从根路径查找
	 * 
	 * @param c
	 *            类文件
	 * @param name
	 *            路径
	 * @param dirOnly
	 *            如果此选项设置为true,则只从目录中寻找文件
	 * @return 资源文件
	 */
	public static URL getResource(Class<?> c, String name, boolean dirOnly) {
		if (c == null)
			return getResource(name, dirOnly);
		name = name.replace('\\', '/');
		CompsiteLoader loader = new CompsiteLoader();
		if (dirOnly) {
			loader.addResourceLoader(new UrlResourceLoader(c, true));
			loader.addResourceLoader(new ClasspathLoader(true, c.getClassLoader()));
		} else {
			loader.addResourceLoader(new UrlResourceLoader(c)); // 优先指定路径
			loader.addResourceLoader(new ClasspathLoader(true, c.getClassLoader()));// 其次目录
			loader.addResourceLoader(new ClasspathLoader(false, c.getClassLoader()));// 再次全部
		}
		return loader.getResource(name);
	}

	/**
	 * 无论资源以什么形态存在，总是以文件形式返回
	 * 
	 * @param name
	 * @return
	 */
	public static File getResourceFile(String name) {
		FileResource fs = Resource.getFileResource(getResource(name));
		return fs == null ? null : fs.getFile();
	}

	/**
	 * 获得资源<br>
	 * 相当于调用 getResource(name,false, new ClassLoader[0]);<br>
	 * 参考{@link #getResource(String, boolean, ClassLoader...)}
	 * 
	 * @param name
	 * @return
	 */
	public static URL getResource(String name) {
		return getResource(name, false);
	}

	/**
	 * 获得全部资源 相当于调用 getResources(name,false, new ClassLoader[0]);<br>
	 * 参考{@link #getResources(String, boolean, ClassLoader...)}
	 * 
	 * @param name
	 * @return
	 */
	public static List<URL> getResources(String name) {
		return getResources(name, false);
	}

	/**
	 * 将找到的资源封装为resource对象 参考{@link #getResource(String)}
	 * 
	 * @param name
	 * @return
	 */
	public static Resource getResourceEx(String name) {
		return Resource.getResource(getResource(name, false));
	}

	/**
	 * 在指定的路径上查找资源文件 从生产实践讲，放在目录下的文件由于便于维护，比放在JAR包中的文件享有绝对优先权。
	 * 
	 * @param name
	 *            资源名称，资源名称可以采用下列格式之一
	 *            <table width=90% border=1>
	 *            <tr>
	 *            <td>resource-path/name</td>
	 *            <td width="80%">即classpath下的resource-path/name资源</td>
	 *            </tr>
	 *            <tr>
	 *            <td>/resource-path/name</td>
	 *            <td>第一个斜杠会自动被忽略，结果同上(请避免这样用)</td>
	 *            </tr>
	 *            <tr>
	 *            <td>classpath*:resource-path/name</td>
	 *            <td>“classpath*:”会被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>classpath:resource-path/name</td>
	 *            <td>“classpath:” 会被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>file:resource-path/name</td>
	 *            <td>会在当前程序运行目录下寻找资源</td>
	 *            </tr>
	 * 
	 *            <tr>
	 *            <td>file:/resource-path/name</td>
	 *            <td>第一个斜杠会自动被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>file://c:/resource-path/name</td>
	 *            <td>第一个斜杠被忽略，第二个斜杠表明这是绝对路径，按绝对路径查找文件</td>
	 *            </tr>
	 *            <tr>
	 *            <td>http://localhost:80/context/name</td>
	 *            <td>直接以这个URL作为资源</td>
	 *            </tr>
	 *            <tr>
	 *            <td>ftp://user:password@host:21/name</td>
	 *            <td>直接以这个URL作为资源</td>
	 *            </tr>
	 *            </table>
	 * @param dironly
	 *            为true只查找目录中的资源，放弃jar包或zip包的资源，以及http等网络资源(仅当查找classpath类资源有效)
	 * @param loaders
	 *            指定类加载器，将使用这些类加载器来查找资源 (仅当查找classpath类资源有效)
	 * @return
	 */
	public final static URL getResource(String name, boolean dironly, ClassLoader... loaders) {
		if (name.startsWith("classpath*:")) {
			name = name.substring(11);
		} else if (name.startsWith("classpath:")) {
			name = name.substring(10);
		} else if (name.startsWith("file:")) {
			name = name.substring(5);
			URL u = LOADER_USERDIR.getResource(name);
			if (u != null) {
				return u;
			} else {
				return null;
			}
		} else if (name.indexOf("://") > -1) {
			try {
				return new URL(name);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(name + " is invalid! " + e.getMessage());
			}
		}
		if (loaders.length == 0) {
			return dironly ? LOADER_DirOnlyCp.getResource(name) : LOADER_DIR_FIRST.getResource(name);
		}
		ResourceLoader loader = new ClasspathLoader(true, loaders);
		if (dironly)
			return loader.getResource(name);
		loader = new CompsiteLoader(loader, new ClasspathLoader(false, loaders));
		return loader.getResource(name);
	}

	/**
	 * 在指定的路径上查找所有入选资源文件
	 * 
	 * @param name
	 *            <table width=90% border=1>
	 *            <tr>
	 *            <td>resource-path/name</td>
	 *            <td width="80%">即classpath下的resource-path/name资源</td>
	 *            </tr>
	 *            <tr>
	 *            <td>/resource-path/name</td>
	 *            <td>第一个斜杠会自动被忽略，结果同上(请避免这样用)</td>
	 *            </tr>
	 *            <tr>
	 *            <td>classpath*:resource-path/name</td>
	 *            <td>“classpath*:”会被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>classpath:resource-path/name</td>
	 *            <td>“classpath:” 会被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>file:resource-path/name</td>
	 *            <td>会在当前程序运行目录下寻找资源</td>
	 *            </tr>
	 * 
	 *            <tr>
	 *            <td>file:/resource-path/name</td>
	 *            <td>第一个斜杠会自动被忽略，结果同上</td>
	 *            </tr>
	 *            <tr>
	 *            <td>file://c:/resource-path/name</td>
	 *            <td>第一个斜杠被忽略，第二个斜杠表明这是绝对路径，按绝对路径查找文件</td>
	 *            </tr>
	 *            <tr>
	 *            <td>http://localhost:80/context/name</td>
	 *            <td>直接以这个URL作为资源</td>
	 *            </tr>
	 *            <tr>
	 *            <td>ftp://user:password@host:21/name</td>
	 *            <td>直接以这个URL作为资源</td>
	 *            </tr>
	 *            </table>
	 * @param dironly
	 *            为true只查找目录中的资源，放弃jar包或zip包的资源，以及http等网络资源(仅当查找classpath类资源有效)
	 * @param loaders
	 *            指定类加载器，将使用这些类加载器来查找资源 (仅当查找classpath类资源有效)
	 * @return
	 */
	public final static List<URL> getResources(String name, boolean dironly, ClassLoader... loaders) {
		if (name.startsWith("classpath*:")) {
			name = name.substring(11);
		} else if (name.startsWith("classpath:")) {
			name = name.substring(10);
		} else if (name.startsWith("file:")) {
			name = name.substring(5);
			URL u = LOADER_USERDIR.getResource(name);
			if (u != null) {
				return Arrays.asList(u);
			} else {
				return Arrays.asList();
			}
		} else if (name.indexOf("://") > -1) {
			try {
				URL u = new URL(name);
				return Arrays.asList(u);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(name + " is invalid! " + e.getMessage());
			}
		}
		if (loaders.length == 0) {
			return dironly ? LOADER_DirOnlyCp.getResources(name) : LOADER_DefaultCp.getResources(name);
		}
		ResourceLoader loader = new ClasspathLoader(dironly, loaders);
		return loader.getResources(name);
	}

	/**
	 * 将指定文件的资源加载为Properties。注意会查找所有classpath上的多个资源，全部加载。
	 * 如果资源文件中的相同的项目，后加载的会覆盖先加载的。
	 * 
	 * @param name
	 * @param charset
	 * @return
	 */
	public final static Map<String, String> loadAsProperties(String name, String charset) {
		List<URL> res = getResources(name);
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (URL u : res) {
			IOUtils.loadProperties(IOUtils.getReader(u, charset), result,false);
		}
		return result;
	}

	/**
	 * 将本地文件转化为URL
	 */
	public static URL fileToURL(File f) {
		if (f instanceof URLFile) {
			try {
				return ((URLFile) f).toURL();
			} catch (MalformedURLException e) {
				LogUtil.exception(e);
			}
		}
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public static InputStream asStream(String string) {
		Resource res = getResourceEx(string);
		if (res != null)
			return res.openStream();
		return null;
	}
	
	/**
	 * 查找符合Pattern的所有资源
	 * @param locationPattern
	 * @return
	 * @throws IOException
	 */
	public static IResource[] findResources(String locationPattern) {
		ResourcePatternResolver rl= new PathMatchingResourcePatternResolver();
		try {
			return rl.getResources(locationPattern);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	

	/**
	 * 查找符合Pattern的所有资源
	 * @param locationPattern
	 * @return
	 * @throws IOException
	 */
	public static IResource[] findResources(ClassLoader cl,String locationPattern,boolean excludeInnerClass) {
		ResourcePatternResolver rl= new PathMatchingResourcePatternResolver(cl);
		try {
		    IResource[] res= rl.getResources(locationPattern);
		    if(excludeInnerClass){
		        List<IResource> list=ArrayUtils.filter(res, new Predicate<IResource>(){
                    public boolean test(IResource o) {
                        String s=o.getFilename();
                        int dollor=s.indexOf('$');
                        return dollor<=0; //当$位于第一个字符时，不认为是内部类
                    }
		        });
		        res=list.toArray(new IResource[list.size()]);
		    }
			return res;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * 
	 * @param string
	 * @return
	 */
	public static String asString(String string) {
		Resource res = getResourceEx(string);
		if (res != null)
			return res.loadAsString();
		return null;
	}

	/**
	 * 按Class相同路径的方式查找资源
	 * 
	 * @param clz
	 * @param string
	 * @return
	 */
	public static String asString(Class<?> clz, String string) {
		Resource res = getClassResource(clz, string);
		if (res != null)
			return res.loadAsString();
		return null;
	}

	/**
	 * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the given
	 * connection, preferring {@code false} but leaving the flag at {@code true}
	 * for JNLP based resources.
	 * 
	 * @param con
	 *            the URLConnection to set the flag on
	 */
	public static void useCachesIfNecessary(URLConnection con) {
		con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
	}

	/**
	 * Return whether the given resource location is a URL: either a special
	 * "classpath" pseudo URL or a standard URL.
	 * 
	 * @param resourceLocation
	 *            the location String to check
	 * @return whether the location qualifies as a URL
	 * @see #CLASSPATH_URL_PREFIX
	 * @see java.net.URL
	 */
	public static boolean isUrl(String resourceLocation) {
		if (resourceLocation == null) {
			return false;
		}
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			return true;
		}
		try {
			new URL(resourceLocation);
			return true;
		} catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * Extract the URL for the actual jar file from the given URL (which may
	 * point to a resource in a jar file or to a jar file itself).
	 * 
	 * @param jarUrl
	 *            the original URL
	 * @return the URL for the actual jar file
	 * @throws MalformedURLException
	 *             if no valid jar file URL could be extracted
	 */
	public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();
		int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
		if (separatorIndex != -1) {
			String jarFile = urlFile.substring(0, separatorIndex);
			try {
				return new URL(jarFile);
			} catch (MalformedURLException ex) {
				// Probably no protocol in original jar URL, like
				// "jar:C:/mypath/myjar.jar".
				// This usually indicates that the jar file resides in the file
				// system.
				if (!jarFile.startsWith("/")) {
					jarFile = "/" + jarFile;
				}
				return new URL(FILE_URL_PREFIX + jarFile);
			}
		} else {
			return jarUrl;
		}
	}

	/**
	 * Determine whether the given URL points to a resource in a jar file, that
	 * is, has protocol "jar", "zip", "wsjar" or "code-source".
	 * <p>
	 * "zip" and "wsjar" are used by BEA WebLogic Server and IBM WebSphere,
	 * respectively, but can be treated like jar files. The same applies to
	 * "code-source" URLs on Oracle OC4J, provided that the path contains a jar
	 * separator.
	 * 
	 * @param url
	 *            the URL to check
	 * @return whether the URL has been identified as a JAR URL
	 */
	public static boolean isJarURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_JAR.equals(protocol) || URL_PROTOCOL_ZIP.equals(protocol) || URL_PROTOCOL_WSJAR.equals(protocol) || (URL_PROTOCOL_CODE_SOURCE.equals(protocol) && url.getPath().contains(JAR_URL_SEPARATOR)));
	}

	/**
	 * Create a URI instance for the given URL, replacing spaces with "%20"
	 * quotes first.
	 * <p>
	 * Furthermore, this method works on JDK 1.4 as well, in contrast to the
	 * {@code URL.toURI()} method.
	 * 
	 * @param url
	 *            the URL to convert into a URI instance
	 * @return the URI instance
	 * @throws URISyntaxException
	 *             if the URL wasn't a valid URI
	 * @see java.net.URL#toURI()
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		return toURI(url.toString());
	}

	/**
	 * Create a URI instance for the given location String, replacing spaces
	 * with "%20" quotes first.
	 * 
	 * @param location
	 *            the location String to convert into a URI instance
	 * @return the URI instance
	 * @throws URISyntaxException
	 *             if the location wasn't a valid URI
	 */
	public static URI toURI(String location) throws URISyntaxException {
		return new URI(StringUtils.replace(location, " ", "%20"));
	}

	/**
	 * Resolve the given resource location to a {@code java.io.File}, i.e. to a
	 * file in the file system.
	 * <p>
	 * Does not check whether the fil actually exists; simply returns the File
	 * that the given location would correspond to.
	 * 
	 * @param resourceLocation
	 *            the resource location to resolve: either a "classpath:" pseudo
	 *            URL, a "file:" URL, or a plain file path
	 * @return a corresponding File object
	 * @throws FileNotFoundException
	 *             if the resource cannot be resolved to a file in the file
	 *             system
	 */
	public static File getFile(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			String description = "class path resource [" + path + "]";
			URL url = ClassUtils.getDefaultClassLoader().getResource(path);
			if (url == null) {
				throw new FileNotFoundException(description + " cannot be resolved to absolute file path " + "because it does not reside in the file system");
			}
			return getFile(url, description);
		}
		try {
			// try URL
			return getFile(new URL(resourceLocation));
		} catch (MalformedURLException ex) {
			// no URL -> treat as file path
			return new File(resourceLocation);
		}
	}

	/**
	 * Resolve the given resource URL to a {@code java.io.File}, i.e. to a file
	 * in the file system.
	 * 
	 * @param resourceUrl
	 *            the resource URL to resolve
	 * @return a corresponding File object
	 * @throws FileNotFoundException
	 *             if the URL cannot be resolved to a file in the file system
	 */
	public static File getFile(URL resourceUrl) throws FileNotFoundException {
		return getFile(resourceUrl, "URL");
	}

	/**
	 * Resolve the given resource URL to a {@code java.io.File}, i.e. to a file
	 * in the file system.
	 * 
	 * @param resourceUrl
	 *            the resource URL to resolve
	 * @param description
	 *            a description of the original resource that the URL was
	 *            created for (for example, a class path location)
	 * @return a corresponding File object
	 * @throws FileNotFoundException
	 *             if the URL cannot be resolved to a file in the file system
	 */
	public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
		Assert.notNull(resourceUrl, "Resource URL must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
			throw new FileNotFoundException(description + " cannot be resolved to absolute file path " + "because it does not reside in the file system: " + resourceUrl);
		}
		try {
			return new File(toURI(resourceUrl).getSchemeSpecificPart());
		} catch (URISyntaxException ex) {
			// Fallback for URLs that are not valid URIs (should hardly ever
			// happen).
			return new File(resourceUrl.getFile());
		}
	}

	/**
	 * Resolve the given resource URI to a {@code java.io.File}, i.e. to a file
	 * in the file system.
	 * 
	 * @param resourceUri
	 *            the resource URI to resolve
	 * @return a corresponding File object
	 * @throws FileNotFoundException
	 *             if the URL cannot be resolved to a file in the file system
	 */
	public static File getFile(URI resourceUri) throws FileNotFoundException {
		return getFile(resourceUri, "URI");
	}

	/**
	 * Resolve the given resource URI to a {@code java.io.File}, i.e. to a file
	 * in the file system.
	 * 
	 * @param resourceUri
	 *            the resource URI to resolve
	 * @param description
	 *            a description of the original resource that the URI was
	 *            created for (for example, a class path location)
	 * @return a corresponding File object
	 * @throws FileNotFoundException
	 *             if the URL cannot be resolved to a file in the file system
	 */
	public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
		Assert.notNull(resourceUri, "Resource URI must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
			throw new FileNotFoundException(description + " cannot be resolved to absolute file path " + "because it does not reside in the file system: " + resourceUri);
		}
		return new File(resourceUri.getSchemeSpecificPart());
	}

	/**
	 * Determine whether the given URL points to a resource in the file system,
	 * that is, has protocol "file" or "vfs".
	 * 
	 * @param url
	 *            the URL to check
	 * @return whether the URL has been identified as a file system URL
	 */
	public static boolean isFileURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_FILE.equals(protocol) || protocol.startsWith(URL_PROTOCOL_VFS));
	}

}
