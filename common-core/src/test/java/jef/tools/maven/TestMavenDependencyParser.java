package jef.tools.maven;

import java.io.File;
import java.util.List;

import jef.common.log.LogUtil;

import org.junit.Test;

/**
 * TODO pom.xml中存在特定环境依赖，故先ingore
 *
 */
public class TestMavenDependencyParser {

	@Test
	public void test() throws Exception {
		File pomFile=new File("E:/MyWork/jef/support-lib/jef-cxf/pom.xml");
		MavenDependencyParser.debug=false;
		List<File> files=MavenDependencyParser.parseDependency(pomFile);
		LogUtil.show(files.size());
		LogUtil.show(files);
	}

}
