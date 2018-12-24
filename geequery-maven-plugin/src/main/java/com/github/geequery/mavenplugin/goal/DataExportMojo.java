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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ORMConfig;
import jef.database.support.InitDataExporter;
import jef.tools.StringUtils;

/**
 * @goal export-data
 * @phase generate-resources
 */

public class DataExportMojo extends AbstractMojo {
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
	 * tableNamePattern a table name pattern; must match the table name as it is
	 * stored in the database (default: null)
	 *
	 * @parameter
	 * @required
	 */
	private String exportDataFrom;

	/**
	 * target source folder to create the sources into (e.g.
	 * target/generated-sources/java)
	 *
	 * @parameter default-value="src/main/resources"
	 * @required
	 */
	private String resourceFolder;

	/**
	 * The directory containing generated classes.
	 * 
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 * 
	 */
	private File classesDirectory;

	/**
	 * Whether to skip the exporting execution
	 *
	 * @parameter default-value=false property="maven.geequery.skip"
	 */
	private boolean skip;
	
	/**
	 * 设置为true时，仅当类上具有@InitializeData注解的时才会导出。
	 * @parameter default-value=false
	 */
	private boolean withInitializeDataAnnotation;

	/**
	 * 每张表最多导出多少条记录
	 * 
	 * @parameter default-value=0
	 */
	private int maxResult;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		ORMConfig.getInstance().setCheckEnhancement(false);
		DbClient db;
		db = new DbClientBuilder(this.jdbcUrl, this.jdbcUser, this.jdbcPassword).setEnhanceScanPackages(false).build();
		try {
			InitDataExporter ex = new InitDataExporter(db, new File(this.resourceFolder));
			ex.addClassRoot(classesDirectory.toURI().toURL());
			ex.setTarget(new File(resourceFolder));
			ex.setExportOnlyAnnotationPresent(withInitializeDataAnnotation);
			if (maxResult > 0) {
				ex.setMaxResults(maxResult);
			}
			if (StringUtils.isNotEmpty(exportDataFrom)) {
				ex.exportPackage(this.exportDataFrom);
			}
		} catch (ClassNotFoundException e) {
			throw new MojoExecutionException("ClassNotFound", e);
		} catch (IOException e) {
			throw new MojoExecutionException("IOException", e);
		} finally {
			db.shutdown();
		}
	}

	protected boolean isForTest() {
		return false;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public void setJdbcPassword(String jdbcPassword) {
		this.jdbcPassword = jdbcPassword;
	}

	public void setTargetFolder(String targetFolder) {
		this.resourceFolder = targetFolder;
	}

	public void setExportDataFrom(String exportDataFrom) {
		this.exportDataFrom = exportDataFrom;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}

	public void setResourceFolder(String resourceFolder) {
		this.resourceFolder = resourceFolder;
	}

	public void setClassesDirectory(File classesDirectory) {
		this.classesDirectory = classesDirectory;
	}

	public void setWithInitializeDataAnnotation(boolean withInitializeDataAnnotation) {
		this.withInitializeDataAnnotation = withInitializeDataAnnotation;
	}
}
