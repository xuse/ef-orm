package jef.orm.onetable.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;

import jef.common.log.LogUtil;
import jef.tools.Exceptions;
import jef.tools.IOUtils;

public class Test1 {
	public static void main(String[] args) {
		LogUtil.show("");
		Test1 t1 = new Test1();
		LogUtil.show(t1.getFromSource());
	}

	private Map<String, String> getFromSource() {
		Map<String, String> result = new HashMap<String, String>();
		Class type = CaAsset.class;
		URL url = this.getClass().getResource("/" + type.getName().replace('.', '/') + ".java");
		if (url == null) {
			url = getFixedPathSource(type);
		}

		if (url == null)
			return result;
		try {
			InputStream in = url.openStream();
			try {
				CompilationUnit unit = JavaParser.parse(in, "UTF-8");
				if (unit.getTypes().isEmpty())
					return result;
				TypeDeclaration typed = unit.getTypes().get(0);
				if (typed instanceof ClassOrInterfaceDeclaration) {
					ClassOrInterfaceDeclaration clz = (ClassOrInterfaceDeclaration) typed;
					String table = getContent(clz.getComment());
					if (table != null)
						result.put("#TABLE", table);
					for (BodyDeclaration body : typed.getMembers()) {
						if (body instanceof FieldDeclaration) {
							FieldDeclaration field = (FieldDeclaration) body;
							if (ModifierSet.isStatic(field.getModifiers())) {
								continue;
							}
							if (field.getVariables().size() > 1) {
								continue;
							}
							String name = field.getVariables().get(0).getId().getName();
							Object doc = field.getJavaDoc();
							String javaDoc = getContent(field.getComment());
							if (javaDoc != null)
								result.put(name, javaDoc);
						}
					}
				}
			} finally {
				IOUtils.close(in);
			}
		} catch (ParseException e) {
			Exceptions.log(e);
		} catch (IOException e) {
			Exceptions.log(e);
		}
		return result;
	}

	private String getContent(Comment comment) {
		if (comment == null)
			return null;
		String s = comment.getContent();
		return s.replaceAll("\\s*\\*", "").trim();
	}

	private URL getFixedPathSource(Class type) {
		String clzPath = "/" + type.getName().replace('.', '/') + ".class";
		URL url = this.getClass().getResource(clzPath);
		if (url == null)
			return null;
		String path = url.getPath();
		path = path.substring(0, path.length() - clzPath.length());
		File source = null;
		if (path.endsWith("/target/test-classes")) {
			source = new File(path.substring(0, path.length() - 20), "src/test/java");
		} else if (path.endsWith("/target/classes")) {
			source = new File(path.substring(0, path.length() - 15), "src/main/java");
		}
		if (source == null)
			return null;
		File java = new File(source, type.getName().replace('.', '/') + ".java");
		if (java.exists())
			try {
				return java.toURI().toURL();
			} catch (MalformedURLException e) {
				Exceptions.log(e);
				return null;
			}
		return null;
	}
}
