/*
 * Copyright 2015, The Querydsl Team (http://www.querydsl.com/team)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.geequery.mavenplugin.goal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.geequery.utils.SpringApplicationGenerator;

import jef.common.log.LogUtil;
import jef.tools.Exceptions;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.io.Charsets;

/**
 * @goal springboot-init
 * @phase generate-resources
 */

public class SpringbootMojo extends AbstractMojo {

	/**
	 * Whether to skip the exporting execution
	 *
	 * @parameter default-value=false property="maven.querydsl.skip"
	 */
	private boolean skip;

	/**
	 * @parameter default-value="com.mycompany.application" require=true
	 */
	private String applicationPackage;

	/**
	 * @parameter default-value="MyGeeQueryApplication" require=true
	 */
	private String applicationName;

	/**
	 * target source folder to create the sources into (e.g.
	 * target/generated-sources/java)
	 *
	 * @parameter
	 * @required
	 */
	private String targetFolder;

	/**
	 * @parameter default-value="src/test/java" required=true
	 */
	private String testFolder;

	/**
	 * JDBC driver class name
	 * 
	 * @parameter required=true
	 */
	private String jdbcDriver;

	/**
	 * JDBC connection url
	 * 
	 * @parameter required=true
	 */
	private String jdbcUrl;

	/**
	 * JDBC connection username
	 * 
	 * @parameter
	 */
	private String jdbcUser;

	/**
	 * JDBC connection password
	 * 
	 * @parameter
	 */
	private String jdbcPassword;

	/**
	 * package name for sources
	 * 
	 * @parameter
	 * @required
	 */
	private String packageName;

	/**
	 * 生成仓库
	 *
	 * @parameter default-value=false
	 */
	private boolean exportRepos;

	/**
	 * Repository Package name
	 * 
	 * @parameter
	 */
	private String repositoryPackage;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		/**
		 * 生成Spring文件
		 */
		new SpringApplicationGenerator().setTestFoler(new File(testFolder))
				// .setSrcFolder(new File(targetFolder))
				.setPackageName(applicationPackage).generateSpringClass(applicationName);
		
		/**
		 * 移动资源文件
		 */
		File resourceRoot = new File(System.getProperty("user.dir"), "src/main/resources");
		File propertiesTarget = new File(resourceRoot, "application.properties");
		
		if (!propertiesTarget.exists()) {
			URL source = this.getClass().getResource("/resource/application.properties");
			if (source != null) {
				try {
					Map<String, String> props = new HashMap<String, String>();
					props.put("spring.datasource.url", this.jdbcUrl);
					props.put("spring.datasource.username", this.jdbcUser);
					props.put("spring.datasource.password", this.jdbcPassword);
					props.put("geequery.packagesToScan", this.packageName);
					String reposPackage = this.repositoryPackage;
					if (StringUtils.isEmpty(reposPackage)) {
						reposPackage = StringUtils.substringBeforeLast(packageName, ".") + ".repos";
					}
					props.put("geequery.repos", reposPackage);
					moveProperties(source, propertiesTarget, props);
				} catch (IOException e) {
					throw Exceptions.asIllegalState(e);
				}
			}else {
				LogUtil.info("file was not found. {}", "/resource/application.properties");
			}
		}else {
			LogUtil.info("application.properties was already exists.");
		}

	}

	private void moveProperties(URL source, File propertiesTarget, Map<String, String> properties) throws IOException {
		BufferedReader reader = IOUtils.getReader(source, Charsets.UTF8);
		BufferedWriter writer = IOUtils.getWriter(propertiesTarget, Charsets.UTF8);
		String line;
		for (; (line = reader.readLine()) != null;) {
			if (line.startsWith("#")) {
				writer.write(line);
				writer.newLine();
				continue;
			}
			int n = line.indexOf('=');
			if (n > 0) {
				String key = line.substring(0, n).trim();
				if (properties.containsKey(key)) {
					writer.write(key + "=" + properties.get(key));
				} else {
					writer.write(line);
				}
			} else {
				writer.write(line);
			}
			writer.newLine();
		}
		IOUtils.closeQuietly(reader);
		IOUtils.closeQuietly(writer);
		LogUtil.info("{} was generated.", propertiesTarget.getAbsolutePath());
	}

	protected boolean isForTest() {
		return false;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	private static String emptyIfSetToBlank(String value) {
		boolean setToBlank = value == null || value.equalsIgnoreCase("BLANK");
		return setToBlank ? "" : value;
	}

	public void setApplicationPackage(String applicationPackage) {
		this.applicationPackage = applicationPackage;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
