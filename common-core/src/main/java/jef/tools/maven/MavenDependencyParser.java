package jef.tools.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.SimpleXPath;
import jef.tools.StringUtils;
import jef.tools.X;
import jef.tools.XMLUtils;
import jef.tools.maven.jaxb.Dependency;
import jef.tools.maven.jaxb.Dependency.Exclusions;
import jef.tools.maven.jaxb.Exclusion;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class MavenDependencyParser {
	public static boolean debug = false;

	public static List<File> parseDependency(File pomFile) {
		File m2 = getM2();
		Map<String, Dependency> set = new HashMap<String, Dependency>();
		try {
			parseDependency(new Pom(pomFile, true), set, null, m2);
		} catch (SAXException e) {
			LogUtil.exception(e);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		List<File> result = new ArrayList<File>(set.size());
		for (Dependency d : set.values()) {
			result.add(new File(m2, getJarFileName(d)));
		}
		return result;
	}

	private static String getKey(Dependency dep) {
		return dep.getGroupId() + "-" + dep.getArtifactId();

	}

	static class ExtendsAsset {
		final Map<String, String> properties = new HashMap<String, String>();
		final Map<String, Dependency> dependencies = new HashMap<String, Dependency>();

		public void add(Dependency d) {
			dependencies.put(getKey(d), d);
		}
	}

	static class Pom {
		File pomFile;
		private Document document;
		boolean isNative;

		Pom(File file) {
			this.pomFile = file;
		}

		Pom(File file, boolean isNative) {
			this.pomFile = file;
			this.isNative = isNative;
		}

		public Document getDocument() throws SAXException, IOException {
			if (document == null) {
				document = XMLUtils.loadDocument(pomFile);
			}
			return document;
		}

		@Override
		public String toString() {
			return pomFile.toString();
		}
	}

	private static void parseDependency(Pom pomFile, Map<String, Dependency> result, Set<String> exclusions, File m2) throws SAXException, IOException {
		if (exclusions == null)
			exclusions = new HashSet<String>();

		// 首先向上追溯，获得上级中全部的变量，和继承得到的依赖(，还有父级别依赖的排除。)
		// 其中排除隶属于父级依赖，一旦子级依赖中有了该依赖，那么连同排除一起覆盖父级
		ExtendsAsset parent = new ExtendsAsset();
		parseExtends(parent, pomFile);

		// 向下级解析
		for (Dependency d : parent.dependencies.values()) {
			String groupId = d.getGroupId();
			String artifactId = d.getArtifactId();
			String version = d.getVersion();
			File jarFile = new File(m2, getJarFileName(d));
			if (exclusions.contains(getKey(d))) {
				continue;
			}
			if (jarFile.exists()) {
				if (debug) {
					LogUtil.debug("loading " + jarFile.getAbsolutePath() + " ..");
				}
				Dependency old = result.put(getKey(d), d);
				if (old != null) {
					if (canReplace(d, old)) {
						result.put(getKey(d), old);// replaced with old dep,
													// so no need calc...
						if (debug) {
							LogUtil.debug("skip:" + d);

						}
						continue;
					}
					if (debug) {
						LogUtil.debug("skip:" + old);
					}
				}
			} else {
				// if(debug){
				// LogUtil.warn(" NOT EXIST: " + jarFile.getAbsolutePath());
				// }
				continue;
			}

			Set<String> newExclusion = new HashSet<String>();
			newExclusion.addAll(exclusions);
			for (Exclusion ex : d.getExclusions().getExclusion()) {
				newExclusion.add(ex.getGroupId() + "-" + ex.getArtifactId());
			}

			String childPomFileName = getChildPomName(groupId, artifactId, version);
			File childPomFile = new File(m2, childPomFileName);
			if (childPomFile.exists()) {
				parseDependency(new Pom(childPomFile), result, newExclusion, m2);
			} else {
				// LogUtil.warn("POM NOT EXIST: " +
				// childPomFile.getAbsolutePath());
			}
		}
	}

	// 传入本级DOC,载入父级DOC，按倒序
	private static List<Pom> getParents(Pom doc) throws SAXException, IOException {
		LinkedList<Pom> list = new LinkedList<Pom>();
		list.add(doc);
		while (doc != null) {
			doc = getParentPomFile(doc);
			if (doc != null)
				list.addFirst(doc);
		}
		return list;
	}

	private static Pom getParentPomFile(Pom doc) throws SAXException, IOException {
		Element parent = X.$("/project/parent", doc.getDocument());
		if (parent != null) {
			String pgroupId = X.nodeText(parent, "groupId");
			String partifactId = X.nodeText(parent, "artifactId");
			String pversion = X.nodeText(parent, "version");
			String relativePath = X.nodeText(parent, "relativePath");
			File pPomFile;
			// if(StringUtils.isEmpty(relativePath)){
			StringBuilder sb = new StringBuilder();
			sb.append(pgroupId.replace('.', '/')).append('/');
			sb.append(partifactId.replace('.', '.'));
			sb.append("/");
			sb.append(pversion).append('/').append(partifactId).append('-').append(pversion);
			pPomFile = new File(getM2(), sb.toString() + ".pom");
			// }else{
			// if(relativePath.endsWith("/")||relativePath.endsWith("\\")){
			// relativePath=relativePath.concat("pom.xml");
			// }else if(relativePath.endsWith("pom.xml")){
			// }else{
			// relativePath=relativePath.concat("/pom.xml");
			// }
			// pPomFile=new File(doc.pomFile.getParentFile(),relativePath);
			// }
			if (pPomFile.exists())
				return new Pom(pPomFile,doc.isNative);
		}
		return null;
	}

	private static void parseExtends(ExtendsAsset parentContext, Pom doc) throws SAXException, IOException {
		List<Pom> parents = getParents(doc);
		for (Pom parent : parents) {
			Map<String, String> map = XMLUtils.getAttributesMap((Element) SimpleXPath.getNodeByXPath(parent.getDocument(), "/project/properties"), true);
			parentContext.properties.putAll(map);
			parseDependency(parentContext, parent);
		}
	}

	/**
	 * 计算版本冲突，如果前面的版本不大于前面的版本，就返回true
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private static boolean canReplace(Dependency from, Dependency to) {
		String[] v1 = StringUtils.split(from.getVersion(), ".-");
		String[] v2 = StringUtils.split(to.getVersion(), ".-");
		for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
			int vv1 = StringUtils.toInt(v1[i], 0);
			int vv2 = StringUtils.toInt(v2[i], 0);
			if (vv1 > vv2)
				return false;
			if (vv1 < vv2)
				return true;
		}
		if (v1.length > v2.length) {
			return false;
		}
		return true;
	}

	public static File getM2() {
		File m2 = null;
		String mavenRepo = System.getenv("M2_REPOSITORY");
		if (StringUtils.isNotEmpty(mavenRepo)) {// 支持自定义的Mavan目录
			m2 = new File(mavenRepo);
			Assert.isTrue(m2.exists(), "Can not locate .m2/repository folder!" + m2.getAbsolutePath());
		} else {
			String home = System.getenv("USERPROFILE");
			if (StringUtils.isEmpty(home)) {// 支持Linux环境的参数
				home = System.getenv("HOME");
			}
			m2 = new File(home, ".m2/repository");
			Assert.isTrue(m2.exists(), "Can not locate .m2/repository folder!" + m2.getAbsolutePath() + ". if you have a custom repository path, set it into the env-variable 'M2_REPOSITORY'.");
		}
		return m2;
	}

	private static String processVersion(String version, Pom pom, Map<String, String> assets) throws SAXException, IOException {
		Assert.notNull(version);
		if (version.indexOf("${") == -1) {
			return version;
		}

		String paramName = org.apache.commons.lang.StringUtils.substringBetween(version, "${", "}");
		String paramValue = null;
		Document doc = pom.getDocument();
		if ("project.version".equals(paramName)) {
			try {
				paramValue = SimpleXPath.getAttributeByXPath(doc, "/project/version@#text");
			} catch (NoSuchElementException e) {
			}
			if (StringUtils.isEmpty(paramValue)) {
				try {
					paramValue = SimpleXPath.getAttributeByXPath(doc, "/project/parent/version@#text");
				} catch (NoSuchElementException e) {
				}
			}
		} else {
			paramValue = assets.get(paramName);
			if (paramValue == null) {
				Pom currentDoc = pom;
				while (currentDoc != null) {
					Element propertiesNode = (Element) SimpleXPath.getNodeByXPath(currentDoc.getDocument(), "/project/properties");
					paramValue = XMLUtils.nodeText(propertiesNode, paramName);
					if (paramValue != null)
						break;

					currentDoc = getParentPomFile(currentDoc);
				}
			}
		}
		if (paramValue != null) {
			return version.replace("${" + paramName + "}", paramValue);
		}
		return version;
	}

	public static String getJarFileName(Dependency dep) {
		StringBuilder sb = new StringBuilder();
		sb.append(dep.getGroupId().replace('.', '/')).append('/');
		sb.append(dep.getArtifactId().replace('.', '.'));
		sb.append("/");
		sb.append(dep.getVersion()).append('/').append(dep.getArtifactId()).append('-').append(dep.getVersion());
		if (StringUtils.isNotEmpty(dep.getClassifier()))
			sb.append("-").append(dep.getClassifier());
		sb.append(".jar");
		return sb.toString();
	}

	private static String getChildPomName(String groupId, String artifactId, String version) {
		StringBuilder sb = new StringBuilder();
		sb.append(groupId.replace('.', '/')).append('/');
		sb.append(artifactId.replace('.', '.'));
		sb.append("/");
		sb.append(version).append('/').append(artifactId).append('-').append(version);
		sb.append(".pom");
		return sb.toString();
	}

	private static Set<Exclusion> parseExclusion(Element e) {
		Set<Exclusion> result = new HashSet<Exclusion>();
		List<Element> nodes = XMLUtils.getElementsByTagNames(e, "exclusion");
		for (Element anExclusionNode : nodes) {
			String gpId = XMLUtils.first(anExclusionNode, "groupId").getTextContent();
			String arId = XMLUtils.first(anExclusionNode, "artifactId").getTextContent();
			Exclusion anExclusion = new Exclusion();
			result.add(anExclusion);
			anExclusion.setGroupId(gpId);
			anExclusion.setArtifactId(arId);
		}
		return result;
	}

	/**
	 * 解析本级的依赖，和上级合并
	 * 
	 * @param parent
	 * @param doc
	 * @param assets
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	private static void parseDependency(ExtendsAsset assets, Pom doc) throws SAXException, IOException {
		Element docEle = doc.getDocument().getDocumentElement();
		for (Element dependenciesEle : XMLUtils.getElementsByTagNames(docEle, "dependencies")) {
			List<Element> dependEles = XMLUtils.childElements(dependenciesEle, "dependency");
			if (dependEles == null)
				return;
			for (Element e : dependEles) {
				String groupId = XMLUtils.nodeText(e, "groupId");
				String artifactId = XMLUtils.nodeText(e, "artifactId");
				String version = XMLUtils.nodeText(e, "version");
				if (groupId == null || artifactId == null || version == null)
					continue;

				String classifier = XMLUtils.nodeText(e, "classifier");
				String scope = XMLUtils.nodeText(e, "scope");
				if ("test".equals(scope) || "system".equals(scope)) {// 这些依赖无效
					continue;
				}
				if ("provided".equals(scope) && doc.isNative == false) {// 非本级的provided依赖无效
					continue;
				}
				version = processVersion(version, doc, assets.properties);

				Exclusions exclusions = new Exclusions();
				Dependency d = new Dependency();
				d.setGroupId(groupId);
				d.setArtifactId(artifactId);
				d.setVersion(version);
				d.setClassifier(classifier);
				d.setScope(scope);
				d.setExclusions(exclusions);
				Set<Exclusion> exclusionSet = parseExclusion(e);
				exclusions.getExclusion().addAll(exclusionSet);
				assets.add(d);
			}
		}
	}
}
