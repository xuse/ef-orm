package jef.tools.resource;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

abstract class VfsPatternUtils extends VfsUtils {

	static Object getVisitorAttribute() {
		return doGetVisitorAttribute();
	}

	static String getPath(Object resource) {
		return doGetPath(resource);
	}

	static Object findRoot(URL url) throws IOException {
		return getRoot(url);
	}

	static void visit(Object resource, InvocationHandler visitor) throws IOException {
		Object visitorProxy = Proxy.newProxyInstance(VIRTUAL_FILE_VISITOR_INTERFACE.getClassLoader(), new Class<?>[] { VIRTUAL_FILE_VISITOR_INTERFACE }, visitor);
		invokeVfsMethod(VIRTUAL_FILE_METHOD_VISIT, resource, visitorProxy);
	}

}