package jef.tools.resource;

import java.io.IOException;


public interface ResourcePatternResolver {

	/**
	 * Pseudo URL prefix for all matching resources from the class path:
	 * "classpath*:" This differs from ResourceLoader's classpath URL prefix in
	 * that it retrieves all matching resources for a given name (e.g.
	 * "/beans.xml"), for example in the root of all deployed JAR files.
	 * 
	 * @see jef.tools.resource.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * Resolve the given location pattern into Resource objects.
	 * <p>
	 * Overlapping resource entries that point to the same physical resource
	 * should be avoided, as far as possible. The result should have set
	 * semantics.
	 * 
	 * @param locationPattern
	 *            the location pattern to resolve
	 * @return the corresponding Resource objects
	 * @throws IOException
	 *             in case of I/O errors
	 */
	IResource[] getResources(String locationPattern) throws IOException;

	/**
	 * Return a Resource handle for the specified resource. The handle should
	 * always be a reusable resource descriptor, allowing for multiple
	 * {@link Resource#getInputStream()} calls.
	 * <p>
	 * <ul>
	 * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
	 * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
	 * <li>Should support relative file paths, e.g. "WEB-INF/test.dat". (This
	 * will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)
	 * </ul>
	 * <p>
	 * Note that a Resource handle does not imply an existing resource; you need
	 * to invoke {@link Resource#exists} to check for existence.
	 * 
	 * @param location
	 *            the resource location
	 * @return a corresponding Resource handle
	 * @see #CLASSPATH_URL_PREFIX
	 * @see jef.tools.resource.Resource#exists
	 * @see jef.tools.resource.Resource#getInputStream
	 */
//	Resource getResource(String location);
}
