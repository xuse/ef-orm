package jef.tools.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import jef.tools.Assert;
import jef.tools.ResourceUtils;
import jef.tools.StringUtils;

/**
 * {@link Resource} implementation for <code>java.net.URL</code> locators.
 * Obviously supports resolution as URL, and also as File in case of the "file:"
 * protocol.
 * 
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.net.URL
 */
public class UrlResource extends AbstractFileResolvingResource {

	/**
	 * Original URL, used for actual access.
	 */
	private final URL url;

	/**
	 * Cleaned URL (with normalized path), used for comparisons.
	 */
	private final URL cleanedUrl;

	/**
	 * Original URI, if available; used for URI and File access.
	 */
	private final URI uri;

	/**
	 * Create a new UrlResource.
	 * 
	 * @param url
	 *            a URL
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.url = url;
		this.cleanedUrl = getCleanedUrl(this.url, url.toString());
		this.uri = null;
	}

	/**
	 * Create a new UrlResource.
	 * 
	 * @param uri
	 *            a URI
	 * @throws MalformedURLException
	 *             if the given URL path is not valid
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.url = uri.toURL();
		this.cleanedUrl = getCleanedUrl(this.url, uri.toString());
		this.uri = uri;
	}

	/**
	 * Create a new UrlResource.
	 * 
	 * @param path
	 *            a URL path
	 * @throws MalformedURLException
	 *             if the given URL path is not valid
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
		this.uri = null;
	}

	/**
	 * Determine a cleaned URL for the given original URL.
	 * 
	 * @param originalUrl
	 *            the original URL
	 * @param originalPath
	 *            the original URL path
	 * @return the cleaned URL
	 * @see org.springframework.util.StringUtils#cleanPath
	 */
	private URL getCleanedUrl(URL originalUrl, String originalPath) {
		try {
			return new URL(cleanPath(originalPath));
		} catch (MalformedURLException ex) {
			// Cleaned URL path cannot be converted to URL
			// -> take original URL.
			return originalUrl;
		}
	}

	/**
	 * This implementation opens an InputStream for the given URL. It sets the
	 * "UseCaches" flag to <code>false</code>, mainly to avoid jar file locking
	 * on Windows.
	 * 
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		ResourceUtils.useCachesIfNecessary(con);
		try {
			return con.getInputStream();
		} catch (IOException ex) {
			// Close the HTTP connection (if applicable).
			if (con instanceof HttpURLConnection) {
				((HttpURLConnection) con).disconnect();
			}
			throw ex;
		}
	}

	/**
	 * This implementation returns the underlying URL reference.
	 */
	@Override
	public URL getURL(){
		return this.url;
	}

	/**
	 * This implementation returns the underlying URI directly, if possible.
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		} else {
			return super.getURI();
		}
	}

	/**
	 * This implementation returns a File reference for the underlying URL/URI,
	 * provided that it refers to a file in the file system.
	 * 
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		} else {
			return super.getFile();
		}
	}

	/**
	 * This implementation creates a UrlResource, applying the given path
	 * relative to the path of the underlying URL of this resource descriptor.
	 * 
	 * @see java.net.URL#URL(java.net.URL, String)
	 */
	@Override
	public IResource createRelative(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return new UrlResource(new URL(this.url, relativePath));
	}

	/**
	 * This implementation returns the name of the file that this URL refers to.
	 * 
	 * @see java.net.URL#getFile()
	 * @see java.io.File#getName()
	 */
	@Override
	public String getFilename() {
		return new File(this.url.getFile()).getName();
	}

	/**
	 * This implementation returns a description that includes the URL.
	 */
	public String getDescription() {
		return "URL [" + this.url + "]";
	}

	/**
	 * This implementation compares the underlying URL references.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this || (obj instanceof UrlResource && this.cleanedUrl.equals(((UrlResource) obj).cleanedUrl)));
	}

	/**
	 * This implementation returns the hash code of the underlying URL
	 * reference.
	 */
	@Override
	public int hashCode() {
		return this.cleanedUrl.hashCode();
	}
	
	
	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";
	
	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";
	
	/**
	 * Path当中的路径进行精简和折叠。
	 * 其实这个功能一般无需自行编写，JDK中的{@link java.io.File#getCanonicalPath())就能起到相同作用。
	 * 此方法直接针对String进行处理，因此可以适用于各种非本地文件的URL的场合
	 * 
	 * Normalize the path by suppressing sequences like "path/.." and
	 * inner simple dots.
	 * <p>The result is convenient for path comparison. For other uses,
	 * notice that Windows separators ("\") are replaced by simple slashes.
	 * @param path the original path
	 * @return the normalized path
	 */
	public static String cleanPath(String path) {
		if (path == null) {
			return null;
		}
		String pathToUse = StringUtils.replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(":");
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			pathToUse = pathToUse.substring(prefixIndex + 1);
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = StringUtils.split(pathToUse, FOLDER_SEPARATOR);
		List<String> pathElements = new LinkedList<String>();
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// Points to current directory - drop it.
			}
			else if (TOP_PATH.equals(element)) {
				// Registering top path found.
				tops++;
			}
			else {
				if (tops > 0) {
					// Merging path element with element corresponding to top path.
					tops--;
				}
				else {
					// Normal path element found.
					pathElements.add(0, element);
				}
			}
		}

		// Remaining top paths need to be retained.
		for (int i = 0; i < tops; i++) {
			pathElements.add(0, TOP_PATH);
		}
		return prefix + StringUtils.join(pathElements, FOLDER_SEPARATOR);
	}

	@Override
	public boolean isFile() {
		String p=url.getProtocol();
		return p.startsWith(ResourceUtils.URL_PROTOCOL_VFS)||"file".equals(p);
	}

}